package org.chromium.chrome.browser.tab;

import androidx.annotation.Nullable;

/** Stub for Chromium's Tab interface. */
public interface Tab {
    int INVALID_TAB_ID = -1;

    int getId();
    boolean isIncognito();

    @Nullable
    String getSatelliteSpaceId();

    void setSatelliteSpaceId(@Nullable String spaceId);
}
