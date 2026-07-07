/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace

import android.graphics.Color
import org.json.JSONObject
import java.util.UUID

/**
 * Data model representing a Satellite Space — an isolated browsing container
 * with its own cookies, cache, web storage, and session data.
 *
 * Each SatelliteSpace maps 1:1 to a Chromium [Profile], which provides
 * complete data isolation at the native layer.
 */
data class SatelliteSpace(
    /** Unique identifier for this space. Used as the Chromium Profile name. */
    val id: String,

    /** User-visible display name for this space. */
    val name: String,

    /** ARGB color used to visually distinguish tabs in this space. */
    val color: Int,

    /** Optional emoji icon for the space. */
    val icon: String = "🛰️",

    /** ISO-8601 timestamp of when this space was created. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Whether this space is currently active (has at least one open tab). */
    val isActive: Boolean = false
) {
    /**
     * Returns the Chromium profile name for this space.
     * This is used to create/retrieve an isolated Profile via
     * Profile.getProfile(profileName).
     */
    val profileName: String
        get() = "satellite_space_$id"

    /** Serializes this space to JSON for SharedPreferences storage. */
    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_NAME, name)
        put(KEY_COLOR, color)
        put(KEY_ICON, icon)
        put(KEY_CREATED_AT, createdAt)
        put(KEY_IS_ACTIVE, isActive)
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_COLOR = "color"
        const val KEY_ICON = "icon"
        const val KEY_CREATED_AT = "created_at"
        const val KEY_IS_ACTIVE = "is_active"

        /** The default/default space ID. Tabs with null spaceId use the default profile. */
        const val DEFAULT_SPACE_ID = "__default__"

        /** Predefined color palette for new spaces. */
        val SPACE_COLORS = intArrayOf(
            Color.parseColor("#4CAF50"),  // Green
            Color.parseColor("#2196F3"),  // Blue
            Color.parseColor("#FF9800"),  // Orange
            Color.parseColor("#9C27B0"),  // Purple
            Color.parseColor("#F44336"),  // Red
            Color.parseColor("#00BCD4"),  // Cyan
            Color.parseColor("#795548"),  // Brown
            Color.parseColor("#607D8B"),  // Blue Grey
        )

        /** Creates a new SatelliteSpace with a generated UUID. */
        fun create(name: String, color: Int, icon: String = "🛰️"): SatelliteSpace {
            return SatelliteSpace(
                id = UUID.randomUUID().toString().take(8),
                name = name,
                color = color,
                icon = icon,
                createdAt = System.currentTimeMillis()
            )
        }

        /** Deserializes a SatelliteSpace from JSON. */
        fun fromJson(json: JSONObject): SatelliteSpace {
            return SatelliteSpace(
                id = json.getString(KEY_ID),
                name = json.getString(KEY_NAME),
                color = json.getInt(KEY_COLOR),
                icon = json.optString(KEY_ICON, "🛰️"),
                createdAt = json.optLong(KEY_CREATED_AT, System.currentTimeMillis()),
                isActive = json.optBoolean(KEY_IS_ACTIVE, false)
            )
        }
    }
}
