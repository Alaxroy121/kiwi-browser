package org.chromium.base;

/** Stub for Chromium's ThreadUtils. */
public class ThreadUtils {
    public static void assertOnUiThread() {}
    public static boolean runningOnUiThread() { return true; }
    public static void postOnUiThread(Runnable r) { r.run(); }
}
