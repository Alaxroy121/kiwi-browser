package org.chromium.chrome.browser.incognito;

import org.chromium.chrome.browser.profiles.Profile;

/** Stub for Chromium's IncognitoUtils. */
public class IncognitoUtils {
    public static Profile getProfileFromWindowAndroid(Object window, boolean incognito) {
        return new Profile();
    }
}
