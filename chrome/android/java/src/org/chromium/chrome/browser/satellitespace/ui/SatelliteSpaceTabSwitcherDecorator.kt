/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import org.chromium.chrome.browser.satellitespace.SatelliteSpace
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager
import org.chromium.chrome.browser.tab.Tab

/**
 * Strategy for decorating the Tab Switcher UI to visually group
 * tabs by their Satellite Space.
 *
 * ## Integration with Kiwi's Tab Switcher
 *
 * Kiwi Browser uses a grid-based tab switcher. To integrate:
 *
 * ### Option A: ItemDecoration (Recommended)
 *
 * Add a colored border/background to each tab card based on its space:
 *
 * ```kotlin
 * // In TabSwitcherFragment or equivalent
 * val decorator = SatelliteSpaceTabSwitcherDecorator(context, spaceManager)
 * recyclerView.addItemDecoration(decorator)
 * ```
 *
 * ### Option B: Adapter Modification
 *
 * Modify the tab adapter to inject space headers between groups:
 *
 * ```kotlin
 * // In the tab adapter's onBindViewHolder:
 * val space = spaceManager.getSpaceForTabOrDefault(tab.id)
 * holder.spaceLabel.text = "${space.icon} ${space.name}"
 * holder.spaceLabel.setBackgroundColor(space.color)
 * holder.spaceLabel.visibility = View.VISIBLE
 * ```
 *
 * ### Option C: Bottom Sheet Space Filter
 *
 * Add a bottom sheet with space chips that filter the tab list:
 *
 * ```kotlin
 * val spaceSelector = SatelliteSpaceSelectorView(context)
 * spaceSelector.initialize(spaceManager)
 * spaceSelector.setOnSpaceSelectedListener { space ->
 *     adapter.filterBySpace(space.id)
 * }
 * ```
 */
class SatelliteSpaceTabSwitcherDecorator(
    private val context: Context,
    private val manager: SatelliteSpaceManager
) : RecyclerView.ItemDecoration() {

    /** Paint for the space indicator bar at the bottom of each tab card. */
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** Paint for the space label text. */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        isFakeBoldText = true
    }

    /** Cache of tab ID → space color for performance. */
    private val colorCache = mutableMapOf<Int, Int>()

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) ?: continue
            val holder = parent.getChildViewHolder(child) ?: continue

            // Get the tab ID from the ViewHolder's tag or adapter position
            val tabId = getTabIdFromHolder(holder) ?: continue
            val space = manager.getSpaceForTabOrDefault(tabId)
            if (space.id == SatelliteSpace.DEFAULT_SPACE_ID) continue

            // Draw a colored bar at the bottom of the tab card
            val color = colorCache.getOrPut(tabId) { space.color }
            indicatorPaint.color = adjustAlpha(color, 0.8f)

            val rect = RectF(
                child.left.toFloat(),
                child.bottom - INDICATOR_HEIGHT,
                child.right.toFloat(),
                child.bottom.toFloat()
            )
            c.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, indicatorPaint)

            // Draw space icon + name
            val label = "${space.icon} ${space.name}"
            c.drawText(
                label,
                rect.left + dpToPx(8),
                rect.top + dpToPx(16),
                textPaint
            )
        }
    }

    /**
     * Clears the color cache. Call when spaces are modified.
     */
    fun clearColorCache() {
        colorCache.clear()
    }

    /**
     * Extracts the tab ID from a RecyclerView.ViewHolder.
     * Override this if your tab adapter uses a different mechanism.
     */
    private fun getTabIdFromHolder(holder: RecyclerView.ViewHolder): Int? {
        // Default: use itemView tag
        return holder.itemView.tag as? Int
    }

    private fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val INDICATOR_HEIGHT = 48f  // dp, converted in onDraw
        private const val CORNER_RADIUS = 8f
    }
}

/**
 * Extension: Group tabs by their Satellite Space for sectioned display.
 *
 * Returns a list of [SpaceTabGroup] that can be rendered as sections
 * in the tab switcher.
 */
data class SpaceTabGroup(
    val space: SatelliteSpace,
    val tabs: List<Tab>
)

/**
 * Groups a list of tabs by their Satellite Space.
 *
 * @param tabs The list of all open tabs.
 * @param manager The Satellite Space manager.
 * @return A list of [SpaceTabGroup], ordered with default space first.
 */
fun groupTabsBySatelliteSpace(
    tabs: List<Tab>,
    manager: SatelliteSpaceManager
): List<SpaceTabGroup> {
    val groups = mutableMapOf<String, MutableList<Tab>>()

    tabs.forEach { tab ->
        val spaceId = manager.getSpaceForTab(tab.id) ?: SatelliteSpace.DEFAULT_SPACE_ID
        groups.getOrPut(spaceId) { mutableListOf() }.add(tab)
    }

    return groups.entries.map { (spaceId, tabList) ->
        SpaceTabGroup(
            space = manager.getSpace(spaceId) ?: manager.getDefaultSpace(),
            tabs = tabList
        )
    }.sortedByDescending { it.space.id == SatelliteSpace.DEFAULT_SPACE_ID }
}
