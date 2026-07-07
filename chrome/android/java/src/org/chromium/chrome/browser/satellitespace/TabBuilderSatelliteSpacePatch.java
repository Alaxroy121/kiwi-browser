/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 *
 * ============================================================================
 * PATCH: TabBuilder.java — Add Satellite Space support
 * ============================================================================
 *
 * Apply the following changes to TabBuilder.java:
 *
 *
 * ─── 1. Add import ───────────────────────────────────────────────────────
 *
 *   import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager;
 *
 *
 * ─── 2. Add field ────────────────────────────────────────────────────────
 *
 * Add alongside the existing fields:
 *
 *   private String mSatelliteSpaceId;
 *
 *
 * ─── 3. Add setter method ────────────────────────────────────────────────
 *
 *   /**
 *    * Sets the Satellite Space ID for the tab being built.
 *    * When set, the tab's WebContents will be created with an isolated
 *    * Chromium Profile that has its own cookies, cache, and session data.
 *    *
 *    * @param spaceId The Satellite Space ID, or null for the default space.
 *    * @return This [TabBuilder].
 *    */
 *   public TabBuilder setSatelliteSpaceId(@Nullable String spaceId) {
 *       mSatelliteSpaceId = spaceId;
 *       return this;
 *   }
 *
 *
 * ─── 4. Modify build() method ────────────────────────────────────────────
 *
 * In the build() method, after creating the TabImpl and before
 * calling initialize(), add:
 *
 *   // Set the Satellite Space binding before initialization
 *   if (mSatelliteSpaceId != null) {
 *       tab.setSatelliteSpaceId(mSatelliteSpaceId);
 *       // Persist the binding
 *       SatelliteSpaceManager.getInstance(tab.getContext())
 *           .bindTabToSpace(tab.getId(), mSatelliteSpaceId);
 *   }
 *
 *
 * ─── 5. Modify createFromFrozenState() ───────────────────────────────────
 *
 * When restoring tabs from saved state, preserve the space binding:
 *
 *   public static TabBuilder createFromFrozenState(
 *           @Nullable String satelliteSpaceId) {
 *       return new TabBuilder()
 *               .setLaunchType(TabLaunchType.FROM_RESTORE)
 *               .setCreationType(TabCreationState.FROZEN_ON_RESTORE)
 *               .setFromFrozenState(true)
 *               .setSatelliteSpaceId(satelliteSpaceId);
 *   }
 *
 *
 * ─── 6. Usage examples ───────────────────────────────────────────────────
 *
 * Creating a tab in the default space:
 *
 *   Tab tab = TabBuilder.createLiveTab(false)
 *       .setId(tabId)
 *       .setWindow(windowAndroid)
 *       .build();
 *
 * Creating a tab in a Satellite Space:
 *
 *   Tab tab = TabBuilder.createLiveTab(false)
 *       .setId(tabId)
 *       .setWindow(windowAndroid)
 *       .setSatelliteSpaceId("my-work-space")
 *       .build();
 *
 * Restoring a tab with its space binding:
 *
 *   Tab tab = TabBuilder.createFromFrozenState(tabState.satelliteSpaceId)
 *       .setId(tabId)
 *       .setWindow(windowAndroid)
 *       .setTabState(tabState)
 *       .build();
 */
package org.chromium.chrome.browser.satellitespace;

/**
 * Documentation-only class describing TabBuilder patches for Satellite Spaces.
 */
public class TabBuilderSatelliteSpacePatch {
    // See file header for patch instructions.
}
