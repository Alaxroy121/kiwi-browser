# 🛰️ Satellite Space — Integration Guide

## Overview

Satellite Space adds **multi-profile data isolation** to Kiwi Browser. Each Space is a fully isolated browsing container with its own cookies, cache, web storage, and session data. Tabs in Space A cannot see data from Space B or the Default space.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Kiwi Browser                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │           SatelliteSpaceManager                   │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐       │   │
│  │  │ Space A  │  │ Space B  │  │ Default  │       │   │
│  │  │ Profile  │  │ Profile  │  │ Profile  │       │   │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘       │   │
│  │       │              │              │             │   │
│  │  ┌────▼─────┐  ┌────▼─────┐  ┌────▼─────┐       │   │
│  │  │ Cookies  │  │ Cookies  │  │ Cookies  │       │   │
│  │  │ Cache    │  │ Cache    │  │ Cache    │       │   │
│  │  │ Storage  │  │ Storage  │  │ Storage  │       │   │
│  │  │ IndexedDB│  │ IndexedDB│  │ IndexedDB│       │   │
│  │  └──────────┘  └──────────┘  └──────────┘       │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │                    Tab Layer                       │   │
│  │  Tab1(space=A)  Tab2(space=B)  Tab3(default)     │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## How Profile Isolation Works

Chromium's `Profile` system creates separate data directories on disk:

```
<app_data>/app_chrome/Default/           ← Default space
<app_data>/app_chrome/satellite_space_abc/  ← Space "abc"
<app_data>/app_chrome/satellite_space_def/  ← Space "def"
```

Each directory contains its own:
- `Cookies` — SQLite database with all cookies
- `Cache/` — HTTP disk cache
- `Local Storage/` — localStorage and sessionStorage
- `IndexedDB/` — IndexedDB databases
- `Service Worker/` — Service Worker registrations and caches
- `Login Data` — Saved passwords and credentials

## Files Created

```
chrome/android/java/src/org/chromium/chrome/browser/satellitespace/
├── SatelliteSpace.kt                          # Data model
├── SatelliteSpacePreferences.kt               # SharedPreferences persistence
├── SatelliteSpaceManager.kt                   # Core manager (singleton)
├── SatelliteSpaceTabHelper.kt                 # Tab ↔ Profile bridge
├── TabSatelliteSpacePatch.java                # TabImpl.java patches (docs)
├── TabBuilderSatelliteSpacePatch.java         # TabBuilder.java patches (docs)
├── TabInterfacePatch.java                     # Tab interface patches (docs)
└── ui/
    ├── SatelliteSpaceSelectorView.kt          # Horizontal space selector
    ├── CreateSatelliteSpaceDialog.kt          # Create space dialog
    └── SatelliteSpaceTabSwitcherDecorator.kt  # Tab switcher grouping
```

## Step-by-Step Integration

### Step 1: Add the Tab Interface Methods

**File:** `chrome/android/java/src/org/chromium/chrome/browser/tab/Tab.java`

Add to the Tab interface:

```java
import androidx.annotation.Nullable;

// Add these method declarations:
@Nullable
String getSatelliteSpaceId();

void setSatelliteSpaceId(@Nullable String spaceId);
```

### Step 2: Add the Field to TabImpl

**File:** `chrome/android/java/src/org/chromium/chrome/browser/tab/TabImpl.java`

```java
// Add imports:
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager;
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceTabHelper;

// Add field (around line 130, with other fields):
private @Nullable String mSatelliteSpaceId;

// Add getter/setter:
@Override
@Nullable
public String getSatelliteSpaceId() {
    return mSatelliteSpaceId;
}

@Override
public void setSatelliteSpaceId(@Nullable String spaceId) {
    mSatelliteSpaceId = spaceId;
}
```

### Step 3: Modify WebContents Creation in TabImpl

**File:** `TabImpl.java` — Find the `initialize()` method.

**Before (line ~554):**
```java
Profile profile =
    IncognitoUtils.getProfileFromWindowAndroid(mWindowAndroid, isIncognito());
webContents = WebContentsFactory.createWebContents(profile, isHidden());
```

**After:**
```java
// Satellite Space: use isolated profile if tab is bound to a space
Profile profile = SatelliteSpaceTabHelper.getProfileForTabStatic(
    getContext(), mId, mIncognito, mWindowAndroid);
webContents = WebContentsFactory.createWebContents(profile, isHidden());
```

**Repeat for the second site (line ~895):**

**Before:**
```java
Profile profile = IncognitoUtils.getProfileFromWindowAndroid(
    mWindowAndroid, isIncognito());
webContents = WebContentsFactory.createWebContents(profile, initiallyHidden);
```

**After:**
```java
Profile profile = SatelliteSpaceTabHelper.getProfileForTabStatic(
    getContext(), mId, mIncognito, mWindowAndroid);
webContents = WebContentsFactory.createWebContents(profile, initiallyHidden);
```

**And the third site (line ~1528):**

Same pattern — replace `IncognitoUtils.getProfileFromWindowAndroid(...)` with the SatelliteSpaceTabHelper call.

### Step 4: Add Space Cleanup to Tab Destruction

