# Remaining-work validation through November 21, 2026

This is a bounded repair and verification pass from `1c0768c`, not a production
readiness certificate. [The issue ledger](remaining-work-status.md) is the current
R01–R10 status; [the review map](remaining-regression-scope.md) records inspected
boundaries and unexecuted cases. The original 20 findings have implemented fixes.
Private/raw evidence is local and ignored under
`build/reports/remaining-2026-11-16/`; the directory name reflects the start date.
Nothing was pushed, published, or uploaded.

## Behavior and compatibility

- Recovery snapshots have separate ownership from ordinary exports, two bounded
  published generations, integrity verification, interrupted-operation metadata,
  conservative reconciliation, and corrupt-newest fallback. Unknown outcomes stay
  protected; a new export cannot erase the pending recovery copy.
- Version-2 JSON accepts an additive bounded image-assets section. App-owned PNG,
  JPEG and WebP images survive export/import without the source path or grant.
  Foreign URI strings remain untrusted and are omitted with a preview count.
  [Format, budgets and commit-boundary policy](../backup-format.md).
- Import failures before Room commit preserve the library. Postcommit preference
  or index failures report that replacement occurred; cancellation is propagated.
  Output success follows stream close, including close-time failures.
- AppSearch mutations/rebuilds share an owner. Partial batches invalidate readiness;
  bounded background reconciliation coalesces retries. Busy/cold reads allow Room
  fallback promptly. Rebuild icon reads are bulk, and source transactions end
  before AppSearch calls. Deleted/archived Room IDs remain authoritative.
- Metadata refresh preserves personal titles and custom/emoji selection, and
  skips a row changed while the response was in flight. Normal capture now applies
  the blocked-scheme rule already used by strict normalization and opening/fetching.
- Category drafts and sheet IDs, saved-filter identity, picker/operation guards,
  and warm deep links preserve intended state without replaying destructive actions.
  Shortcut activation reporting consumes a matching stable ID once.
- Sheet/settings/shortcut text moved to owned resources; category/website counts
  use plurals. Persisted enum/JSON/route identifiers are unchanged. Localization
  readiness remains incomplete; no translations were invented.

Current configuration remains Gradle 8.13, AGP 8.13.2, Kotlin 2.2.10,
KSP 2.2.10-2.0.2, SDK 36/36/minimum 26, version 0.1.0/code 1 and package
`com.linknest.app`. The configured daemon JDK is 21; installed Studio's launcher
JBR is 25.0.3; bytecode target is 17. Dependencies were not migrated as part of
storage repairs. [Every retained lint notice has a disposition and follow-up](lint-2026-11-17.md).

## Commands and executed checks

Root alone owned Gradle/device state. Current task names were discovered with
`projects` and `tasks`; no ktlint/detekt workflow was invented. Final source gates:

```powershell
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat test lintDebug lintRelease :app:assembleDebug `
  :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease `
  :app:assembleOptimizedQa '-Pksp.incremental=false' --max-workers=2 --console=plain
```

Instrumentation used explicit serials and `am instrument -w -r` with
`androidx.test.runner.AndroidJUnitRunner` for `com.linknest.app.test`,
`com.linknest.core.database.test`, and `com.linknest.core.data.test`.
Counts are taken from runner statuses, including assumptions; `OK (N tests)` alone
does not distinguish execution from discovery. Debug/release JVM variants are
duplicate executions, not independent cases. Fixture setup and cross-install
transfer are reported separately from ordinary suite coverage.

The November 18 repairs and resource fixes passed. The November 21 follow-up
passed JVM, debug/release lint, debug app/data test APKs, optimized QA, release APK
and AAB (`integration-2026-11-21.log`). The revised search test compiled in
`search-semantics-build.log`. Packaging and lint passed again from committed
application sources (`packaging-2026-11-21.log`, source `e85d480`).

