# Copy-ready implementation prompt

You are working in `D:\MyDrive\Personal Builds\LinkNest`, a modular offline-first Android application. Implement the verified repairs and maintainability work below. Start by reading `AGENTS.md`, `docs/audit/deep-scan-2026-11-14.md`, and `docs/audit/deep-scan-validation-2026-11-14.md`. The audit baseline is commit `0f9ad3a1017a6a261a44af61f4fe54465938a566`; revalidate findings against the current working tree and preserve existing user changes. Do not assume historical audit claims remain true.

The deliverable is a working, tested improvement, not another plan. Use small changes at the responsible shared layer, preserve existing module boundaries, and finish each affected caller/test/fixture/document. No wholesale rewrite, architecture replacement, dependency upgrade sweep, arbitrary deletion target, or new framework is requested. Do not delete repository interfaces merely because they have one implementation. Explain any necessary new dependency. Do not create a server: the backend here is Room/DataStore/AppSearch, repositories, network helpers and WorkManager.

## Non-negotiable safety and compatibility

1. Dashboard must remain the startup destination. Never reintroduce a blocking health/integrity launch gate.
2. Preserve existing records, IDs, associations, ordering, custom fields, preferences, and supported backup compatibility. Do not use destructive migration fallback, `pm clear`, uninstall, emulator wipe, or production-data fixtures.
3. Backup/restore changes require round-trip and rollback tests before expanding behavior. Distinguish database atomicity from preferences/index/file operations outside its transaction. Never claim all-or-nothing behavior across stores unless implemented and tested.
4. Do not bypass TLS verification, network trust checks, size limits, cancellation, accessibility, or error handling to pass a check.
5. Public network validation must cover actual connection addresses and redirects, not only string checks or a separate DNS lookup. Avoid sending real user URLs to test servers.
6. Preserve file/content URI provenance: an imported URI string is not a persisted permission grant. Never read arbitrary local paths merely because a backup supplied them.
7. Use the Gradle wrapper, installed compatible JDK/SDK, and one Gradle workflow at a time. Local machine paths belong only in ignored local configuration or command environment.
8. Test fixtures must use isolated databases and disposable emulator records. Preserve original diagnostics before retrying. Do not hide warnings or weaken assertions.

## Phase 1 — Establish evidence

Record current commit, git status, modules, toolchain, package, variant and device/API. The app package is expected to be `com.linknest.app`, minSdk 26, target/compile SDK 36. Verify from current files/APK. Gradle daemon criteria currently select JetBrains JDK 21; source/bytecode compatibility is 17. Do not confuse these settings.

Run baseline tests, lint and debug assembly. Capture fresh logs and report counts per variant, including skipped tests and failures. Do not quote the old 13-test/101-warning baseline as current. Inspect existing emulator availability with pinned ADB serials. Confirm an installed APK matches the current source before drawing runtime conclusions.

Create a compact issue checklist with audit IDs 1–20 and states: reproduced, fixed, verified, deferred with reason, or disproved with evidence. Add a minimal failing regression for each nontrivial behavior before repair. Reproduce investigation-only items before treating them as verified defects.

## Phase 2 — Protect user data and trust boundaries

### A. Duplicate decisions (audit 2)

Trace AddEditViewModel → AddWebsitePipeline → PersistWebsiteAction → OfflineFirstWebsiteRepository and all duplicate-choice callers. Implement distinct semantics:

- CANCEL_SAVE performs no write, tag creation, file download or mapping update.
- SAVE_NEW preserves explicit duplicate creation behavior.
- MOVE_EXISTING changes category and destination ordering only; preserve note, tags, title, URLs, icon choice, pin, priority, follow-up, revisit date, labels, counts and timestamps except the intended updated time.
- MERGE_METADATA preserves user-authored data and uses an explicitly documented merge rule for fetched metadata and tags. Empty incoming values must not erase useful stored values.
- REPLACE_EXISTING applies replacement intentionally while retaining identity/history fields that the product promises to preserve.

