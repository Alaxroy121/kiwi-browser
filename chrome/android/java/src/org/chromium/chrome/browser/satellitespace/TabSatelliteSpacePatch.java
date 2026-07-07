/*
 * Copyright 2024 The Kiwi Browser Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 *
 * ============================================================================
 * PATCH: TabImpl.java — Add satelliteSpaceId property
 * ============================================================================
 *
 * Apply the following changes to TabImpl.java:
 *
 * ─── 1. Add import ───────────────────────────────────────────────────────
 *
 * Add at the top of TabImpl.java:
 *
 *   import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager;
 *   import org.chromium.chrome.browser.satellitespace.SatelliteSpaceTabHelper;
 *
 *
 * ─── 2. Add field ────────────────────────────────────────────────────────
 *
 * Add after the existing field declarations (around line 130):
 *
 *   /**
 *    * The Satellite Space ID this tab belongs to, or null for the default space.
 *    * When set, the tab's WebContents will use an isolated Chromium Profile
 *    * with its own cookies, cache, and session data.
 *    *
 *    * @see SatelliteSpaceManager
 *    * /
 *   private @Nullable String mSatelliteSpaceId;
 *
 *
 * ─── 3. Add getter/setter ────────────────────────────────────────────────
 *
 * Add after the existing getId() method:
 *
 *   /**
 *    * Returns the Satellite Space ID this tab belongs to.
 *    * @return The space ID, or null if this tab is in the default space.
 *    */
 *   @Override
 *   @Nullable
 *   public String getSatelliteSpaceId() {
 *       return mSatelliteSpaceId;
 *   }
 *
 *   /**
 *    * Sets the Satellite Space ID for this tab.
 *    * This should be called before the tab's WebContents are created,
 *    * as the Profile is determined at WebContents creation time.
 *    *
 *    * @param spaceId The space ID, or null for the default space.
 *    */
 *   @Override
 *   public void setSatelliteSpaceId(@Nullable String spaceId) {
 *       mSatelliteSpaceId = spaceId;
 *   }
 *
 *
 * ─── 4. Modify WebContents creation ──────────────────────────────────────
 *
 * FIND this code in the initialize() method (around line 554):
 *
 *   Profile profile =
 *       IncognitoUtils.getProfileFromWindowAndroid(mWindowAndroid, isIncognito());
 *   webContents = WebContentsFactory.createWebContents(profile, isHidden());
 *
 * REPLACE with:
 *
 *   // Satellite Space: use isolated profile if tab is bound to a space
 *   Profile profile = SatelliteSpaceTabHelper.getProfileForTabStatic(
 *       getContext(), mId, mIncognito, mWindowAndroid);
 *   webContents = WebContentsFactory.createWebContents(profile, isHidden());
 *
 *
 * ─── 5. Also modify the second WebContents creation site ─────────────────
 *
 * FIND (around line 895):
 *
 *   Profile profile = IncognitoUtils.getProfileFromWindowAndroid(
 *       mWindowAndroid, isIncognito());
 *   webContents = WebContentsFactory.createWebContents(profile, initiallyHidden);
 *
 * REPLACE with:
 *
 *   Profile profile = SatelliteSpaceTabHelper.getProfileForTabStatic(
 *       getContext(), mId, mIncognito, mWindowAndroid);
 *   webContents = WebContentsFactory.createWebContents(profile, initiallyHidden);
 *
 *
 * ─── 6. Modify tab destruction to clean up binding ───────────────────────
 *
 * In the destroy() method, add before the existing cleanup:
 *
 *   // Clean up Satellite Space binding
 *   if (mSatelliteSpaceId != null) {
 *       SatelliteSpaceManager.getInstance(getContext()).unbindTab(mId);
 *   }
 *
 *
 * ─── 7. Modify tab save/restore ──────────────────────────────────────────
 *
 * In TabState (or wherever tab state is serialized), add satelliteSpaceId
 * to the persisted state so it survives app restarts.
 *
 * In TabState.java, add:
 *   public String satelliteSpaceId;
 *
 * In the save method:
 *   tabState.satelliteSpaceId = tab.getSatelliteSpaceId();
 *
 * In the restore method (TabBuilder):
 *   if (tabState.satelliteSpaceId != null) {
 *       tab.setSatelliteSpaceId(tabState.satelliteSpaceId);
 *   }
 */
package org.chromium.chrome.browser.satellitespace;

/**
 * Documentation-only class that describes the exact patches needed
 * for TabImpl.java to support Satellite Spaces.
 *
 * The actual patches are applied directly to TabImpl.java and Tab.java.
 * This file serves as a reference for developers integrating the feature.
 */
public class TabSatelliteSpacePatch {
    // This is a documentation/patch reference file.
    // See the file header comments for the complete patch instructions.
}