| Check | Discovered | Executed / passed | Skipped | Failed | Evidence |
|---|---:|---:|---:|---:|---|
| JVM, debug/release plus app optimized-QA duplicate | 45 distinct cases | 91 executions / 91 | 0 | 0 | `**/build/test-results/test*UnitTest/TEST-*.xml` |
| API 36 data | 36 | 35 / 35 | 1 gated portable-transfer control | 0 | `data-emulator-5554-2026-11-21.txt` |
| API 26 data | 35 | 31 / 31 | 3 provider SDK gates + 1 transfer control | 0 | `data-emulator-5556-2026-11-21.txt` |
| API 36 database | 6 | 6 / 6 | 0 | 0 | `database-first.txt` |
| API 26 database | 6 | 6 / 6 | 0 | 0 | `api26-migrations-final.txt` |
| API 36 app | 16 | 14 / 14 | 2 explicit fixture controls | 0 | `app-emulator-5554-2026-11-21-final.txt` |
| API 26 app | 11 | 9 / 9 | 2 explicit fixture controls | 0 | `app-emulator-5556-2026-11-21.txt` |
| API 36 app, final resource changes | 3 | 3 / 3 | 0 | 0 | `resource-final.txt` |
| API 36 canonical image reference, final test APK | 1 | 1 / 1 | 0 | 0 | `media-identity-final.txt` |

The two native widget test methods check 40 size/font/density/content
configurations, including fractional 220dpi and 200% text; these are two JUnit
cases, not 40 independent tests. `widget-final.png` shows all four minimum-size
attached host types against a light-gray parent, inspected after the clipping fix.
Five app tests and the AtomicFile `.new` publication fault case are excluded
from API 26 discovery by SDK filters. The two gated
portable-transfer tests passed separately (`portable-source.txt` and
`portable-destination.txt`). The profiling fixture create/cleanup controls are
also separate. `test-summary-2026-11-21.json` contains the parsed final runner counts.

The API 36 emulator is Pixel_10_Pro_XL, x86_64, 1344×2992, density 480, 60 Hz.
The separate `LinkNest_QA_API26` AVD uses the installed API 26 Google APIs x86_64
image, 1080×1920. Both are emulators. No physical-device evidence is claimed.
The API 26 AVD uses task-owned ignored storage; no existing emulator was wiped.

## Runtime and state evidence

- Default, 200% text with animations disabled, and 2992×1344 wide main accessibility
  flow plus two draft-recreation cases passed (3/3 in each enlarged configuration).
  Font/display/animation settings were restored in `finally` blocks.
- Arabic application-locale configuration and decor-root RTL were asserted, and
  the mirrored dashboard screenshot inspected. This does not prove every screen's
  RTL behavior. Pseudolocale rendering and widget bounds have dedicated tests.
- ATF flag `[144,2919][590,2920]` matches a one-pixel slice of the editor's
  “Why this was saved” label. After scrolling it measured 72 pixels high, with
  `#B9C7E5` text on sampled dark backgrounds of `#131F38` to `#15203A`
  (approximately 9.5–9.7:1). Health Report flag `[96,2871][330,2872]` likewise
  matched a one-pixel title slice and was inspected scrolled to full height.
  Filter Builder's 3.35:1 estimate matches a genuinely disabled Clear button
  when no filters are active. The repeated search labels are distinct smart
  collection and quick-flag actions. Smart collection names now include their collection context. The API 36
  regression verifies separate names, accessibility click actions and selection
  behavior for Pinned collection and the Pinned quick flag. Keyboard/IME surface
  estimates and spoken traversal still need separate verification. Actual touch
  on the final optimized artifact selected each control after dismissing the IME
  (`final-21-search-*.xml`). Trees, screenshots and
  warning output are saved as `contrast-*.xml`, `contrast-*-full.png`, and
  `accessibility-warnings-final.txt`. No warning was suppressed.
- One real saved-task category edit survived process death: the script waited for
  LinkNest's own STOPPED record with `mHaveState=true`, killed its PID, confirmed
  process termination, resumed the saved task, and verified the unsaved draft with
  a new PID (`10144` → `10505`). Screenshot `process-final.png` and
  `process-death-final.json` preserve evidence. The draft was discarded afterward.
  Recreation tests alone are not process-death evidence.
- Optimized runtime exercised populated cold launch, list/grid, warm search and
  dashboard/settings routes, back navigation, real SAF export, import preview
  cancellation, explicit replacement, custom-image rendering/export, and explicit
  recovery of the prior 300-link fixture. All replacement records were this task's
  identity-checked `profiling.invalid` data on an initially empty installation.
- The November 21 follow-up used the November 18 locally signed optimized copy
  (`816f60336baf0c220eabf4f369884e45f059c87cf39d35903806fe669a68422e`) on API 36.
  `CheckWebsiteHealthWorker` returned `SUCCESS` through WorkManager on the empty
  QA library. The scheduled job showed connected-network and battery-not-low
  constraints. Retry/backoff and active network probing remain separate tests.
