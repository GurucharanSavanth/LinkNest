# Validation evidence — 2026-11-14

Baseline: `0f9ad3a1017a6a261a44af61f4fe54465938a566`. Only audit documentation was added. The ignored Linux SDK path in local.properties was temporarily replaced for Windows commands and restored afterward. Existing tracked source was not changed.

## Host checks

- Gradle 8.13, AGP 8.13.0, Kotlin 2.2.10, JetBrains JDK 21 selected by daemon criteria. Android SDK/build tools 36.0.0 available. SDK platform 36. Java source/target compatibility remains 17.
- `gradlew.bat test lintDebug :app:assembleDebug --console=plain`: tests and debug assembly passed. The combined command failed on a PropertyEscape lint error introduced by the audit's temporary unescaped Windows SDK path, not by tracked application source.
- Corrected temporary SDK property to `sdk.dir=C\:/Users/SavanthGc/AppData/Local/Android/Sdk`. A normal lint retry reused the previous analysis result and repeated the stale error.
- Forced only app lint analysis/report/check tasks to rerun: `gradlew.bat :app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug --rerun --console=plain`. Passed in 14 seconds. Other module lint tasks passed in the original workflow.
- **24 distinct JVM tests, 48 executions across debug/release: zero failures, errors or skips.** Network: 13 per variant; data: 10 per variant; app: one per variant. Most modules have no tests. No functional instrumentation was run; the existing instrumentation source only checks the package name.
- Build emitted a native symbol stripping notice for `libandroidx.graphics.path.so`, `libdatastore_shared_counter.so`, and `libicing.so`. They were packaged unchanged. This is not a compilation failure.

Logs: [original host run](../../build/reports/deep-scan-2026-11-14/host-checks.log), [cached lint retry](../../build/reports/deep-scan-2026-11-14/lint-retry.log), [successful lint rerun](../../build/reports/deep-scan-2026-11-14/lint-final.log).

## Lint inventory

Final app report: **95 warnings, one hint, zero errors**. Module-local reports add seven warnings: two Add/Edit UseKtx, two dashboard UseKtx, one settings UseKtx, one action UseKtx, and one model UseTomlInstead. Aggregate report occurrences: **102 warnings, one hint**; do not equate this with 102 functional bugs.

| App issue | Count | Disposition |
|---|---:|---|
| UnusedResources | 50 | Verify references; reuse relevant strings in feature resources, remove genuinely obsolete resources |
| GradleDependency | 19 | Version notices, not automatic upgrade instructions |
| NewerVersionAvailable | 11 | Version notices; assess separately |
| AndroidGradlePluginVersion | 3 | Toolchain update notices; no blanket upgrade |
| UseKtx | 5 | Low-risk style cleanup when touching callers |
| Overdraw | 3 | Widget XML backgrounds; inspect host rendering before deletion |
| OldTargetApi | 1 | Review target requirements separately; SDK change requires compatibility testing |
| UnusedAttribute | 1 | API-gated manifest attribute; not proof of broken older-API behavior |
| DataExtractionRules | 1 | Define desired device-transfer/cloud-backup policy; existing XML files are not wired |
| TypographyEllipsis | 1 | Replace punctuation when resource is used, or delete if truly obsolete |
| ReportShortcutUsage | 1 hint | Optional shortcut usage reporting |

Exact locations and messages: [preserved lint XML](../../build/reports/deep-scan-2026-11-14/lint-results-debug.xml). Unused candidates include app_widget_linknest layout, unused backup/data-extraction XML, template colors, and many strings replaced by hardcoded Compose text. Do not delete backup policy files without first deciding and wiring the intended policy. No blanket suppression or baseline was added.

## Device smoke checks