Test a richly populated existing record with an incoming form containing defaults. Test every enum choice, missing/deleted duplicate target, category move, duplicate-match ambiguity and failed persistence. Keep multi-table mutation atomic. Do not apply one shared replacement request to all three update decisions.

### B. Restore preview, bounds, validation and recovery (audit 3, 4, 15)

Trace all three import entries: selected document, pasted text and staged backup. Parse/validate before database mutation. Show the current and incoming record counts, clear replacement wording and explicit confirm/cancel. Selecting a file must not replace records immediately.

Bound the input stream before building a full String; size metadata may be absent or false. Use consistent documented limits for serialized and decompressed data. Preserve the current checksum and bounded gzip implementation. Test limit boundaries, giant plain input, compressed expansion, invalid base64, invalid checksum, truncated JSON/gzip, unsupported versions and malformed optional sections.

Validate unique positive identities, references including icon-cache website IDs, tag-name conflicts, reasonable numeric fields, required strings, supported schema/format ranges, saved-filter specJson/enum values, and URI/network policy. A transaction must not silently drop conflicting tags while claiming the incoming count was imported. An invalid saved-filter payload must not commit successfully and then crash the search observer. Maintain compatibility fixtures for legacy JSON and format-v2 envelopes.

Treat device-local cache paths as regenerable. Preserve local backup-folder grants and do not import a grant from another installation. Make custom image portability explicit: copy validated assets through a supported format only if needed, otherwise report missing nonportable assets and fall back safely. Display restore warnings in Settings instead of discarding `ImportSummary.warnings`.

Provide and verify a recoverable pre-restore snapshot before replacing user records. Preserve it on failure. Test populated export/import across every table, preferences behavior, database reopen, injected mid-transaction failure, malformed references before writes, post-commit index/preference failure and rollback restoration. Never announce “original data preserved” after a committed database replacement unless that statement is true.

### C. Remote image and URL policy (audit 1, 6, 14)

Trace UrlNormalizer, UrlMetadataFetcher, UrlSecurityPolicy, IconStorageManager, every `preferredIconUrl`, AsyncImage use, and application ImageLoader configuration. Close the path that lets Coil fetch a URL rejected by IconStorageManager. Choose the smallest complete design: validated local-cache-only rendering with deterministic fallback, or one guarded image transport covering initial targets, redirects and connection DNS. Keep HTTPS verification intact. Do not allow an error fallback to trigger an unguarded second network request.

Add controlled network tests for private/loopback/link-local targets, IPv4/IPv6, public-to-private redirects, redirect loops, excessive hops, DNS rebinding/failure, oversized bodies, invalid image data and cancellation. Keep tests isolated; do not probe the user's LAN. Test imported remote/custom URI handling separately.

Preserve raw URI path/query/fragment encoding during normalization. Cases must include `%2F`, `%26`, `%3D`, `%25`, encoded Unicode, fragments, signed query strings, explicit ports, IDN and credentials. Decide unsafe scheme behavior consistently at the production normalization/validation entry. Current normalizer lenient mode accepts unsafe custom schemes while the opener rejects all non-HTTP(S); retain safe storage semantics without suggesting such links are operational. Do not add arbitrary external-intent launch support as an incidental fix.

Respect the selected IconSource: emoji should render the selected emoji, custom should use a valid granted asset, remote sources should prefer valid cached data, and failures must produce a local placeholder. OG_IMAGE must not skip the local cache. Ensure temp files are removed on oversize/read/write failures and cancellation; preserve committed files. Test no residual temp files after failures.

## Phase 3 — Make search and organization correct

### D. Deleted records and index recovery (audit 5)

Capture category website IDs before cascade deletion. Remove corresponding website documents after commit. Ensure search never returns deleted Room records when the index update fails or the process dies between stores. Reconcile rather than silently accepting permanent stale state. Keep the index derived and rebuildable.

Test delete-category/search immediately and after restart, delete-website, archive/unarchive, import, interrupted reindex, empty index, corrupt index and concurrent warm/rebuild calls. Investigate volatile-only session initialization; synchronize only where evidence requires it, without recursive lock deadlocks. Failures should be observable and recoverable rather than converted indistinguishably to empty successful results.

