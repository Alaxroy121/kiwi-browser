/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace

import android.content.Context
import org.chromium.base.ThreadUtils
import org.chromium.base.annotations.VisibleForTesting
import org.chromium.chrome.browser.profiles.Profile
import org.chromium.chrome.browser.profiles.ProfileManager

/**
 * Central manager for Satellite Spaces — isolated browsing containers.
 *
 * Each Satellite Space maps to a Chromium [Profile] with its own:
 * - Cookies and session data
 * - HTTP cache and disk cache
 * - Web Storage (localStorage, sessionStorage)
 * - IndexedDB databases
 * - Service Worker registrations
 * - Credential storage
 *
 * ## Architecture
 *
 * ```
 * SatelliteSpaceManager
 *   ├── spaceMap: Map<String, SatelliteSpace>   (in-memory cache)
 *   ├── profileMap: Map<String, Profile>         (Chromium Profile cache)
 *   └── preferences: SatelliteSpacePreferences   (SharedPreferences persistence)
 * ```
 *
 * ## Thread Safety
 *
 * All public methods must be called on the main (UI) thread, as they
 * interact with Chromium's Profile APIs which are main-thread-only.
 *
 * ## Usage
 *
 * ```kotlin
 * val manager = SatelliteSpaceManager.getInstance(context)
 *
 * // Create a new isolated space
 * val space = manager.createSpace("Work", Color.BLUE)
 *
 * // Get the Chromium Profile for this space (for WebContents creation)
 * val profile = manager.getProfileForSpace(space.id)
 *
 * // Bind a tab to a space
 * manager.bindTabToSpace(tabId = 42, spaceId = space.id)
 * ```
 */
class SatelliteSpaceManager private constructor(context: Context) {

    /** In-memory cache of all spaces, keyed by space ID. */
    private val spaceMap = LinkedHashMap<String, SatelliteSpace>()

    /** Cache of Chromium Profiles, keyed by space ID. */
    private val profileMap = HashMap<String, Profile>()

    /** Persistence layer. */
    private val preferences = SatelliteSpacePreferences(context.applicationContext)

    /** Listeners for space lifecycle events. */
    private val observers = mutableListOf<SatelliteSpaceObserver>()

    init {
        // Load persisted spaces on initialization
        val saved = preferences.loadSpaces()
        saved.forEach { space ->
            spaceMap[space.id] = space
        }
    }

    // ── Space Lifecycle ─────────────────────────────────────────────────

    /**
     * Creates a new Satellite Space with complete data isolation.
     *
     * @param name User-visible name for the space.
     * @param color ARGB color for visual identification.
     * @param icon Optional emoji icon.
     * @return The created [SatelliteSpace].
     * @throws IllegalStateException if called off the main thread.
     */
    fun createSpace(name: String, color: Int, icon: String = "🛰️"): SatelliteSpace {
        ThreadUtils.assertOnUiThread()
        val space = SatelliteSpace.create(name, color, icon)
        spaceMap[space.id] = space
        preferences.addSpace(space)
        notifySpaceCreated(space)
        return space
    }

    /**
     * Removes a Satellite Space and cleans up its Chromium Profile.
     *
     * Tabs bound to this space should be moved to the default space
     * or closed before calling this method.
     *
     * @param spaceId The ID of the space to remove.
     * @return true if the space was found and removed.
     */
    fun removeSpace(spaceId: String): Boolean {
        ThreadUtils.assertOnUiThread()
        if (spaceId == SatelliteSpace.DEFAULT_SPACE_ID) return false

        val removed = spaceMap.remove(spaceId)
        if (removed != null) {
            profileMap.remove(spaceId)
            preferences.removeSpace(spaceId)
            notifySpaceRemoved(removed)
        }
        return removed != null
    }

    /**
     * Updates the display properties of a space (name, color, icon).
     */
    fun updateSpace(space: SatelliteSpace): Boolean {
        ThreadUtils.assertOnUiThread()
        if (!spaceMap.containsKey(space.id)) return false
        spaceMap[space.id] = space
        preferences.updateSpace(space)
        notifySpaceUpdated(space)
        return true
    }

    /**
     * Returns the space with the given ID, or null if it doesn't exist.
     */
    fun getSpace(spaceId: String): SatelliteSpace? {
        return spaceMap[spaceId]
    }

    /**
     * Returns all spaces, including the implicit default space.
     */
    fun getAllSpaces(): List<SatelliteSpace> {
        return listOf(getDefaultSpace()) + spaceMap.values.toList()
    }

    /**
     * Returns all user-created (non-default) spaces.
     */
    fun getUserSpaces(): List<SatelliteSpace> {
        return spaceMap.values.toList()
    }

    /**
     * Returns the implicit default space representation.
     * The default space uses Chromium's original default Profile.
     */
    fun getDefaultSpace(): SatelliteSpace {
        return SatelliteSpace(
            id = SatelliteSpace.DEFAULT_SPACE_ID,
            name = "Default",
            color = SatelliteSpace.SPACE_COLORS[0],
            icon = "🌐"
        )
    }

    // ── Profile Management ──────────────────────────────────────────────

