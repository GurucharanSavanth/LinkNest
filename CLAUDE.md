# LinkNest — Project Reference

## 1. PROJECT OVERVIEW

**Name:** LinkNest
**Purpose:** Offline-first Android bookmark manager with health checks, backup/restore, search, categories, widgets, share-intent support.
**Architecture:** Clean Architecture + MVVM + MVI (dashboard) + Repository Pattern
**UI:** Jetpack Compose + Navigation Compose
**DI:** Hilt
**Persistence:** Room + DataStore + AppSearch
**Background:** WorkManager
**Networking:** Jsoup (metadata) + OkHttp (health checks)
**Image Loading:** Coil

## 2. MODULE DEPENDENCY GRAPH

```
:app
├── :core:common
├── :core:model
├── :core:database
├── :core:datastore
├── :core:network
├── :core:designsystem
├── :core:data
├── :core:action
├── :feature:dashboard
├── :feature:addedit
├── :feature:settings
└── :feature:search
```

## 3. SCREEN FLOW DIAGRAM

```
Splash → Dashboard → Add/Edit → Search → Settings
         ↓           ↓           ↓
      Categories   Metadata    Filters
```

## 4. DATA FLOW DIAGRAM

```
UI → ViewModel → UseCase → Repository → DAO → Room DB
     ↓           ↓          ↓          ↓
 StateFlow    Result<T>  Flow<T>    @Transaction
```

## 5. BUILD CONFIGURATION

- minSdk: 26 | targetSdk: 36 | compileSdk: 36 | JDK: 17
- Gradle: 8.9 (wrapper)
- AGP: 8.7.3

## 6. CRITICAL PATHS

- **App Launch:** MainActivity → NavHost → Dashboard (no health gate)
- **Add Website:** Share Intent / Manual URL → Metadata Fetch → Save to Room
- **Backup:** Export tables + preferences → Encrypt → User-selected storage
- **Restore:** Decrypt → Validate schema → Transactional import

## 7. KNOWN ISSUES

- [x] URL validation now lenient (accepts any URL format) - PATCH 2.5 done
- [ ] Backup needs SAF file picker instead of internal storage
- [x] Dashboard uses Material Surface instead of decorative GlassPanel - PATCH 2.3 partial
- [ ] No @Stable/@Immutable on some data classes
- [x] LazyColumn already has keys for items
- [ ] Missing indices on some Room columns

## 8. PERFORMANCE BASELINE

- App start: < 1.5s cold
- Scroll: 90th percentile < 16ms (target)
- Memory: < 80MB average

## 9. SECURITY CHECKLIST

- Backup encryption: AES-256-GCM via BackupCryptoManager
- SQL injection prevention: Room parameterized queries
- R8 rules: Enabled for release
- URL validation: Whitelist-based (HttpSecurityPolicy)