**File:** `TabImpl.java` — In the `destroy()` method:

```java
// Add before existing cleanup:
if (mSatelliteSpaceId != null) {
    SatelliteSpaceManager.getInstance(getContext()).unbindTab(mId);
}
```

### Step 5: Modify TabBuilder for Space Support

**File:** `TabBuilder.java`

```java
// Add import:
import org.chromium.chrome.browser.satellitespace.SatelliteSpaceManager;

// Add field:
private String mSatelliteSpaceId;

// Add setter:
public TabBuilder setSatelliteSpaceId(@Nullable String spaceId) {
    mSatelliteSpaceId = spaceId;
    return this;
}

// In build() method, after creating TabImpl and before initialize():
if (mSatelliteSpaceId != null) {
    tab.setSatelliteSpaceId(mSatelliteSpaceId);
    SatelliteSpaceManager.getInstance(tab.getContext())
        .bindTabToSpace(tab.getId(), mSatelliteSpaceId);
}
```

### Step 6: Persist Space Binding in Tab State

**File:** `TabState.java` (or equivalent tab state class)

```java
// Add field:
public String satelliteSpaceId;
```

**File:** `TabStateExtractor.java`

```java
// In the method that extracts tab state:
tabState.satelliteSpaceId = tab.getSatelliteSpaceId();
```

**File:** Tab restoration code

```java
// When restoring from saved state:
if (tabState.satelliteSpaceId != null) {
    SatelliteSpaceManager mgr = SatelliteSpaceManager.getInstance(context);
    if (mgr.getSpace(tabState.satelliteSpaceId) != null) {
        builder.setSatelliteSpaceId(tabState.satelliteSpaceId);
    }
}
```

### Step 7: Add UI to Tab Switcher

**Option A: Add Space Selector to Tab Switcher**

In your tab switcher layout XML:

```xml
<org.chromium.chrome.browser.satellitespace.ui.SatelliteSpaceSelectorView
    android:id="@+id/satellite_space_selector"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

In your tab switcher fragment/activity:

```kotlin
val selector = findViewById<SatelliteSpaceSelectorView>(R.id.satellite_space_selector)
val manager = SatelliteSpaceManager.getInstance(context)
selector.initialize(manager)
selector.setOnSpaceSelectedListener { space ->
    // Filter tabs by space
    val filteredTabs = if (space.id == SatelliteSpace.DEFAULT_SPACE_ID) {
        allTabs
    } else {
        allTabs.filter { manager.getSpaceForTab(it.id) == space.id }
    }
    adapter.submitList(filteredTabs)
}
```

**Option B: Add Colored Indicators to Tab Cards**

```kotlin
// In your tab adapter's onBindViewHolder:
val space = manager.getSpaceForTabOrDefault(tab.id)
if (space.id != SatelliteSpace.DEFAULT_SPACE_ID) {
    holder.spaceIndicator.visibility = View.VISIBLE
    holder.spaceIndicator.setBackgroundColor(space.color)
    holder.spaceLabel.text = "${space.icon} ${space.name}"
} else {
    holder.spaceIndicator.visibility = View.GONE
}
```

**Option C: Add "Create Space" to Menu**

```kotlin
// In your options menu or toolbar:
menu.findItem(R.id.create_satellite_space)?.setOnMenuItemClickListener {
    CreateSatelliteSpaceDialog.show(context, manager) { newSpace ->
        selector.refreshSpaces()
    }
    true
}
```

### Step 8: Add Tab-to-Space Assignment UI

When creating a new tab, show a space picker:

```kotlin
fun showNewTabDialog(context: Context, manager: SatelliteSpaceManager) {
    val spaces = manager.getAllSpaces()
    val names = spaces.map { "${it.icon} ${it.name}" }.toTypedArray()

    AlertDialog.Builder(context)
        .setTitle("New Tab in Space")
        .setItems(names) { _, which ->
            val space = spaces[which]
            createNewTab(spaceId = space.id)
        }
        .show()
}
```

## Testing Checklist

- [ ] Create two Satellite Spaces (A and B)
- [ ] Open a tab in Space A, log into a website
- [ ] Open a tab in Space B, verify the website shows as logged out
- [ ] Verify cookies are isolated (check via DevTools)
- [ ] Verify localStorage is isolated
- [ ] Close and reopen the app, verify spaces persist
- [ ] Verify tabs restore with correct space bindings
- [ ] Delete a space, verify tabs move to default
- [ ] Verify the tab switcher shows space indicators
- [ ] Test incognito tabs (should use default space regardless)

## Build Notes

1. The Satellite Space files are pure Kotlin/Java — no native code changes needed.
2. The `Profile.getProfile(name)` API is available in Chromium M110+ (which Kiwi uses).
3. SharedPreferences is used for persistence — no database migration needed.
4. The UI components are optional — the core isolation works without them.

## Performance Considerations

- Each active Profile maintains its own cookie store and cache, so memory usage scales with the number of spaces.
- Profile creation is lazy — a Profile's native counterpart is only created when the first WebContents uses it.
- Consider limiting the maximum number of Satellite Spaces (e.g., 10) to prevent excessive resource usage.
