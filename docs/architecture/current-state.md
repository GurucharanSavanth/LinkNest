# Current State

LinkNest is an offline-first modular Android application. The app now launches directly into the dashboard. Startup background work is coordinated separately by `DeferredStartupCoordinator`, which observes `backgroundHealthChecksEnabled` from DataStore and syncs periodic WorkManager health work through `HealthWorkScheduler`.

## Screen Flow

- Dashboard is the app start destination.
- Dashboard opens add/edit, search, settings, health report, and external web links.
- Add/edit uses URL normalization, validation, metadata fetch, category suggestion, duplicate review, icon resolution, and persistence pipelines.
- Settings exposes backup/export, import/restore, integrity center, and health report.
- Search exposes offline search, filtering, saved searches, and smart collections.

## Data Flow

- Feature ViewModels call `core:action` pipelines or `core:data` use cases.
- Repositories in `core:data` coordinate Room DAOs, DataStore, AppSearch, metadata fetchers, and storage helpers.
- Room entities and migrations live in `core:database`; exported schemas currently exist for versions 1 through 6.
- URL metadata and health checks use `core:network`; URL security blocks local/private web targets and unsafe schemes.

## Build Configuration

The project uses Gradle 8.13, Android Gradle Plugin 8.13.0, Kotlin 2.2.10, JDK 17, `compileSdk = 36`, `targetSdk = 36`, and `minSdk = 26`.
