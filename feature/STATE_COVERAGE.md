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
| Dashboard category actions/edit/reorder | Save target ID only, reload current record, restore small edit draft | `StateRestorationFlowTest.categoryEditDraft_survivesRecreation_withoutSaving` | Stale/deleted/archive IDs and saved-task process death |
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
| Export Save-As picker | Existing saved filename guard prevents recreation re-launch; callback consumes ready package before IO | Existing launcher guard, synchronous Building state on save | Process death while picker is open; provider failures |
| Import/folder picker | Save picker-open guard until activity result; ignore duplicate launches | `rememberSaveable` guards | Recreation/process death with picker and revoked grants |
| Pre-restore recovery | Stage independently from exports; explicit recovery preview uses same validation and confirmation | Backup-module fault and retention tests plus Settings integration | Latest run results and restart/partial-outcome matrix |
| Health check | Claim running state before launch so rapid taps cannot schedule concurrent checks | Source-level guard | Cancellation/failure and WorkManager overlap tests |

Activity recreation tests do not establish process death. The sheet recreation tests never
confirm a save, delete, or restore. The saved-filter test creates uniquely named
filters and deletes only identity-checked owned fixtures in `finally`. The gated-provider test returns invalid JSON,
which fails before preview, recovery staging, and database replacement.

## Accessibility and adaptive screen inventory

Historical default/200%/wide automated checks are recorded in
[the November 16 quality report](../docs/audit/quality-2026-11-16.md). They are not
fresh results for this patch. Retest changed sheets and backup controls with the
current build. Effective RTL requires a locale/root-direction assertion, not a
successful settings write.

| Flow/state | Existing evidence | Remaining checks |
| --- | --- | --- |
| Empty/populated/loading/error dashboard; list/grid; collapse | Historical default/200%/wide main flow and named/selected actions | Error/loading variants, focus restoration, keyboard-only actions |
| Website/category action, edit, reorder, delete sheets | Historical category action scan; new draft recreation tests prepared | Every sheet, independent actions, dialog-root scan, large font, stale ID feedback |
| Add/edit and duplicate decisions | Historical editor entry/back, create-category scan | Duplicate move/merge/cancel, IME, full TalkBack actions and focus |
| Search suggestions, chips, saved-filter dialog | Historical query/filter scan; small name draft now saveable | Suggestions, saved filter deletion/revision, disabled/error states, keyboard-only completion |
| Settings, backup preview/recovery, provider errors | Historical main settings scan | New recovery action, dialog traversal, cancellation/partial outcome announcements |
| Health and integrity | Historical default/200%/wide automated main flow | Error and long report states, non-color status, focus order |
| Share, widgets, shortcuts | Existing widget/package functional tests | Launcher-host contrast/sizing, accessible names/order, cold/warm entry focus |

Across all rows: actual RTL, text pseudolocale expansion, minimum API, reduced
animations, TalkBack spoken output, Switch Access, Voice Access, and complete
hardware-keyboard flows require distinct evidence. Clipped-content ATF contrast
warnings remain untriaged until controls are scrolled into view and inspected.
No automated error-level pass establishes complete assistive-technology coverage.
