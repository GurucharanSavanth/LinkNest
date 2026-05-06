# Code Audit

Date: 2026-05-06

## Verified Issues Addressed

### WARNING: Launch health gate blocked dashboard startup

- Evidence: `app/src/main/java/com/linknest/app/navigation/LinkNestNavHost.kt` used `LAUNCH_GATE_ROUTE` as `startDestination`; `LaunchGateRoute` required user action to continue or run health checks.
- Fix: `NavHost` now starts at `DASHBOARD_ROUTE`; obsolete launch-gate screen and ViewModel were removed after reference search showed no remaining uses.
- Risk of fix: Low. Background health scheduling remains in `DeferredStartupCoordinator` and `HealthWorkScheduler`.
- Validation: `.\gradlew.bat :app:assembleDebug --console=plain`, `.\gradlew.bat lintDebug --console=plain`, `.\gradlew.bat test --console=plain`.

### WARNING: URL input rejected practical non-web schemes

- Evidence: `UrlNormalizer` required `http`/`https`, rejected fragments, and `ValidateUrlAction` failed non-web schemes.
- Fix: Missing schemes still normalize to `https`; `http` still upgrades to `https`; custom schemes can be saved but are marked unsupported for metadata/health automation; unsafe schemes (`file`, `javascript`, `data`, `content`) remain blocked.
- Risk of fix: Medium. Custom-scheme links are saved but cannot be externally opened by current `openExternalWebsite`, which intentionally only opens `http`/`https`.
- Validation: `UrlNormalizerTest`, `.\gradlew.bat :core:network:testDebugUnitTest --console=plain`, `.\gradlew.bat test --console=plain`.

## Open Findings

### TESTING_GAP: Backup/restore lacks round-trip tests

- Evidence: `BackupManager` and `OfflineFirstBackupRepository` implement JSON/encrypted export/import, but no backup-specific tests were found.
- Proposed fix: Add serialization tests, corrupted payload tests, schema-version tests, and Room-backed export/import round-trip tests.
- Risk of fix: Medium to high because import touches categories, websites, tags, mappings, filters, events, and AppSearch rebuild.
- Validation command: `.\gradlew.bat test --console=plain`.

### SECURITY: Backup format has no checksum/header verification yet

- Evidence: `BackupManager.parse` validates required JSON arrays and max payload size, but there is no magic/header, checksum, or format-version envelope.
- Proposed fix: Introduce a documented envelope with `formatVersion`, `schemaVersion`, `exportedAt`, checksum, encryption, and compression metadata.
- Risk of fix: High if backward compatibility is not planned.
- Validation command: backup compatibility tests plus import round-trip tests.

### DOCUMENTATION_GAP: Dashboard/performance cleanup remains unaudited

- Evidence: Dashboard files are large and use hardcoded strings; no UI test or benchmark evidence was found in this pass.
- Proposed fix: Split dashboard cleanup into dedicated UI patch with Compose previews/tests, then add measured performance or document unavailable profiling.
- Risk of fix: Medium because dashboard is the primary workflow.
- Validation command: `.\gradlew.bat lintDebug --console=plain`, `.\gradlew.bat :app:assembleDebug --console=plain`.
