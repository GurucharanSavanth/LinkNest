# Ponytail Debt Ledger (2026-07-20)

Historical report. See the [current audit index](README.md) and [remaining work ledger](remaining-work-status.md) for resolved DNS, test, resource, and state claims.

Deferred shortcuts and known gaps left after the audit/fix pass. Each: what, why deferred, impact, trigger to address.

## Security residuals

- **DNS-rebinding TOCTOU (metadata/icon/health fetch).** requirePublicWebTarget checks
  the resolved IP once, but Jsoup/HttpURLConnection re-resolve on connect, so a host that
  flips its A/AAAA record between check and connect can still be reached. Impact: narrow
  (needs attacker-controlled low-TTL DNS + a reachable private service). Trigger: if a
  pinned-connection HTTP client is introduced, pin to the validated address. Owner: network.
- **No hermetic test for the resolved-IP path.** UrlSecurityPolicyTest uses literal hosts
  (rejected at the normalizeHost/IDN stage before the resolved stage), so
  ensureResolvedAddressesPublic / the AAAA-record case is verified by inspection + the
  independent reviewer's empirical check, not by a unit test. Trigger: add an injectable
  resolver seam to UrlSecurityPolicy if this path regresses. Owner: network.

## UX / cosmetic

- **AddEdit deleted-id message does not render.** The "That website no longer exists."
  message is set with saveCompleted=true; onDone() pops the screen before the snackbar
  shows, so the close is effectively silent. The core bug (blank Add form that would create
  a duplicate) IS fixed. Trigger: if users report confusion, surface the message on the
  destination (dashboard) via a nav result instead. Owner: addedit.

## Dead / redundant code left in place (low value, ripple risk)

- **UrlNormalizer.normalizeStrict + strict branch** — zero production callers, but 2 tests
  (UrlNormalizerTest L53/L66) assert it. Removing means editing those tests. Trigger: next
  core:network cleanup. Owner: network.
- **WebsiteDao.insertWebsites(List)** — zero callers; a trivial @Insert. Trigger: could back
  the row-by-row restore loop (OfflineFirstBackupRepository ~290-436) for a bulk-insert perf
  win. Owner: data.
- **BackupModels.encryptedBackupsEnabled / BackupArtifact.isEncrypted / BackupManager
  `encrypted` param** — always false since encryption was stripped. Kept to avoid rippling
  into the round-trip test + model. Trigger: next backup-format bump. Owner: data.
- **Legacy encrypted (.lnen) backups** — after the encryption strip, an old encrypted file
  now fails with a misleading "missing schemaVersion". App is 0.1.0/versionCode 1 (pre-
  release), so such files are unlikely. Trigger: if a real user hits it, add an
  encrypted-kind detection + clear "no longer supported" error. Owner: data.

## Resolved this pass (was deferred)

- **"Auto-save folder" (M6) — DONE.** Wired: BackupFileManager.writeToFolder creates a
  document in the granted SAF tree; onExportBackup auto-saves there when set, falling back to
  the manual picker on failure. Verified on emulator (API 37): file written to
  Download/LinkNestBackups with a valid envelope, no picker shown.

## Known-issue items not yet addressed (from audit register)

- Room schema DEFAULT drift (migrated vs fresh installs) — cosmetic, needs a table rebuild
  at the next schema version.
- icon_cache has no FK to website_entries → orphan icon rows/files on category delete.
- LOWER(name) lookups defeat indices on tags/categories/recent_queries (add COLLATE NOCASE).
- Process-death state loss in Search/Dashboard sheet targets (SavedStateHandle / saveable).
- ShareCaptureUseCases regex runs on full shared text on Main before the length cap (ANR risk).
- a11y: color swatches + filter-chip delete lack semantics / 48dp touch targets.
