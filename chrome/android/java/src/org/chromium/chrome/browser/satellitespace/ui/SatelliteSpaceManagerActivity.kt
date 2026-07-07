/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.satellitespace.ui

import android.app.AlertDialog
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.chromium.chrome.browser.satellitespace.SatelliteSpace
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager

/**
 * Activity for managing Satellite Spaces.
 *
 * Shows a list of all spaces with options to:
 * - Create new spaces
 * - Edit space name/color
 * - Delete spaces
 * - View how many tabs are in each space
 *
 * ## AndroidManifest.xml Registration
 *
 * ```xml
 * <activity
 *     android:name=".satellitespace.ui.SatelliteSpaceManagerActivity"
 *     android:label="Satellite Spaces"
 *     android:theme="@style/Theme.AppCompat.Light.DarkActionBar" />
 * ```
 */
class SatelliteSpaceManagerActivity : AppCompatActivity() {

    private lateinit var manager: SatelliteSpaceManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpaceListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = SatelliteSpaceManager.getInstance(this)

        // Build UI programmatically (or use a layout XML)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF121212.toInt())
        }

        // Header
        root.addView(TextView(this).apply {
            text = "🛰️ Satellite Spaces"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(24), dp(24), dp(24), dp(8))
        })

        root.addView(TextView(this).apply {
            text = "Each space has its own cookies, cache, and session data."
            textSize = 14f
            setTextColor(0xFFAAAAAA.toInt())
            setPadding(dp(24), 0, dp(24), dp(16))
        })

        // Space list
        recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@SatelliteSpaceManagerActivity)
            setPadding(dp(16), 0, dp(16), dp(16))
            clipToPadding = false
        }
        root.addView(recyclerView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // "Add Space" button
        root.addView(TextView(this).apply {
            text = "+ Create New Space"
            textSize = 16f
            setTextColor(0xFF90CAF9.toInt())
            setPadding(dp(24), dp(16), dp(24), dp(24))
            setOnClickListener { showCreateDialog() }
        })

        setContentView(root)

        adapter = SpaceListAdapter()
        recyclerView.adapter = adapter
        refreshList()
    }

    private fun refreshList() {
        adapter.submitList(manager.getAllSpaces())
    }

    private fun showCreateDialog() {
        CreateSatelliteSpaceDialog.show(this, manager) {
            refreshList()
        }
    }

    private fun showDeleteConfirmation(space: SatelliteSpace) {
        val tabCount = manager.getTabsInSpace(space.id).size
        val message = if (tabCount > 0) {
            "Delete \"${space.name}\"? Its $tabCount open tab(s) will be moved to the Default space."
        } else {
            "Delete \"${space.name}\"?"
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Space")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                manager.removeSpace(space.id)
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ── Adapter ─────────────────────────────────────────────────────────

    private inner class SpaceListAdapter : RecyclerView.Adapter<SpaceListAdapter.ViewHolder>() {
        private var spaces = listOf<SatelliteSpace>()

        fun submitList(newSpaces: List<SatelliteSpace>) {
            spaces = newSpaces
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val row = LinearLayout(this@SatelliteSpaceManagerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
            return ViewHolder(row)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val space = spaces[position]
            holder.bind(space)
        }

        override fun getItemCount() = spaces.size

        inner class ViewHolder(private val row: LinearLayout) : RecyclerView.ViewHolder(row) {
            fun bind(space: SatelliteSpace) {
                row.removeAllViews()

                // Color indicator
                val colorDot = View(this@SatelliteSpaceManagerActivity).apply {
                    val size = dp(40)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = dp(16)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(space.color)
                    }
                }
                row.addView(colorDot)

                // Name + info
                val textContainer = LinearLayout(this@SatelliteSpaceManagerActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tabCount = manager.getTabsInSpace(space.id).size

                textContainer.addView(TextView(this@SatelliteSpaceManagerActivity).apply {
                    text = "${space.icon} ${space.name}"
                    textSize = 18f
                    setTextColor(0xFFFFFFFF.toInt())
                })

                textContainer.addView(TextView(this@SatelliteSpaceManagerActivity).apply {
                    text = if (tabCount > 0) "$tabCount open tab(s)" else "No open tabs"
                    textSize = 13f
                    setTextColor(0xFF888888.toInt())
                })

                row.addView(textContainer)

                // Delete button (not for default space)
                if (space.id != SatelliteSpace.DEFAULT_SPACE_ID) {
                    row.addView(TextView(this@SatelliteSpaceManagerActivity).apply {
                        text = "🗑️"
                        textSize = 20f
                        setPadding(dp(16), 0, 0, 0)
                        setOnClickListener { showDeleteConfirmation(space) }
                    })
                }
            }
        }
    }
}