- On that same optimized copy, the editor created one unique custom-scheme
  fixture with a note, high priority and Needs review state. Cancel left the
  original in Inbox. Move put the only record in the owned category. Merge from
  a conflicting incoming draft kept the same category and the existing personal
  fields. The app was stopped and reopened. A later read-only Room copy confirmed
  one record and the persisted `HIGH` and `REVIEW` values (`duplicate-room/verified.json`).
  The first diagnostic assertion expected the wrong enum spelling and was corrected
  against the stored schema and selected UI label. The owned category, its link
  and the exact learned Inbox suggestion were removed after verification.
- The final optimized release also completed an empty-library SAF export after
  rotating with the Save-As picker open. The expected filename survived, one
  document was saved, and the app returned to Settings without relaunching the
  picker. The exported JSON checksum was verified and the owned document removed.
  Rotation settings returned to their original values. See `final-21-picker-*.xml`
  and `final-21-rotation-export.json`.
- `BackupBoundaryTest` covers the platform's log-only rename failure and a write
  blocked before AtomicFile publication. The latter preserves an existing staged
  export. Both run under API 36. Valid image bytes plus a blocked owned image
  directory caused a precommit restore failure on API 26 and 36. Room reopened
  with the original library and image bytes intact.
- Source image export and destination import ran in separate **app and data-test
  package installations**, after deleting the original source image. Destination
  database reopen retained the generated private image bytes. These gated transfer
  fixtures do not execute in a normal suite.
- A real optimized export of the restored image-bearing library contained the
  exact portable asset, no raw custom URI, and checksum
  `cf84dabd01e6726007d03b3b1fe8025bdc2721f0df85b96be52bc1a33cf1e2de`.
  An ordinary export left the 300-link recovery discoverable, which was previewed
  and explicitly restored. See `release-media.png`, `export-media-check.log`,
  `recovery-preview.xml` and `recovery-result.xml`.

See [the state matrix](../../feature/STATE_COVERAGE.md) for remaining pairwise,
stale-ID, provider, and assistive-technology gaps.

## Optimized artifacts and R8

Release shrinking/resource shrinking remain enabled. Application model keeps
were narrowed to persisted enums after inspecting explicit JSON serialization;
an unused model-package rule and repository-interface keep were removed.
Existing broad library keeps remain conservatively retained, alongside library
consumer rules. This is incremental narrowing, not proof every remaining rule is
necessary. No blanket keep, new broad `dontwarn`, or ignored R8 missing-class
diagnostic was added. Configuration, usage, mapping, merged manifest, package
metadata and resource outputs remain under `app/build/outputs/` and intermediates.

`optimizedQa` inherits release, enables shell profiling, and signs only that QA
variant with the local Android Debug key. It remains non-debuggable, uses the same
application ID/version, and excludes debug-only fixtures. A separately signed
copy of the unsigned optimized release has no profileable change. Neither is a
production-signed release. No production signing material was found or replaced.
Production signing/update compatibility is blocked by **production signing
credentials and the intended existing production certificate**.

The local QA signer SHA-256 is
`4fc1a0e0f1197b6202179d1e12c5ef050073bf2567d77e55b6b5adfb11976331`.
The five files are in `build/deliverables/`. `artifacts.json` records their
full SHA-256 hashes and exact paths. Install a signed APK on an isolated emulator
or an installation with a matching certificate using
`adb -s SERIAL install -r build/deliverables/LinkNest-optimized-local.apk`.
Unsigned release APK and AAB require the normal distribution signing path.

| File in `build/deliverables/` | Build and signing | SHA-256 |
|---|---|---|
| `LinkNest-debug.apk` | Debug, unminified, Android Debug signer | `8287f4b5ff473bd13a9bfe9e7a26c1292941c69c9c1c8c764ddcc406409a0aa0` |
| `LinkNest-optimized-local.apk` | Release APK copy, minified, Android Debug signer | `de97e037a434d0de9e79dd495768fd3c0f82db7998ea6f840bd95583d3e94e88` |
| `LinkNest-optimized-profileable-qa.apk` | Release-derived, minified, shell-profileable, Android Debug signer | `fad85f40df1f731ac7bbcb89a31c23c7df034f64d6ddef923604f263c78c5d27` |
| `LinkNest-release-unsigned.apk` | Release APK, minified, unsigned | `8dddb55c21502d2ea7566c088f6b490f243c13e38e7d11495e94bb7f93ea1cac` |
| `LinkNest-release-unsigned.aab` | Release bundle, minified, unsigned | `c9816ad05601feadf9c30e16c186c323465c9a45840f128fa4f303c759a7e439` |

