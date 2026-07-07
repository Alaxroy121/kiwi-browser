package org.chromium.base;

/** Stub for Chromium's Log utility. */
public final class Log {
    private Log() {}

    public static void d(String tag, String msg) {}
    public static void e(String tag, String msg) {}
    public static void e(String tag, String msg, Throwable tr) {}
    public static void w(String tag, String msg) {}
    public static void i(String tag, String msg) {}
}
