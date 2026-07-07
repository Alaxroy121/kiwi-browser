package org.chromium.chrome.browser.tab.state;

/** Stub for Chromium's CriticalPersistedTabData. */
public class CriticalPersistedTabData {
    public static boolean isEmptySerialization(Object data) { return true; }
    public static void build(Object tab, Object data) {}
    public static CriticalPersistedTabData from(Object tab) { return new CriticalPersistedTabData(); }
    public int getParentId() { return -1; }
}
