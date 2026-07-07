/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles persistence of Satellite Spaces using Android SharedPreferences.
 *
 * Storage format:
 * - Key "satellite_spaces" → JSON array of serialized [SatelliteSpace] objects
 * - Key "tab_space_bindings" → JSON map of tabId → spaceId
 *
 * This class is thread-safe for read operations but callers should
 * synchronize write operations if accessed from multiple threads.
 */
class SatelliteSpacePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Loads all persisted Satellite Spaces.
     * @return List of saved spaces, empty if none exist.
     */
    fun loadSpaces(): List<SatelliteSpace> {
        val json = prefs.getString(KEY_SPACES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                SatelliteSpace.fromJson(array.getJSONObject(i))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load satellite spaces", e)
            emptyList()
        }
    }

    /**
     * Persists the full list of Satellite Spaces, replacing any existing data.
     */
    fun saveSpaces(spaces: List<SatelliteSpace>) {
        val array = JSONArray()
        spaces.forEach { space ->
            array.put(space.toJson())
        }
        prefs.edit().putString(KEY_SPACES, array.toString()).apply()
    }

    /**
     * Adds a single space to the persisted list.
     * @return The updated full list of spaces.
     */
    fun addSpace(space: SatelliteSpace): List<SatelliteSpace> {
        val current = loadSpaces().toMutableList()
        current.add(space)
        saveSpaces(current)
        return current
    }

    /**
     * Removes a space by ID from the persisted list.
     * @return The updated full list of spaces.
     */
    fun removeSpace(spaceId: String): List<SatelliteSpace> {
        val current = loadSpaces().toMutableList()
        current.removeAll { it.id == spaceId }
        saveSpaces(current)
        // Also clean up any tab bindings for this space
        clearBindingsForSpace(spaceId)
        return current
    }

    /**
     * Updates an existing space in the persisted list.
     * @return The updated full list of spaces.
     */
    fun updateSpace(updated: SatelliteSpace): List<SatelliteSpace> {
        val current = loadSpaces().toMutableList()
        val index = current.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            current[index] = updated
            saveSpaces(current)
        }
        return current
    }

    // ── Tab ↔ Space Bindings ────────────────────────────────────────────

    /**
     * Saves a binding: which tab belongs to which space.
     */
    fun bindTabToSpace(tabId: Int, spaceId: String?) {
        val bindings = loadTabBindings().toMutableMap()
        if (spaceId == null) {
            bindings.remove(tabId.toString())
        } else {
            bindings[tabId.toString()] = spaceId
        }
        saveTabBindings(bindings)
    }

    /**
     * Returns the space ID for a given tab, or null if the tab is in the default space.
     */
    fun getSpaceForTab(tabId: Int): String? {
        val bindings = loadTabBindings()
        return bindings[tabId.toString()]
    }

    /**
     * Returns all tab IDs bound to a specific space.
     */
    fun getTabsInSpace(spaceId: String): List<Int> {
        val bindings = loadTabBindings()
        return bindings.filter { it.value == spaceId }.keys.mapNotNull { it.toIntOrNull() }
    }

    /**
     * Removes a tab binding (e.g., when a tab is closed).
     */
    fun unbindTab(tabId: Int) {
        val bindings = loadTabBindings().toMutableMap()
        bindings.remove(tabId.toString())
        saveTabBindings(bindings)
    }

    private fun loadTabBindings(): Map<String, String> {
        val json = prefs.getString(KEY_TAB_BINDINGS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key ->
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load tab bindings", e)
            emptyMap()
        }
    }

    private fun saveTabBindings(bindings: Map<String, String>) {
        val obj = JSONObject()
        bindings.forEach { (tabId, spaceId) ->
            obj.put(tabId, spaceId)
        }
        prefs.edit().putString(KEY_TAB_BINDINGS, obj.toString()).apply()
    }

    private fun clearBindingsForSpace(spaceId: String) {
        val bindings = loadTabBindings().toMutableMap()
        bindings.entries.removeAll { it.value == spaceId }
        saveTabBindings(bindings)
    }

    companion object {
        private const val TAG = "SatelliteSpacePrefs"
        private const val PREFS_NAME = "satellite_space_prefs"
        private const val KEY_SPACES = "satellite_spaces"
        private const val KEY_TAB_BINDINGS = "tab_space_bindings"
    }
}
