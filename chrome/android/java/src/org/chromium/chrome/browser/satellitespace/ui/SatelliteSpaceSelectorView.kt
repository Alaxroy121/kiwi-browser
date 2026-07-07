/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.chromium.chrome.browser.satellitespace.SatelliteSpace
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager

/**
 * A horizontal scrollable selector for switching between Satellite Spaces.
 *
 * Displays each space as a colored chip with an emoji icon. Tapping a chip
 * switches the active space and filters the tab list.
 *
 * ## Layout XML
 *
 * ```xml
 * <org.chromium.chrome.browser.satellitespace.ui.SatelliteSpaceSelectorView
 *     android:id="@+id/satellite_space_selector"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:orientation="vertical" />
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * val selector = findViewById<SatelliteSpaceSelectorView>(R.id.satellite_space_selector)
 * selector.initialize(SatelliteSpaceManager.getInstance(context))
 * selector.setOnSpaceSelectedListener { space ->
 *     // Filter tabs to show only this space
 *     tabSwitcher.filterBySpace(space.id)
 * }
 * ```
 */
class SatelliteSpaceSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpaceChipAdapter
    private var manager: SatelliteSpaceManager? = null
    private var onSpaceSelected: ((SatelliteSpace) -> Unit)? = null
    private var selectedSpaceId: String = SatelliteSpace.DEFAULT_SPACE_ID

    init {
        orientation = VERTICAL
        setupView()
    }

    private fun setupView() {
        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            clipToPadding = false
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    /**
     * Initializes the selector with the Satellite Space manager.
     */
    fun initialize(manager: SatelliteSpaceManager) {
        this.manager = manager
        adapter = SpaceChipAdapter { space ->
            selectedSpaceId = space.id
            adapter.setSelectedId(space.id)
            onSpaceSelected?.invoke(space)
        }
        recyclerView.adapter = adapter
        refreshSpaces()
    }

    /**
     * Refreshes the space list from the manager.
     */
    fun refreshSpaces() {
        val mgr = manager ?: return
        adapter.submitList(mgr.getAllSpaces())
    }

    /**
     * Sets the listener for space selection events.
     */
    fun setOnSpaceSelectedListener(listener: (SatelliteSpace) -> Unit) {
        onSpaceSelected = listener
    }

    /**
     * Programmatically selects a space by ID.
     */
    fun selectSpace(spaceId: String) {
        selectedSpaceId = spaceId
        adapter.setSelectedId(spaceId)
        manager?.getSpace(spaceId)?.let { onSpaceSelected?.invoke(it) }
    }

    /**
     * Returns the currently selected space ID.
     */
    fun getSelectedSpaceId(): String = selectedSpaceId

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    // ── Adapter ─────────────────────────────────────────────────────────

    private class SpaceChipAdapter(
        private val onChipClicked: (SatelliteSpace) -> Unit
    ) : RecyclerView.Adapter<SpaceChipAdapter.ChipViewHolder>() {

        private var spaces = listOf<SatelliteSpace>()
        private var selectedId = SatelliteSpace.DEFAULT_SPACE_ID

        fun submitList(newSpaces: List<SatelliteSpace>) {
            spaces = newSpaces
            notifyDataSetChanged()
        }

        fun setSelectedId(id: String) {
            val oldPos = spaces.indexOfFirst { it.id == selectedId }
            val newPos = spaces.indexOfFirst { it.id == id }
            selectedId = id
            if (oldPos >= 0) notifyItemChanged(oldPos)
            if (newPos >= 0) notifyItemChanged(newPos)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
            val chip = createChipView(parent.context)
            return ChipViewHolder(chip)
        }

        override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
            val space = spaces[position]
            holder.bind(space, space.id == selectedId)
            holder.itemView.setOnClickListener { onChipClicked(space) }
        }

        override fun getItemCount() = spaces.size

        private fun createChipView(context: Context): LinearLayout {
            return LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(context, 12), dpToPx(context, 6), dpToPx(context, 12), dpToPx(context, 6))
                val lp = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.WRAP_CONTENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = dpToPx(context, 8)
                layoutParams = lp
            }
        }

        class ChipViewHolder(private val chip: LinearLayout) : RecyclerView.ViewHolder(chip) {
            private val iconView: TextView
            private val nameView: TextView

            init {
                iconView = TextView(chip.context).apply {
                    textSize = 16f
                    setPadding(0, 0, dpToPx(chip.context, 4), 0)
                }
                nameView = TextView(chip.context).apply {
                    textSize = 13f
                    setTextColor(0xFFFFFFFF.toInt())
                }
                chip.addView(iconView)
                chip.addView(nameView)
            }

            fun bind(space: SatelliteSpace, isSelected: Boolean) {
                iconView.text = space.icon
                nameView.text = space.name

                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(chip.context, 16).toFloat()
                    setColor(if (isSelected) space.color else adjustAlpha(space.color, 0.5f))
                    if (isSelected) {
                        setStroke(dpToPx(chip.context, 2), 0xFFFFFFFF.toInt())
                    }
                }
                chip.background = bg
            }

            companion object {
                private fun dpToPx(context: Context, dp: Int): Int {
                    return (dp * context.resources.displayMetrics.density).toInt()
                }
            }

            private fun adjustAlpha(@ColorInt color: Int, factor: Float): Int {
                val alpha = (android.graphics.Color.alpha(color) * factor).toInt()
                return android.graphics.Color.argb(
                    alpha,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)
                )
            }
        }

        private fun dpToPx(context: Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
    }
}