- Existing AVD `Pixel_10_Pro_XL`, serial `emulator-5554`, x86_64, Android 16/API 36. No device existed at start; the existing AVD was launched without wiping or saving a new snapshot.
- Fresh `app/build/outputs/apk/debug/app-debug.apk` installed successfully. APK metadata: package `com.linknest.app`, version 0.1.0/code 1, minSdk 26, targetSdk 36.
- APK SHA-256: `48DCF0B93BDDB0B4D091BB5035ADD08539F00EF630D4E8501F5C90E0B868F218`.
- `apksigner verify --print-certs` passed; signer is Android Debug. The host tool emitted a Java native-access warning. This is not production signing validation.
- Cold launch reached an interactive dashboard with Inbox and zero websites; no launch gate. `am start -W` reported TotalTime 6862ms, WaitTime 6893ms, one run. This is first debug launch on a running emulator while host build work was active; it is not a production startup benchmark.
- Used current UI-tree bounds to open Settings, return to dashboard, open Add Website, and return. Screens rendered and screenshots were inspected. No user links were created or replaced. No crashes/ANRs matched the bounded PID-scoped log sample; that is not proof all flows are crash-free.
- Observed black status-bar time/icons against the dark app background on all three screens. Other visible content was readable at the tested configuration. Full accessibility/large-font/RTL and populated-list QA remain open.
- Screenshots: [dashboard](../../build/reports/deep-scan-2026-11-14/dashboard.png), [settings](../../build/reports/deep-scan-2026-11-14/settings.png), [add website](../../build/reports/deep-scan-2026-11-14/add.png).
- UI trees and bounded log: [dashboard tree](../../build/reports/deep-scan-2026-11-14/dashboard-ui.xml), [settings tree](../../build/reports/deep-scan-2026-11-14/settings-ui.xml), [add tree](../../build/reports/deep-scan-2026-11-14/add-ui.xml), [app log](../../build/reports/deep-scan-2026-11-14/app-logcat.txt).

## Focused performance evidence

Flow: stable empty dashboard → Settings → dashboard. Reset gfxinfo immediately before navigation and captured it after returning. One run, debug build, emulator, host compilation/lint active during the session.

- 86 rendered frames; 19 janky frames (22.09%); median 25ms, p90 53ms, p95 125ms, p99 750ms; 13 slow UI-thread and 13 slow draw-command counters.
- The capture indicates this run missed frames. It does not identify app-owned hotspots, establish large-library performance, or prove an animation is the cause. Emulator GPU percentile output includes saturated buckets and should not be read as accurate GPU execution time.
- Idle memory snapshot after return: total PSS 125105KB, RSS 257676KB, one Activity, ten Views. A single snapshot cannot establish a leak.
- Artifacts: [frame summary](../../build/reports/deep-scan-2026-11-14/gfxinfo-settings-roundtrip.txt), [memory snapshot](../../build/reports/deep-scan-2026-11-14/meminfo.txt).
- Next useful measurement: same flow on a warmed profileable/release-equivalent build without host build contention, repeated runs; then a populated category scroll/collapse Perfetto trace. No CPU hotspot, recomposition count or leak-root claim is made. Simpleperf/Perfetto/heap analysis were not performed.

## Focused host reproductions

1. **URI encoding:** JetBrains JDK 21 JShell reconstructed `https://example.com/a%2Fb?q=a%26b` using the same decoded path/query URI constructor as UrlNormalizer. Output was `https://example.com/a/b?q=a&b`; an assertion confirmed it. The first JShell attempt failed because Android Studio's stripped JBR does not include JShell; the Gradle-provisioned JDK does. No application file was modified.
2. **Migration relationship loss:** Python sqlite3 3.50.4 created tables/indices from the checked-in v2 Room JSON schema in memory. With `PRAGMA foreign_keys=ON`, inserted one category, website, tag, and website-tag association. Extracted and executed all 19 SQL statements from MIGRATION_2_3 in a transaction. Before: one association. After: zero associations, one website. Assertions passed. This is a real SQL reproduction, not Android Room MigrationTestHelper coverage; Android confirmation remains required before implementing the repair.

## Limits and next work

No full feature matrix, populated performance fixture, restore against device data, installed-widget scenario, Android migration execution, real-device profiling, release shrinker build, dependency vulnerability database scan, broad OEM/API matrix, or production signing check was completed. Security findings are static trust-path findings, not executed attacks. Diagnostic artifacts are under ignored build/reports and may disappear on a future clean; copy them elsewhere if long-term retention is needed.

The detailed implementation handoff is [repair-prompt-2026-11-14.md](repair-prompt-2026-11-14.md). Application behavior is unchanged; findings remain unfixed.