    /**
     * Returns the Chromium [Profile] for the given space ID.
     *
     * For the default space (null or DEFAULT_SPACE_ID), returns the
     * original default Profile. For any other space ID, returns or
     * creates a named Profile that provides complete data isolation.
     *
     * ## How Profile Isolation Works
     *
     * Chromium's Profile system creates separate data directories:
     * - Default: `<app_data>/Default/`
     * - Space "abc": `<app_data>/satellite_space_abc/`
     *
     * Each directory contains its own:
     * - `Cookies` database
     * - `Cache/` directory
     * - `Local Storage/` (IndexedDB, localStorage)
     * - `Service Worker/` registrations
     * - `Login Data` credentials
     *
     * @param spaceId The space ID, or null for the default space.
     * @return A Chromium Profile with isolated data.
     */
    fun getProfileForSpace(spaceId: String?): Profile {
        ThreadUtils.assertOnUiThread()

        // Default space uses the original default Profile
        if (spaceId == null || spaceId == SatelliteSpace.DEFAULT_SPACE_ID) {
            return ProfileManager.getLastUsedRegularProfile()
        }

        // Check cache first
        profileMap[spaceId]?.let { return it }

        // Get the space definition
        val space = spaceMap[spaceId]
            ?: throw IllegalArgumentException("Unknown space ID: $spaceId")

        // Create or retrieve a named Chromium Profile.
        // Profile.getProfile(name) returns an existing profile if one with
        // that name already exists, or creates a new isolated one.
        val profile = Profile.getProfile(space.profileName)
        profileMap[spaceId] = profile
        return profile
    }

    /**
     * Returns the Chromium Profile for a tab, based on its space binding.
     *
     * @param tabId The tab ID.
     * @return The Profile for the tab's space, or the default Profile.
     */
    fun getProfileForTab(tabId: Int): Profile {
        val spaceId = preferences.getSpaceForTab(tabId)
        return getProfileForSpace(spaceId)
    }

    // ── Tab ↔ Space Binding ─────────────────────────────────────────────

    /**
     * Binds a tab to a Satellite Space.
     * The tab's WebContents will use the space's isolated Profile.
     *
     * @param tabId The tab ID to bind.
     * @param spaceId The space ID to bind to, or null for default.
     */
    fun bindTabToSpace(tabId: Int, spaceId: String?) {
        ThreadUtils.assertOnUiThread()
        preferences.bindTabToSpace(tabId, spaceId)
        notifyTabBoundToSpace(tabId, spaceId)
    }

    /**
     * Returns the space ID for a given tab.
     * @return The space ID, or null if the tab is in the default space.
     */
    fun getSpaceForTab(tabId: Int): String? {
        return preferences.getSpaceForTab(tabId)
    }

    /**
     * Returns the SatelliteSpace for a given tab.
     * @return The space, or the default space if not bound.
     */
    fun getSpaceForTabOrDefault(tabId: Int): SatelliteSpace {
        val spaceId = preferences.getSpaceForTab(tabId)
        return if (spaceId != null) {
            spaceMap[spaceId] ?: getDefaultSpace()
        } else {
            getDefaultSpace()
        }
    }

    /**
     * Unbinds a tab from its space (e.g., when the tab is closed).
     */
    fun unbindTab(tabId: Int) {
        preferences.unbindTab(tabId)
    }

    /**
     * Moves a tab from one space to another.
     * This changes the tab's underlying Profile, which means the tab
     * will need to be recreated with the new Profile's data context.
     *
     * @param tabId The tab to move.
     * @param newSpaceId The target space ID, or null for default.
     */
    fun moveTabToSpace(tabId: Int, newSpaceId: String?) {
        bindTabToSpace(tabId, newSpaceId)
    }

    /**
     * Returns all tab IDs currently bound to a specific space.
     */
    fun getTabsInSpace(spaceId: String): List<Int> {
        return preferences.getTabsInSpace(spaceId)
    }

    // ── Observers ───────────────────────────────────────────────────────

    fun addObserver(observer: SatelliteSpaceObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: SatelliteSpaceObserver) {
        observers.remove(observer)
    }

    private fun notifySpaceCreated(space: SatelliteSpace) {
        observers.forEach { it.onSpaceCreated(space) }
    }

    private fun notifySpaceRemoved(space: SatelliteSpace) {
        observers.forEach { it.onSpaceRemoved(space) }
    }

    private fun notifySpaceUpdated(space: SatelliteSpace) {
        observers.forEach { it.onSpaceUpdated(space) }
    }

    private fun notifyTabBoundToSpace(tabId: Int, spaceId: String?) {
        observers.forEach { it.onTabBoundToSpace(tabId, spaceId) }
    }

    // ── Observer Interface ──────────────────────────────────────────────

    /**
     * Observer interface for Satellite Space lifecycle events.
     */
    interface SatelliteSpaceObserver {
        fun onSpaceCreated(space: SatelliteSpace) {}
        fun onSpaceRemoved(space: SatelliteSpace) {}
        fun onSpaceUpdated(space: SatelliteSpace) {}
        fun onTabBoundToSpace(tabId: Int, spaceId: String?) {}
    }

    companion object {
        private const val TAG = "SatelliteSpaceMgr"

        @Volatile
        private var instance: SatelliteSpaceManager? = null

        /**
         * Returns the singleton instance of SatelliteSpaceManager.
         * Must be called on the main thread.
         */
        @JvmStatic
        fun getInstance(context: Context): SatelliteSpaceManager {
            return instance ?: synchronized(this) {
                instance ?: SatelliteSpaceManager(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Resets the singleton. Used for testing.
         */
        @VisibleForTesting
        fun resetForTesting() {
            instance = null
        }
    }
}
