package org.chromium.base

/** Stub for Chromium's Log utility. */
object Log {
    @JvmStatic fun d(tag: String, msg: String) {}
    @JvmStatic fun e(tag: String, msg: String) {}
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable?) {}
    @JvmStatic fun w(tag: String, msg: String) {}
    @JvmStatic fun i(tag: String, msg: String) {}
}
