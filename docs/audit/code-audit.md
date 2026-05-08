# Code Audit

Date: 2026-05-06

## Verified Issues Addressed

### WARNING: Baseline profile referenced removed launch-gate class

- Evidence: `app/src/main/baseline-prof.txt` still included `Lcom/linknest/app/launch/LaunchGateViewModel;` after the launch gate was removed.
- Fix: Removed the stale class entry from the baseline profile.
- Risk of fix: Low. The class no longer exists, and the profile still keeps live startup/dashboard ViewModel entries.
- Validation: `.\gradlew.bat :app:assembleDebug --console=plain`, `.\gradlew.bat lintDebug --console=plain`, `.\gradlew.bat test --console=plain`.

### PERFORMANCE: Periodic health checks were allowed on any connected network

- Evidence: `HealthWorkScheduler` required only `NetworkType.CONNECTED` plus battery-not-low for periodic link health work.
- Fix: Periodic health checks now require `NetworkType.UNMETERED`, charging, and battery-not-low constraints.
- Risk of fix: Low to medium. Battery/network impact improves, but health maintenance may run less often on devices that are rarely charging on unmetered networks.
- Validation: `.\gradlew.bat :app:assembleDebug --console=plain`, `.\gradlew.bat lintDebug --console=plain`, `.\gradlew.bat test --console=plain`.

### INFO: Dashboard website rows lacked stable local keys and tile semantics

- Evidence: `DashboardScreen` rendered website tiles with `forEach` loops in list/grid branches, and clickable website tiles had no explicit tile-level semantics.
- Fix: Website tiles and smart-section sheet rows now use `key(website.id)`. List/grid tiles now expose button role, click labels, long-click labels, and concise content descriptions.
- Risk of fix: Low. This is a local Compose rendering/accessibility patch without data-flow changes.
- Validation: `.\gradlew.bat :app:assembleDebug --console=plain`, `.\gradlew.bat lintDebug --console=plain`, `.\gradlew.bat test --console=plain`.

### WARNING: Launch health gate blocked dashboard startup

- Evidence: Before the 2026-05-06 fix, `app/src/main/java/com/linknest/app/navigation/LinkNestNavHost.kt` used `LAUNCH_GATE_ROUTE` as `startDestination`; `LaunchGateRoute` required user action to continue or run health checks.
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

### DOCUMENTATION_GAP: Dashboard/performance cleanup remains incomplete

- Evidence: Dashboard files are large and use hardcoded strings; a narrow stable-key/accessibility patch landed, but the minimalist dashboard revamp and measured profiling remain open.
- Proposed fix: Split dashboard cleanup into dedicated UI patch with Compose previews/tests, then add measured performance or document unavailable profiling.
- Risk of fix: Medium because dashboard is the primary workflow.
- Validation command: `.\gradlew.bat lintDebug --console=plain`, `.\gradlew.bat :app:assembleDebug --console=plain`.
