# KnowlilyBrowser

A lightweight Android browser built on System WebView, featuring Material Design 2, dynamic color theming, plugin system, and multi-tab browsing.

## Features

- **Core browsing** — Android System WebView with JavaScript, DOM Storage, mixed content blocking
- **Multi-tab** — Tab bar with add/close/switch, single WebView instance with LRU state cache (5 states)
- **Incognito mode** — Per-tab privacy: no history, no cache, cookie isolation on tab switch
- **Bookmarks** — Add/view/clear/import/export (JSON), Room-backed with Flow
- **History** — Browsing history with search, Room-backed with Flow
- **Address autocomplete** — Debounced suggestions from history + bookmarks
- **Find in page** — Built-in find bar with match count and prev/next navigation
- **Pull to refresh** — SwipeRefreshLayout on every page
- **HTTPS-Only mode** — Auto-upgrade HTTP to HTTPS
- **Theme system** — Light / Dark / Follow system + 7 accent color presets + Material You (API 31+)
- **User-Agent switching** — Desktop vs mobile mode
- **Floating bottom bar** — Rounded card that auto-hides on scroll
- **Download manager** — System DownloadManager with completion notifications
- **Plugin system** — Extensible via `BrowserPlugin` interface with lifecycle hooks
  - Built-in: AdBlocker, Dark Mode (CSS injection)
  - User-installed: JS/CSS/AdBlock plugins from URL or local JSON file
- **Error handling** — Custom error page with retry, SSL errors blocked by default
- **Popup windows** — `window.open` handled in overlay with hardened WebView

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.22 |
| Build | Gradle 8.5, AGP 8.3.2, KSP |
| DI | Hilt 2.50 |
| Database | Room 2.6.1 + Kotlin Flow |
| Architecture | MVVM + Repository, single-Activity multi-Fragment |
| UI | Material Design 2 (1.13.0), ViewBinding, Navigation |
| Min/Target SDK | API 24 (Android 7.0) / API 34 (Android 14) |

## Download

Latest APK from [GitHub Releases](https://github.com/knowlily/listen-to/releases).

## Project Structure

```
app/
├── src/main/java/com/knowlily/browser/
│   ├── MainActivity.kt              # Single Activity, Fragment host + bottom nav
│   ├── KnowlilyApplication.kt       # @HiltAndroidApp entry point
│   ├── di/
│   │   ├── AppModule.kt             # Hilt module (singleton component)
│   │   ├── DatabaseModule.kt        # Room DB + DAO providers
│   │   └── SettingsRepositoryEntryPoint.kt  # EntryPoint for pre-onCreate injection
│   ├── data/
│   │   ├── AppDatabase.kt           # Room database
│   │   ├── HistoryDao.kt
│   │   └── BookmarksDao.kt
│   ├── model/
│   │   ├── HistoryItem.kt
│   │   ├── BookmarkItem.kt
│   │   └── TabItem.kt
│   ├── ui/
│   │   ├── BrowserFragment.kt       # Main browser UI (tabs + WebView + find bar)
│   │   ├── HistoryFragment.kt
│   │   ├── BookmarksFragment.kt
│   │   └── SettingsFragment.kt
│   ├── viewmodel/
│   │   ├── BrowserViewModel.kt      # Tabs, navigation, suggestions
│   │   ├── HistoryViewModel.kt
│   │   ├── BookmarksViewModel.kt
│   │   └── SettingsViewModel.kt     # Theme, plugins, cache, install
│   ├── repository/
│   │   ├── SettingsRepository.kt    # SharedPreferences, @Singleton @Inject
│   │   ├── HistoryRepository.kt
│   │   └── BookmarksRepository.kt
│   ├── adapter/
│   │   ├── HistoryAdapter.kt
│   │   ├── BookmarksAdapter.kt
│   │   └── TabAdapter.kt
│   └── plugin/
│       ├── BrowserPlugin.kt         # Plugin interface
│       ├── PluginManager.kt         # @Singleton @Inject, lifecycle + JS injection
│       ├── AdBlockerPlugin.kt
│       ├── DarkModePlugin.kt
│       ├── UserPlugin.kt
│       └── UserPluginRepository.kt
├── src/main/res/
│   ├── layout/                      # Fragment + item layouts
│   ├── values/                      # strings.xml (English default), colors, styles
│   ├── drawable/                    # Vector icons
│   ├── menu/                        # Toolbar + bottom nav menus
│   └── xml/                         # network_security_config, backup_rules
├── build.gradle                     # minifyEnabled true, R8 + ProGuard
└── AndroidManifest.xml
```

## Build

### Prerequisites

- JDK 17
- Android SDK 34 with build-tools 34.0.0
- (Optional) `signing.properties` at project root for release builds:
  ```properties
  storeFile=keystore.jks
  storePassword=your_password
  keyAlias=your_alias
  keyPassword=your_password
  ```

### Commands

```bash
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (R8 minified, requires signing.properties)
./gradlew installDebug       # Install to connected device
```

## Permissions

| Permission | Reason |
|-----------|--------|
| `INTERNET` | Web browsing |
| `ACCESS_NETWORK_STATE` | Connectivity check before loading |
| `WRITE_EXTERNAL_STORAGE` (API < 29) | File downloads |

## Compatibility

- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 14 (API 34)
- **Dynamic colors**: Android 12+ (API 31+)

## License

MIT License