### E. Apply search constraints before limiting (audit 7, 12)

Add a fixture with more than 40 query matches where only later matches satisfy a category/tag/health filter. Ensure valid results appear. Apply constraints at the query layer or page eligible matches before enforcing the result limit. Keep AppSearch and Room fallback semantics consistent, including archived category visibility and literal query handling.

Remove per-website icon/tag/duplicate reads from whole-library filtering. Batch using existing DAOs or targeted queries. Record query count and latency for 100/1,000/5,000 links. Do not introduce paging or a new search engine without evidence that the narrower repair is insufficient.

Add delayed-query tests: rapidly type A then B; results for A must not publish under text B. Preserve cancellation and reset busy state in failure paths. Save only intentional recent queries according to a documented rule; avoid treating every intermediate keystroke as a useful saved search if tests demonstrate history pollution.

### F. Ordering, duplicates and archive recovery (audit 8–10)

Use canonical full ordering for moves while filtering, or disable reordering until the filter clears. Test hidden neighbors, first/last moves, cross-category moves, stable ties and reload. Keep multi-row reorders transactional.

Populate dashboard duplicate counts using one consistent identity rule shared with detail/search. Avoid N+1 queries. Test exact URLs, normalized variants, redirected destinations, same title/different domains, exclusion of self, and delete/update effects.

Expose a minimal archived-category recovery flow. Archive should provide understandable feedback and undo; restored categories must reappear in dashboard/search/selectors. Test all-categories-archived and no-categories states. Do not create duplicate Inbox categories to work around hidden data.

Rename “Last Imported” to an accurate current concept if it still sorts by creation/ID. Add true import provenance only if the product actually requires it, with migration and backup compatibility tests.

## Phase 4 — UI, lifecycle, accessibility and measured motion

### G. Durable editing and recovery (audit 11)

Persist minimal add/edit draft fields and search criteria with SavedStateHandle. Recompute derived lists and metadata. Handle deleted edit targets and removed categories gracefully. Test process recreation while editing, back-discard confirmation, picker return, search restoration and saved-filter selection. Avoid large Parcelable state or storing backup payloads in a Bundle.

Fix backup-folder picker cancellation so it leaves the previous selection intact. Report persistable permission failures instead of presenting an unusable folder as saved. Verify save-picker rotation/process-recreation handling; do not regress the existing rotation guard. Ensure concurrent import/export/health actions have sensible UI guards and cancellation outcomes.

### H. Lazy rendering and motion (audit 13)

Profile a focused flow with a large single category: cold dashboard load, expand/collapse, list/grid toggle, and scrolling. Flatten category children into keyed lazy items or rows if measured composition/layout work confirms the static risk. Preserve headers, pin/long-press behavior, focused deep links, drag/reorder semantics and scroll restoration.

Keep animation purposeful. Avoid animating the size of an entire large category if it produces expensive relayout; use the smallest local transition supported by the existing Compose version. Honor system animation scaling. Do not add decorative infinite animation or delay successful saves merely to show animation. Review the existing 900ms save-completion delay and remove/reduce it only while preserving visible success/error feedback and preventing duplicate submission.

Use bounded Perfetto/gfxinfo captures for an exact flow. Record build type, device/API, dataset, run count and artifacts. Compare before/after under the same conditions. Debug emulator frame numbers are diagnostic, not production performance guarantees. Use Simpleperf only if CPU attribution is needed; do not claim memory leaks from one meminfo snapshot.

### I. Accessibility and visual correctness (audit 18)

Give color swatches meaningful names, selected state and 48dp interaction containers. Keep the visible swatch small if desired. Make delete-filter controls independently accessible and large enough without ambiguous nested activation. Add clear empty/no-results/all-archived states and primary recovery actions. Fix the observed API 36 black status-bar icons/time over the always-dark app background by aligning system-bar icon appearance with the rendered theme. Verify system light and dark modes; adding a new app light theme is optional product scope, not required to repair status-bar contrast.

