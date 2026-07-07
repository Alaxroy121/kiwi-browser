/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import org.chromium.chrome.browser.satellitespace.SatelliteSpace
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager

/**
 * Dialog for creating a new Satellite Space.
 *
 * Shows a name input field and a color picker grid.
 *
 * ## Usage
 *
 * ```kotlin
 * CreateSatelliteSpaceDialog.show(context, manager) { newSpace ->
 *     // Space created, refresh UI
 *     tabSwitcher.refreshTabs()
 * }
 * ```
 */
object CreateSatelliteSpaceDialog {

    /**
     * Shows the create space dialog.
     *
     * @param context The activity context.
     * @param manager The Satellite Space manager.
     * @param onCreated Callback invoked with the newly created space.
     */
    fun show(
        context: Context,
        manager: SatelliteSpaceManager,
        onCreated: (SatelliteSpace) -> Unit
    ) {
        val layout = createDialogLayout(context)
        val nameInput = layout.findViewById<EditText>(android.R.id.input)

        var selectedColor = SatelliteSpace.SPACE_COLORS[0]
        val colorGrid = createColorGrid(context) { color ->
            selectedColor = color
        }
        (layout as LinearLayout).addView(colorGrid)

        AlertDialog.Builder(context)
            .setTitle("🛰️ New Satellite Space")
            .setMessage("Create an isolated browsing space with its own cookies, cache, and session data.")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim().ifUntitled()
                val space = manager.createSpace(name, selectedColor)
                onCreated(space)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createDialogLayout(context: Context): View {
        val padding = dpToPx(context, 20)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, dpToPx(context, 8), padding, 0)

            addView(EditText(context).apply {
                id = android.R.id.input
                hint = "Space name (e.g. Work, Personal, Research)"
                textSize = 16f
                setSingleLine(true)
            })
        }
    }

    private fun createColorGrid(
        context: Context,
        onColorSelected: (Int) -> Unit
    ): View {
        val gridPadding = dpToPx(context, 20)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(gridPadding, dpToPx(context, 12), gridPadding, 0)

            addView(TextView(context).apply {
                text = "Color"
                textSize = 14f
                setTextColor(0xFF888888.toInt())
            })

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dpToPx(context, 8), 0, 0)
            }

            SatelliteSpace.SPACE_COLORS.forEachIndexed { index, color ->
                val swatch = View(context).apply {
                    val size = dpToPx(context, 36)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = dpToPx(context, 8)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                    }
                    if (index == 0) {
                        // Mark first as selected by default
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(color)
                            setStroke(dpToPx(context, 3), 0xFFFFFFFF.toInt())
                        }
                    }
                    setOnClickListener {
                        // Reset all swatches
                        for (i in 0 until row.childCount) {
                            val child = row.getChildAt(i)
                            val c = SatelliteSpace.SPACE_COLORS[i]
                            child.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(c)
                            }
                        }
                        // Highlight selected
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(color)
                            setStroke(dpToPx(context, 3), 0xFFFFFFFF.toInt())
                        }
                        onColorSelected(color)
                    }
                }
                row.addView(swatch)
            }

            addView(row)
        }
    }

    private fun String.ifUntitled(): String {
        return ifEmpty { "Space" }
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
