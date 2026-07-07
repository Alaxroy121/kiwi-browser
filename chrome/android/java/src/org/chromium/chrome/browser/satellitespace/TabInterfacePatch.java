/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 *
 * ============================================================================
 * PATCH: Tab.java (interface) — Add Satellite Space methods
 * ============================================================================
 *
 * Apply the following changes to the Tab interface:
 *
 *
 * ─── 1. Add import ───────────────────────────────────────────────────────
 *
 *   import androidx.annotation.Nullable;
 *
 *
 * ─── 2. Add method declarations ──────────────────────────────────────────
 *
 * Add these methods to the Tab interface:
 *
 *   /**
 *    * Returns the Satellite Space ID this tab belongs to.
 *    * @return The space ID, or null if this tab is in the default space.
 *    * /
 *   @Nullable
 *   String getSatelliteSpaceId();
 *
 *   /**
 *    * Sets the Satellite Space ID for this tab.
 *    * @param spaceId The space ID, or null for the default space.
 *    * /
 *   void setSatelliteSpaceId(@Nullable String spaceId);
 *
 *
 * ─── 3. Modify TabState.java ─────────────────────────────────────────────
 *
 * Add to TabState class for persistence:
 *
 *   /** Satellite Space ID this tab belongs to, or null for default. * /
 *   public String satelliteSpaceId;
 *
 *
 * ─── 4. Modify TabStateExtractor.java ────────────────────────────────────
 *
 * In the method that extracts tab state for persistence:
 *
 *   tabState.satelliteSpaceId = tab.getSatelliteSpaceId();
 *
 *
 * ─── 5. Modify tab restoration ───────────────────────────────────────────
 *
 * In the code that restores tabs from saved state (TabWindowManager,
 * TabModelImpl, or equivalent):
 *
 *   if (tabState.satelliteSpaceId != null) {
 *       // Ensure the space still exists before restoring binding
 *       SatelliteSpaceManager mgr = SatelliteSpaceManager.getInstance(context);
 *       if (mgr.getSpace(tabState.satelliteSpaceId) != null) {
 *           builder.setSatelliteSpaceId(tabState.satelliteSpaceId);
 *       }
 *   }
 */
package org.chromium.chrome.browser.satellitespace;

/**
 * Documentation-only class describing Tab interface patches for Satellite Spaces.
 */
public class TabInterfacePatch {
    // See file header for patch instructions.
}