Move touched user-visible strings to resources. Reuse Material 3 and existing design-system components. Validate light/dark, font scale 1.0 and 2.0, narrow screen, landscape, RTL, keyboard insets, TalkBack traversal and reduced motion. Derive ADB taps from fresh UI trees and inspect screenshots after rendering. Do not invent contrast or clipping defects without viewing evidence.

## Phase 5 — Widgets, health, migrations, cleanup and warnings

Fix audit 17: HomeWidgetRefresher manually constructs all four providers and invokes onUpdate; their goAsync/finish lifecycle requires a real broadcast. Move reusable refresh work into an existing appropriate shared owner, call it from activity refresh, and keep pending-result ownership solely within receiver delivery. Finish each real result exactly once, including exceptions; do not merely suppress the crash while leaving receiver lifetime wrong. Test activity resume with each widget installed, first widget enable, actual update broadcasts, no-widget resume and refresh failure. Preserve widget IDs and PendingIntent destinations.

For HEAD 405/501, add a bounded GET fallback using the same security/cancellation constraints (audit 16). Test status mapping, timeout, DNS/TLS failure and redirects. Distinguish unsupported checks from dead links. Confirm metadata refresh callers/scheduling before changing fallback persistence; failed refreshes should not erase good metadata without an explicit reason.

Prioritize audit 19 with the data-protection work: the exported v2 schema plus actual migration SQL reproduced association loss on host SQLite 3.50.4 (one website retained, one tag association reduced to zero). Confirm under Android Room and fix the parent-table rebuild without losing child rows. Add Room migration tests for each exported supported origin and the complete chain to v6. Populate categories, websites, tag cross-references and mappings before upgrade. Reopen and verify all values/relationships. Do not label schema-default differences a crash until Room validation reproduces one. Preserve historical schema files and add a new version/migration only when the fix requires it.

Use the tree cleanup checklist from the audit (audit 20): inspect declaration-only AddWebsiteUseCase, WebsiteDao.insertWebsites, SmartSectionSheet, navigateToDashboard and unused Settings methods. Recheck generated DI, reflection strings, tests, Gradle and manifests before deletion. normalizeStrict has tests; decide whether to keep a coherent tested API or remove it with affected tests only after the production policy is settled. Delete orphan imports/dependencies only when proven unnecessary. Keep the action/use-case architecture for live flows.

Keep build outputs, `.gradle`, `.idea`, `.kotlin`, APKs and machine config ignored. Do not recursively delete them as a source cleanup substitute. Preserve assets and references unless relocation has a clear navigation benefit. Reconcile AGENT.md/CLAUDE.md with AGENTS.md and update README, backup-format documentation and dated audit status. State plainly that checksum plus gzip/base64 is not encryption. Do not add encryption merely to match obsolete documentation.

Fix current compiler/lint warnings by root cause. Separate actionable correctness/accessibility issues from version-update notices. Do not suppress warnings globally or update every library to reduce the count. Add CI using existing repository conventions if absent, covering the checks actually required to prevent the discovered regressions.

## Verification and completion

Run relevant focused tests after each change. Then run one final host workflow:

```powershell
.\gradlew.bat test lintDebug :app:assembleDebug --console=plain
```

Run populated migration/restore tests and functional instrumentation on the selected disposable emulator. Report actual test counts and failures, not just ADB exit codes. Exercise: launch without gate; add/edit/duplicate decisions; category/tag moves and reorder; search/filter; archive/restore; invalid and valid backups with cancellation/rollback; share/deep links; widgets/shortcuts; offline icons; health checks; process restart.

Build release to validate shrinker configuration if touched; do not invent release credentials or install a debug key over an incompatible signed installation. If a tool/environment block remains, record the exact command/error and remaining acceptance items. Do not mark unavailable checks as passed.

Deliver a concise final report linking code, tests, audit checklist and diagnostic artifacts. State behavior changes, checks/counts, unresolved failures, remaining risks and specific follow-ups. Include measured before/after performance only where captured. Finish with a clean, reviewable diff, not an unrelated tree reshuffle. Stop when the scoped repairs and required checks are complete.
