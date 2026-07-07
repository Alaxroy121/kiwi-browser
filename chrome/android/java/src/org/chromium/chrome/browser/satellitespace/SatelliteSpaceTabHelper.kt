/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace

import android.content.Context
import org.chromium.base.Log
import org.chromium.chrome.browser.profiles.Profile
import org.chromium.chrome.browser.tab.Tab

/**
 * Helper that binds [Tab] instances to Satellite Spaces.
 *
 * This class acts as the bridge between the tab model and the
 * Satellite Space isolation system. It ensures that:
 *
 * 1. Each tab's WebContents is created with the correct isolated Profile
 * 2. Tab space bindings persist across app restarts
 * 3. Tab lifecycle events properly manage space bindings
 *
 * ## Integration Point
 *
 * In `TabImpl.java`, the `initialize()` method creates WebContents:
 * ```java
 * Profile profile = IncognitoUtils.getProfileFromWindowAndroid(mWindowAndroid, isIncognito());
 * webContents = WebContentsFactory.createWebContents(profile, initiallyHidden);
 * ```
 *
 * This must be changed to:
 * ```java
 * Profile profile = SatelliteSpaceTabHelper.getProfileForTab(
 *     getContext(), mId, mIncognito, mWindowAndroid);
 * webContents = WebContentsFactory.createWebContents(profile, initiallyHidden);
 * ```
 */
class SatelliteSpaceTabHelper(
    private val context: Context,
    private val manager: SatelliteSpaceManager
) : SatelliteSpaceManager.SatelliteSpaceObserver {

    init {
        manager.addObserver(this)
    }

    /**
     * Returns the correct Chromium Profile for a tab.
     *
     * If the tab is bound to a Satellite Space, returns that space's
     * isolated Profile. Otherwise, falls back to the default behavior
     * (regular or incognito profile).
     *
     * @param context Application context.
     * @param tabId The tab's ID.
     * @param isIncognito Whether the tab is incognito.
     * @param windowAndroid The WindowAndroid for profile resolution.
     * @return The appropriate Chromium Profile.
     */
    fun getProfileForTab(
        context: Context,
        tabId: Int,
        isIncognito: Boolean,
        windowAndroid: org.chromium.ui.base.WindowAndroid?
    ): Profile {
        val spaceId = manager.getSpaceForTab(tabId)
        if (spaceId != null) {
            // Tab is bound to a Satellite Space — use its isolated Profile
            try {
                return manager.getProfileForSpace(spaceId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get profile for space $spaceId, falling back to default", e)
            }
        }

        // Fall back to default behavior: regular or incognito profile
        return org.chromium.chrome.browser.incognito.IncognitoUtils
            .getProfileFromWindowAndroid(windowAndroid, isIncognito)
    }

    /**
     * Binds a newly created tab to a Satellite Space.
     * Call this right after tab creation, before URL loading.
     *
     * @param tab The tab to bind.
     * @param spaceId The space to bind to, or null for default.
     */
    fun bindNewTab(tab: Tab, spaceId: String?) {
        if (spaceId != null) {
            manager.bindTabToSpace(tab.id, spaceId)
            Log.d(TAG, "Bound tab ${tab.id} to space $spaceId")
        }
    }

    /**
     * Handles tab closure — cleans up the space binding.
     */
    fun onTabClosed(tabId: Int) {
        manager.unbindTab(tabId)
    }

    /**
     * Returns the space info for a tab (for UI display).
     */
    fun getSpaceInfoForTab(tabId: Int): SatelliteSpace {
        return manager.getSpaceForTabOrDefault(tabId)
    }

    /**
     * Moves a tab to a different space.
     * Note: The tab's WebContents need to be recreated for the
     * new Profile to take effect.
     */
    fun moveTabToSpace(tab: Tab, newSpaceId: String?) {
        manager.moveTabToSpace(tab.id, newSpaceId)
        // The caller should recreate the tab's WebContents with the new Profile
    }

    // ── SatelliteSpaceObserver ──────────────────────────────────────────

    override fun onSpaceRemoved(space: SatelliteSpace) {
        // When a space is removed, move its tabs to the default space
        val tabIds = manager.getTabsInSpace(space.id)
        tabIds.forEach { tabId ->
            manager.bindTabToSpace(tabId, null)
        }
        Log.d(TAG, "Moved ${tabIds.size} tabs from removed space ${space.id} to default")
    }

    fun destroy() {
        manager.removeObserver(this)
    }

    companion object {
        private const val TAG = "SatelliteTabHelper"

        /**
         * Static helper for use directly in TabImpl.java.
         * This avoids requiring TabImpl to hold a reference to the helper.
         */
        @JvmStatic
        fun getProfileForTabStatic(
            context: Context,
            tabId: Int,
            isIncognito: Boolean,
            windowAndroid: org.chromium.ui.base.WindowAndroid?
        ): Profile {
            val manager = SatelliteSpaceManager.getInstance(context)
            val helper = SatelliteSpaceTabHelper(context, manager)
            return helper.getProfileForTab(context, tabId, isIncognito, windowAndroid)
        }
    }
}