November 18 installed package bytes matched its profileable QA APK on API 36
and its locally signed optimized release on API 26. API 36 showed the populated
dashboard, selected Recent from a warm shortcut intent and returned to the
dashboard. API 26 showed the initial Inbox dashboard. No fatal exception was
logged by either current process during those smokes. The longer SAF/recovery smoke used
the earlier optimized copy preserved as `runtime-optimized-local.apk`
(`3a6132a15d4dd807c8c19871a5786ebbd9e8cafd5711a10011ad5ad077cdffe7`).
`retrace-2026-11-21.txt` resolves a synthetic frame to `DashboardViewModel.kt:67`
using the matching release mapping. `final-release-dependencies.log` records the
resolved graph, and `aab-signature-2026-11-21.txt` verifies unsigned bundle status. The matching
release and optimized QA mappings are retained in `build/deliverables/`.

November 21 cold smokes used the final locally signed optimized copy on API 36
and final profileable QA APK on API 26. Installed bytes matched the delivery
hashes. Each showed `Inbox` and `0 websites`. Process-filtered crash logs and
rendered screen evidence accompany `installed-artifacts-2026-11-21.json`.

## Performance

[Six warmed optimized captures and a sampled CPU profile](performance-2026-11-17.md)
show persistent emulator jank: list p95 69–73 ms, grid 65–69 ms; frame deadline
miss rates 51.82–57.21%. The predefined p95≤33 ms / misses<25% investigation target
was **not met**. Rendering/buffer waits are implicated in this emulator; the sample
does not justify an app-owned Kotlin hotspot claim or a speculative Compose
rewrite. There is no controlled dashboard before/after speedup claim.

The capture APK SHA-256 is
`BB51E47EAECCB96767BD05F249650EBBCFD3F279D5E89271801FDC080107431D`.
Later widget/resource/test repairs do not change that historical capture's build
identity. The captured APK and raw evidence are preserved separately from final
deliverables. No Gradle or second emulator ran during measured captures.

The index bulk-read regression uses one icon query at 100/1,000/5,000 links;
single isolated debug observations were 1008/1491/5899 ms. They are not optimized
budgets or before/after speedups. No leak claim is made from PSS deltas.

## Failed attempts and limits

Failed attempts remain preserved, not silently counted as passes: initial wrapper
lock access, PowerShell splitting the unquoted KSP property, temporary SDK property
escaping, one indentation lint error, a fault fixture that missed its intended
publication boundary, and three debug test classpath/entry-point compilation
errors. These were corrected without deleting assertions or suppressing checks.

Later harness failures were native find-by-text on Compose virtual nodes, an
off-screen lazy editor field on API 26, and an invalid Compose-test import. Tests
now inspect exposed accessibility children and scroll the actual lazy container.
The initial process-death attempt matched an unrelated stopped record and killed
LinkNest before saved state was ready; it was inconclusive, then repeated with
LinkNest-specific state checks. Perfetto config/setup failures preceded the six
valid measured captures and are not counted as runs. Widget inspection exposed
actual clipping and density-rounding defects, which received focused bounds tests.
The first November 21 search regression tried a touch while the IME was open and
the quick flag did not select. The test now executes the exposed semantic click
and checks the resulting state. The original failing suite log is retained. The
local PowerShell policy rejected launching a script file, so the same packaging
commands ran inline without changing policy.

The QA fixture category, website, learned domain mapping, owned SAF files,
generated staged backups, and recovery snapshots were removed after identity
and checksum checks. `final-cleanup-21/verified.json` records one empty Inbox and
zero websites, tags, mappings, recent queries, saved filters, cache entries and
integrity events. Original device settings and machine-local configuration were
restored. Local evidence stays under ignored build directories.

Remaining work includes production signing; the unmet scrolling target; physical
profiling outside the user's emulator-only scope; complete TalkBack spoken output,
Switch Access, Voice Access and keyboard completion; unclassified ATF warnings
outside the inspected cases; broader RTL/state/process-death pairs; arbitrary filesystem/provider and
phase-kill recovery cases; unknown journal outcome resolution; remaining library
keep narrowing, coordinated toolchain/API upgrade, and full resource extraction.
These are visible gaps, not newly confirmed defects or a promise of zero defects.
