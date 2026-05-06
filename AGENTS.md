# Repository Guidelines

## Project Overview

LinkNest is a modular Android app for saving, organizing, searching, backing up, and maintaining a personal website library. Current code uses Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, Hilt, WorkManager, AndroidX AppSearch, Coil, and Jsoup. Architecture is offline-first: feature modules call action pipelines and data use cases, repositories coordinate Room/DataStore/AppSearch/network helpers, and `app` owns navigation, startup scheduling, widgets, shortcuts, and share entry.

## Repo Layout

- `app`: application entry point, navigation, widgets, shortcuts, share flow, startup scheduling.
- `core/common`: shared coroutine/time utilities.
- `core/model`: domain models and enums.
- `core/database`: Room database, DAOs, entities, migrations, schema exports.
- `core/datastore`: DataStore preferences.
- `core/network`: URL normalization, security policy, metadata fetch, health probes.
- `core/data`: repositories, use cases, storage, backup/import logic.
- `core/action`: action and pipeline orchestration plus workers.
- `core/designsystem`: Compose theme and shared components.
- `feature/*`: dashboard, add/edit, settings, and search screens.

## Build And Validation

Use Windows commands from repo root:

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat tasks --console=plain
.\gradlew.bat :core:network:testDebugUnitTest --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat lintDebug --console=plain
.\gradlew.bat :app:assembleDebug --console=plain
```

No ktlint or detekt tasks are currently exposed by `gradlew tasks`.

## Engineering Conventions

Keep changes small and module-scoped. Do not add dependencies without rationale. Use Kotlin/Compose/Room/Hilt idioms already present. Prefer immutable UI state, stable lazy-list keys, `viewModelScope`, injected dispatchers, Room transactions for multi-table writes, and migrations plus schema exports for database changes. Do not suppress warnings without documenting why.

## Safety And Done

Do not reintroduce launch gates that block dashboard startup. Do not broaden backup/restore behavior without round-trip tests and rollback validation. Done means relevant build/tests/lint passed, docs are updated, and final report lists changed behavior, failures, risks, and follow-ups.
