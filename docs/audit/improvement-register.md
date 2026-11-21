# LinkNest Improvement Register (2026-07-20)

Historical report. See the [current audit index](README.md) and [remaining work ledger](remaining-work-status.md); listed findings are not automatically current defects.

Baseline: `main` @ d2a691f. Build/test/lint green (13 tests, 0 fail; 101 lint warnings, 0 errors).
Env: OpenJDK 21, Gradle 8.13, AGP 8.13.0, Kotlin 2.2.10.

Severity-ranked. `V`=verified, `P`=probable, `H`=hypothesis.

## Critical

| ID | File | Issue | Conf |
|----|------|-------|------|
| C1 | core/network/UrlMetadataFetcher.kt:26-33 | SSRF: Jsoup GET fires **before** validateResolvedUrl; lenient normalize (the only prod path) keeps private/loopback hosts. Share→Add reaches it. | V |
| C1b | core/network/UrlMetadataFetcher.kt:29 | followRedirects(true) → public URL 302→127.0.0.1/169.254.169.254 fetched before any check. | V |

## Major

| ID | File | Issue | Conf |
|----|------|-------|------|
| M1 | core/data/storage/IconStorageManager.kt:29,33 | Icon fetch SSRF sibling: gates initial host via isHostAllowed (name-only) but instanceFollowRedirects=true → redirect to private host; og:image/rel=icon are attacker-controlled. | V |
| M2 | core/data/backup/BackupSerializer.kt:91-92 | Gzip-bomb: gunzip fully materialized before MAX_CHARS check; ~24MB compressed → huge heap. | V |
| M3 | core/data/backup/BackupSerializer.kt:112,132,144-145 | enum valueOf throws on unknown value → newer-app backup aborts entire import (healthStatus at :141 already lenient). | V |
| M4 | core/data/backup/BackupSerializer.kt:207-214 | strict getString/getInt/getBoolean on optional prefs block → missing key throws, kills whole import though repo treats prefs as optional. | V |
| M5 | OfflineFirstBackupRepository.kt:438-444 + LinkNestPreferencesDataSource.kt:72-73 | Every import silently unsets user's backupFolderUri (toUserPreferences omits it → remove()). | V |
| M6 | feature/settings/SettingsScreen.kt:352-377 | Dead "auto-save folder" feature: pref persisted, zero writers. Interacts w/ M5. | V |
| M7 | feature/settings/SettingsScreen.kt:114-119 | SAF picker double-launch on rotation (status stays ReadyToSave while picker open). | P |

## Moderate (selected)

- Process-death state loss: AddEditViewModel form, SearchViewModel query/filters, Dashboard sheet targets (remember not saveable/SavedStateHandle). V
- AddEditViewModel:179-213 edit with deleted id → silent blank Add form. V
- Dashboard: archive category no confirm/undo; zero-category empty state missing. V
- a11y: color swatches + filter-chip delete no semantics/48dp target. V
- DB schema DEFAULT drift (DatabaseMigrations vs schema json). V
- icon_cache no FK → orphan rows/files on category delete. V
- OfflineFirstTagRepository:74-80 non-atomic 2-table delete (no withTransaction). V
- LOWER(name) queries defeat indices (Tag/Category/RecentQuery DAO). V
- ShareCaptureUseCases:16-26 regex on full sharedText on Main before size cap → ANR. P
- UrlSecurityPolicy DNS-rebind: name validated, IP never (residual after resolved-IP check). P

## Dead code

- BackupCryptoManager.kt (76 lines) — zero prod callers post-encryption-strip.
- BackupManager.kt:15 `encrypted` param; BackupModels isEncrypted / encryptedBackupsEnabled — always false.
- UrlNormalizer.normalizeStrict + strict branch — zero prod callers.
- WebsiteDao.insertWebsites(List) — zero callers.
- DashboardActionSheets SmartSectionSheet (~46 lines); DashboardNavigation.navigateToDashboard.
- SettingsViewModel onTileSizeSelected / resetExportStatus / resetImportStatus.
- repo_manifest.txt — stale Windows dir listing.

## Doc drift

- CLAUDE.md: AGP 8.7.3/Gradle 8.9 (actual 8.13.0/8.13); "AES-256" (keystore default 128).
- 5 docs still describe stripped .lnen encryption + BackupCryptoManager.

## Missing tests

- No Room migration tests (schemas 1..6, zero MigrationTestHelper).
- Backup: malformed JSON, unknown enum, missing prefs key, gzip bomb, folderUri preservation, restore rollback.
- SSRF: requirePublicWebTarget rejects private/loopback literals.
- No feature/* ViewModel tests.
