# fix(setup): restore session note, wallet select and add wallet after process death instead of crashing

Fixes #1447 🦕

## What was wrong

In an org whose capture setup has a session note or wallet select step, reopening the app from
recents after Android killed it on session note, wallet select or add wallet crashed with
`IllegalStateException: CaptureSetupScope not open. Call open() before getData().` Navigation 3
restores the screen, but `CaptureSetupScopeManager` keeps the capture setup scope only in memory,
and the three view models read it in `init`.

## What changed

- New `CaptureSetupRestorer`, injected into `SessionNoteViewModel`, `WalletSelectViewModel` and
  `AddWalletViewModel`. With an open scope it saves the setup user's id to preferences. When the
  view model is created with the scope closed, it reopens the scope and reloads that user.
- While the user is being reloaded (`restoring`), the screen holds Next, Back and its other ways
  out. If the user can't be found (`setupLost`), the scope is closed again and the screen goes to
  the Dashboard (`SetupLostEffect`). This is the same behaviour Add org got in #1431.
- The setup step index needs no restoring; `navForward` already re-derives it from the back stack.

Not restored: text typed or a wallet picked before the process died. The screen comes back empty
and the user enters it again. `AddOrgViewModel` keeps its own restore code from #1431; I left
moving it onto the new class out of this fix. Details are in `docs/tasks/task_111/README.md`.

## Not run locally

This was written in a sandbox with no Android SDK and no emulator. **Nothing was compiled, and no
unit test, Robolectric test or flow was run.** CI on this branch is the first run of all of it,
including the before/after verdict below, which I have not seen.

## 📸 Proof of change

### 🎥 Screen recording / video

validates: org-process-death-session-note org-process-death-wallet-select org-process-death-add-wallet

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

Three new flows, one per screen in the issue, each on a seeded org (`e2e/run-org.sh` has their
lines). Each goes to its screen, presses Home, kills the app, reopens it, waits for the same
screen and finishes setup into capture.

- Expected before (merge base): the reopened app crashes, so the flow fails at the first wait
  after the relaunch (`SessionNoteField`, `OrangeAddButton` or `WalletNameField` is not visible).
- Expected after: the screen is back, and a note / the user's own wallet / a new wallet name plus
  Next opens capture.

These are expectations until CI has written the verdict and videos between the markers. If a
"before" run fails at any other step, that flow is not proof and needs fixing.

### 🖼️ Screenshots

| Before | After |
| ------ | ----- |
| App closes on reopening (crash) | The same setup screen as before the kill |

No layout changed; the videos cover it.

## Tests

- New: `CaptureSetupRestorerTest`, `SetupProcessDeathUiTest` (Robolectric, all three screens).
- Added process-death cases to `SessionNoteViewModelTest`, `WalletSelectViewModelTest` and
  `AddWalletViewModelTest`.
- Existing tests of the three view models and screens: only their set-up changed (the new
  constructor argument, and `CaptureSetupScopeManager.isOpen` stubbed to `true`, since they mock
  the scope manager). No assertion was edited.

## Checklist

- [x] GitHub issue: #1447
- [x] Tests added
- [ ] Linter (`./codeAnalysis`): not run, no SDK here
- [x] Docs updated (`docs/tasks/task_111`, `docs/testing/e2e-flow-test-plan.md`)

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
