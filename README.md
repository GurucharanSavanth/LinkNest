# LinkNest

LinkNest is an Android app for collecting, organizing, searching, and maintaining a personal library of websites.

It is built as a modular offline-first project with Jetpack Compose, Room, DataStore, Hilt, WorkManager, and AppSearch. The app focuses on fast local organization, metadata-aware capture, health monitoring, and lightweight maintenance workflows for saved links.

## What It Does

- Save websites into categories with icon support, notes, labels, pinning, and priority/follow-up state.
- Organize links with drag-and-drop style reordering, category moves, archive/delete flows, and smart dashboard sections.
- Search locally across titles, domains, categories, tags, notes, flags, and saved searches.
- Reopen filtered slices of the library with Smart Collections, recent queries, and saved searches.
- Run link health checks and review broken, redirected, timed-out, blocked, or login-required entries.
- Export and import backups as JSON or encrypted `.lnen` payloads.
- Track integrity metrics such as duplicate entries, unsorted links, cache size, and database size.
- Publish shortcuts and refresh app widgets for quick access.
- Accept shared content into the app through a dedicated share-entry flow.

## Main Screens

- Launch Gate
- Dashboard
- Add / Edit Website
- Search & Collections
- Settings
- Integrity Center
- Health Report

## Tech Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- Hilt
- Room
- DataStore
- WorkManager
- AndroidX AppSearch
- Coil
- Jsoup

## Requirements

- Android Studio with a valid Android SDK installation
- JDK 17
- Android `minSdk = 26`
- Android `targetSdk = 36`
- Android `compileSdk = 36`

## Project Structure

- `app`
  Android application entry point, navigation, widgets, shortcuts, share flow, and app-level wiring.
- `core/common`
  Shared utilities and cross-cutting helpers.
- `core/model`
  Shared domain models.
- `core/database`
  Room database, DAOs, entities, and migrations.
- `core/datastore`
  Persistent preferences storage.
- `core/network`
  Metadata fetching and URL health monitoring.
- `core/designsystem`
  Theme, surfaces, gradients, and reusable Compose components.
- `core/data`
  Offline-first repositories, storage helpers, backup logic, and use cases.
- `core/action`
  Higher-level actions, pipelines, and workers.
- `feature/dashboard`
  Library overview, category organization, and quick actions.
- `feature/addedit`
  Website capture and editing flow.
- `feature/search`
  Offline search, filters, smart collections, and saved searches.
- `feature/settings`
  Preferences, backup/restore, integrity, and health tooling.

## Build

From the project root:

```powershell
.\gradlew.bat :app:assembleDebug
```

To produce a release build:

```powershell
.\gradlew.bat :app:assembleRelease
```

## Local Setup

1. Open the project in Android Studio.
2. Make sure `local.properties` points at your Android SDK.
3. Sync Gradle.
4. Run the `app` configuration on a device or emulator.

## Useful Commands

```powershell
.\gradlew.bat test
.\gradlew.bat lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

## Troubleshooting

If Gradle reports that configuration cache cannot be reused, that is usually informational and not a failure.

If a Windows build fails with a locked dex file such as `classes.dex`, stop Gradle daemons and rerun:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :app:assembleRelease --console=plain
```

If Android Gradle Plugin complains about the Android user directory in a restricted environment, ensure a writable Android home is available before building.

## Visual Assets

- `linknest_logo.svg`
- `prototype 1.png`
- `Prototype 2.png`
