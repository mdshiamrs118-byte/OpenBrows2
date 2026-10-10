# OpenBrows

A modern, glassmorphic Android browser built with Kotlin. Package `com.openbrows.app`, minSdk 29 (Android 10), targetSdk 34.

## Features
- Glassmorphic, theme-aware UI (light/dark) with translucent bars, sliding panel, animated popup menu (background blur on Android 12+)
- Per-tab WebViews: switching tabs keeps each tab's page and history
- URL bar at the bottom or top, 3-dot menu, tabs button
- Tab layouts: Off (sliding panel from the tabs button), bottom horizontal, left vertical, right vertical
- Hide URL toolbar: Off / On (hides while scrolling down) / On + lock (stays hidden; use the floating button to reveal it)
- Settings: default homepage, website text size, UI item size, layout size, URL bar position, tabs position, toolbar hiding
- Downloads via DownloadManager, bookmarks (defaults + your own), opens http/https links from other apps

## Build
Push to GitHub: the `Android CI` workflow (`.github/workflows/android.yml`) builds `app-debug.apk` and uploads it as an artifact.

Locally (JDK 17 + Android SDK): `./gradlew assembleDebug`
