package org.chromium.chrome.browser.profiles;

/** Stub for Chromium's Profile class. */
public class Profile {
    public String getProfileName() { return ""; }
    public boolean isOffTheRecord() { return false; }

    /** Returns or creates a named profile with isolated data. */
    public static Profile getProfile(String name) { return new Profile(); }
}
