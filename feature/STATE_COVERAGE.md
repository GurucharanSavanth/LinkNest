# UI state and accessibility coverage

Current implementation scope: dashboard selection and query, category sheet drafts,
search filter-name draft, and serialized backup operations. This matrix defines
restoration behavior. Test existence is not evidence of execution; current run
results belong in `docs/audit/remaining-work-status.md` and its validation report.

## State and transition expectations

| State or transition | Expected behavior | Regression coverage | Remaining evidence |
| --- | --- | --- | --- |
| Add/edit URL, annotations, tags, priority and follow-up | Restore existing SavedStateHandle draft; never automatically save | Existing draft implementation and November 16 saved-task process-death observation | Repeat actual saved-task process death with representative paired fields |
| Create-category sheet and name/color/icon draft | Restore open sheet and small draft; cancellation creates no category | `StateRestorationFlowTest.categoryCreateDraft_survivesRecreation_andCancelDoesNotSave` | Real process death and keyboard/large-font pairs |
| Dashboard category actions/edit/reorder | Save target ID only, reload current record, restore small edit draft | `StateRestorationFlowTest.categoryEditDraft_survivesRecreation_withoutSaving`; actual saved-task process kill restored one unsaved category edit draft on API 36 | Stale/deleted/archive IDs, reorder and other process-death pairs |
| Dashboard website actions/reorder | Resolve saved target ID against current dashboard, including current category after a move | Source-level ID resolution | Website lifecycle test and concurrent move/delete coverage |
| Delete confirmation | Restore target identity and require a new explicit confirm click; never execute on restoration | No persistence from saved ID; existing explicit button callback | Recreation/process death at both delete dialogs |
| Target unavailable in current dashboard view | Dismiss target and explain that it is unavailable; never open a blank create form | Source-level reconciliation after loading | Deleted/archived and filtered-target instrumentation |
| Dashboard query | Restore small query through SavedStateHandle | Source-level saved query | Saved-task process death, list scroll position with restored query |
| Advanced search criteria and filter panel | Preserve existing small criteria draft and panel expansion | Existing SavedStateHandle/rememberSaveable implementation | All criteria pairs, actual process death |
| Saved-filter route and identity | Apply route selection once; preserve saved identity; detach changed/deleted filter while keeping current criteria | `SearchSelectionStateTest.routeSelection_appliesOnce_andRestoredSelection_reconcilesDeletedFilter` uses uniquely named owned filters and deletes only identity-checked fixtures | Actual saved-task process death and edited saved-filter revisions |
| Save-filter name dialog | Restore open dialog and name; require explicit Save | `rememberSaveable` for name and dialog visibility | Recreation test and rapid Save navigation |
| Backup read/export operation | Claim busy state before scheduling IO; ignore competing import/export/reset requests | `SettingsOperationTest.rapidImportRequests_openOnlyOneStream_andCannotResetRunningOperation` | Gated save callback and cancellation/restart pairs |
| Import preview and confirmation | Keep in-memory payload across recreation; process death dismisses payload and requires re-read/validation/confirmation | Existing ViewModel preview and explicit confirm transition | Recreation and real process death with file picker/provider |
| Paste-backup dialog | Dialog visibility survives; payload is intentionally not placed in saved state because it can be large | Source-level policy | User feedback after process loss and lifecycle coverage |
| Export Save-As picker | Saved filename guard prevents recreation re-launch; callback consumes ready package before IO | On API 36, optimized release preserved filename through rotation with SAF picker open, saved one verified document and did not reopen the picker | Process death while picker is open, duplicate callbacks, other providers |
| Import/folder picker | Save picker-open guard until activity result; ignore duplicate launches | `rememberSaveable` guards | Recreation/process death with picker and revoked grants |
| Pre-restore recovery | Stage independently from exports; explicit recovery preview uses same validation and confirmation | Backup-module fault and retention tests plus Settings integration | Latest run results and restart/partial-outcome matrix |
| Health check | Claim running state before launch so rapid taps cannot schedule concurrent checks | Source-level guard | Cancellation/failure and WorkManager overlap tests |

Activity recreation tests do not establish process death. The sheet recreation tests never
confirm a save, delete, or restore. The saved-filter test creates uniquely named
filters and deletes only identity-checked owned fixtures in `finally`. The gated-provider test returns invalid JSON,
which fails before preview, recovery staging, and database replacement.

## Accessibility and adaptive screen inventory

Historical checks remain in [the November 16 quality report](../docs/audit/quality-2026-11-16.md).
The current patch passed the main accessibility flow and two category-draft
recreation cases at 200% text with animations disabled (3/3), and in a
2992-by-1344 wide window (3/3). Settings were restored after each run.
The Arabic application-locale test asserts configuration and decor RTL; the
dashboard screenshot was visually inspected. These are bounded configurations,
not evidence for every screen or assistive service. Final run details and failed
test-harness attempts are in the validation report.

| Flow/state | Existing evidence | Remaining checks |
| --- | --- | --- |
| Empty/populated/loading/error dashboard; list/grid; collapse | Historical default/200%/wide main flow and named/selected actions | Error/loading variants, focus restoration, keyboard-only actions |
| Website/category action, edit, reorder, delete sheets | Category create/edit recreation tests pass at default/200%/wide; one actual saved-task category edit restoration passes | Every sheet, independent actions, dialog-root scan, stale ID feedback |
| Add/edit and duplicate decisions | Historical editor entry/back, create-category scan | Duplicate move/merge/cancel, IME, full TalkBack actions and focus |
| Search suggestions, chips, saved-filter dialog | Historical query/filter scan; small name draft now saveable. Distinct smart collection/quick flag names and semantic click/selection pass. Touch checked on optimized API 36 with IME dismissed. | Suggestions, saved filter deletion/revision, keyboard-visible and keyboard-only completion |
| Settings, backup preview/recovery, provider errors | Historical main settings scan | New recovery action, dialog traversal, cancellation/partial outcome announcements |
| Health and integrity | Historical default/200%/wide automated main flow | Error and long report states, non-color status, focus order |
| Share, widgets, shortcuts | Four minimum-size attached widget hosts inspected; native bounds pass at 100%/200% and 480/220 dpi. Optimized warm Recent shortcut selects the collection. | OEM launcher themes and accessibility traversal, cold/warm entry focus, complete share flow |

Across all rows: broader RTL and text expansion, TalkBack spoken output, Switch
Access, Voice Access, and complete hardware-keyboard flows require distinct
evidence. Minimum-API and reduced-animation checks cover only the exercised flows.
The editor label and Health Report title warnings were inspected at full height.
Disabled Clear was verified in the filter builder. Other ATF warning contexts and
spoken distinction between repeated search actions still need coverage.
No automated error-level pass establishes complete assistive-technology coverage.
