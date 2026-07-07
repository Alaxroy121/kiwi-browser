package org.chromium.chrome.browser.tab;

/** Stub for Chromium's TabObserver. */
public interface TabObserver {
    default void onActivityAttachmentChanged(Tab tab, Object window) {}
}
