# Repair status — 2026-11-15

Follow-up implementation and verification: [2026-11-16 repairs](followup-2026-11-16.md). The report below records the earlier patch's state at handoff.

Baseline: `0f9ad3a1017a6a261a44af61f4fe54465938a566`. Changes are applied to the working tree, without committing or publishing. The prior dated audit remains historical evidence. This report distinguishes implemented changes from verification still needed.

## Implemented behavior

| Audit | Change |
|---|---|
| 1 | Shared bounded OkHttp transport checks socket DNS addresses and every redirect. Metadata, cache downloads and health use it. Coil network fetchers were removed; UI remote image URLs no longer trigger downloads. |
| 2 | Move updates category/order without replacing fields. Merge preserves personal fields, category/order/title and combines tags. Missing duplicate targets fail rather than becoming a new save. Cancel returns before capture work. |
| 3 | Imports preview counts and require replacement confirmation. Previous library is atomically staged before replacement. Restore warnings are displayed. |
| 4 | External/staged readers enforce a limit while reading, before allocating complete input. Existing decompression limits remain. |
| 5 | Category deletion captures website IDs before cascade and removes their index documents. Search checks indexed IDs against Room and active categories. Session creation is serialized. |
| 6 | Raw encoded path/query/fragment survive normalization. Credential-bearing web URLs are rejected. Explicit HTTPS port 80 is preserved. |
| 7 | Filtered searches filter the full active library before constructing results; the global first-40 limit no longer removes eligible filtered hits. |
| 8 | Reorder while searching is rejected with feedback. Category moves use a Room transaction and preserve position when moving to the current category. |
| 9 | Dashboard receives batched duplicate counts matching the existing candidate identity predicates. |
| 10 | Settings lists archived categories with Restore actions. |
| 11 | Add/edit drafts and search criteria use SavedStateHandle. Restored criteria take precedence over original route defaults. The artificial save delay was removed. |
| 12 | Whole-library loading batches icon/tag/duplicate data instead of three extra reads per website. |
| 13 | Dashboard list/grid render keyed lazy rows, and search groups render individual lazy results. Whole-category size animation/eager child layout was removed. |
| 14 | Emoji selection takes precedence; remote sources use local cache; missing/failed dashboard cache images use a local fallback. |
| 15 | Restore validates IDs, names, references, ranges and filter enums before writes. Nonportable custom image URIs/cache paths are omitted with warnings. Device-local backup-folder preference remains preserved. |
| 16 | HEAD 405/501 receives a bounded GET fallback using the same guarded transport. |
| 17 | Activity widget refresh sends explicit widget update broadcasts. Providers finish real async broadcast results; redundant onEnabled calls were removed, and update failures are logged. |
| 18 | Both category color pickers have 48dp targets and selected/label semantics. Filter deletion has an independent IconButton. Status-bar icons use the app's dark appearance. Empty dashboard text points users toward search/archive recovery. |
| 19 | Migration 2→3 saves/restores tag links across parent-table replacement, including connections with foreign keys enabled or disabled. Added Android migration and restore rollback tests. |
| 20 | Removed unused AddWebsiteUseCase, bulk website insertion, dashboard navigation helper, smart-section sheet and unused Settings methods. README/backup docs now describe unencrypted JSON; AGENT.md points to authoritative AGENTS.md. |

Additional changes: search requests cancel stale work, failed icon temp files are cleaned, saved-filter unknown enums do not crash the observer, staged backups use AtomicFile, and “Last Imported” was renamed “Recently Added.”

Direct dependency rationale: OkHttp 4.12.0 was already present transitively through Coil. core/network now declares it directly because transport owns DNS enforcement, response bounds, redirects and cancellation. Android test runner dependencies were added to the database/data test modules because the library test APKs otherwise lacked AndroidJUnitRunner. No platform/library version sweep was performed.

## Verification

- JVM tests: 33 distinct tests, 66 debug/release executions, zero failures/errors/skips. Includes raw URI preservation, credential/port handling, actual address rejection, bounded reads and restore validation, alongside existing serializer/share tests.
- API 36 emulator, `emulator-5554`: `MigrationPreservationTest` passed, one test. It upgrades populated v2 to v6 and checks the tag relationship.
- API 36 emulator: `BackupRestoreTest` passed, one test. It round-trips populated website/tag records through the serializer/repository and injects an insertion failure after table clearing; the original rows and associations survive rollback.
- Debug application builds and installs. Dashboard starts directly, remains interactive, and its screenshot was inspected. White status-bar icons are visible on the dark background.
- Final host checks and artifact paths are appended after the last build. No user records were cleared, no emulator was wiped, and no production keys were used.

An early library instrumentation run failed because the runner dependency was missing. The first populated migration run then exposed duplicate restoration when the helper left foreign keys disabled; INSERT OR IGNORE fixed that compatibility case. Those failures were corrected before the passing runs. A stale Linux KSP cache was bypassed with `-Pksp.incremental=false`; no tracked machine paths were introduced.

## Practical limits and remaining work

- This patch does not establish exhaustive UI correctness. Installed-widget interaction, every duplicate-decision path, populated grid/list scrolling, process death, TalkBack, RTL, large font, and all API/OEM combinations need additional device coverage.
- The migration regression covers populated v2→v6, not populated fixtures for every supported origin. Lost associations from an upgrade that already happened cannot be reconstructed without an older backup.
- The restore test exercises website/tag transaction rollback; it does not simulate every document provider, disk-full condition, index interruption or post-commit preference failure. Room replacement is atomic; preferences/index/file operations are not one cross-store transaction.
- Recovery staging retains the previous library locally but subsequent exports/restores replace that staged slot. Export an external copy for durable backup. Imported device-local custom images are deliberately omitted rather than reading arbitrary imported paths.
- Bulk query/composition structure improved, but no production performance speedup, leak fix, or smooth-frame target is claimed. Fresh debug emulator startup includes compilation and host build contention; real-device/profileable measurements remain needed.
- Lint warnings remain, primarily unused resources, library/toolchain update notices, KTX suggestions and backup-policy/template resources. They are not suppressed. A full localization move, backup/device-transfer policy change, dependency upgrade and release shrinker optimization require dedicated compatibility work.
- AppSearch remains a derived index with updates after Room commits. Session creation is protected and deleted IDs are checked, but exhaustive concurrent rebuild/index fault injection is not covered.

The broad audit fixes are implemented; the remaining test matrix and low-priority suggestions above must not be described as fully verified or warning-free.

## Final handoff

The final freshness run finished successfully: `gradlew.bat test lintDebug :app:assembleDebug -Pksp.incremental=false --console=plain` (39 seconds, 944 tasks). Final reports contain 66 JVM test executions with zero failures/errors/skips. Lint has zero errors, 103 warning occurrences across module reports, and one hint. Most warnings predate the patch; the available-version inventory also changed during the multi-day session. No warnings were suppressed.

The installable output is `app/build/outputs/apk/debug/app-debug.apk` (debug variant). Final logs, screenshot, and patch are preserved under ignored `build/reports/repair-2026-11-15/`. The [combined patch](../../build/reports/repair-2026-11-15/linknest-audit-fixes.patch) includes source, regression tests, cleanup and documentation. It is already applied in this working tree; do not apply it again here. It can be applied to the stated baseline checkout.

The local SDK property was restored after validation. No commit, push, production release, or user-data replacement was performed.
