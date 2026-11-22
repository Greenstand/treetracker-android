# fix(setup): restore session note, wallet select and add wallet after process death instead of crashing

Fixes #1447.

## Problem

In an org whose capture setup flow has a session note or wallet select step, killing the app in the background on session note, wallet select or add wallet and reopening it from recents crashed with `CaptureSetupScope not open. Call open() before getData()`. Navigation 3 restores the screen, but the capture setup scope only lives in memory and the three ViewModels read it in `init`.

## Fix

- `CaptureSetupRestorer` (new) keeps a copy of the setup data in preferences: the user's id, session note, destination wallet and organization name. The three ViewModels save it once the scope has its user and each time their step changes the data.
- When one of the three ViewModels is created with the scope closed, it reopens the scope from that copy and reloads the user from the database. `Preferences.setUserId` is set again too, as user select does.
- While the user is reloading, the screen ignores Next, Back and its other ways out, so setup can't continue without a user.
- If the saved user no longer exists, the screen goes back to the Dashboard (`LeaveLostSetupEffect`), as Add org does.

What the user sees after reopening:

| Screen | Restored state |
|---|---|
| Session note | The note typed before is back; Next opens capture. |
| Wallet select | The wallet list is back with nothing selected; pick one, then Next opens capture. |
| Add wallet | The name field is empty; type a name, then Next opens capture. |

## Not verified locally

This sandbox has no Android SDK or emulator. Nothing was compiled, no test was run and the flows were not run on a device. CI is the first build of this branch.

## Tests

- `CaptureSetupRestorerTest`: reopen, reload and save.
- `SessionNoteViewModelTest`, `WalletSelectViewModelTest`, `AddWalletViewModelTest`: new restore cases. Existing cases pass a relaxed mock restorer.
- `SetupStepRestoreUiTest`: Next and Back wait while restoring, and a lost setup ends on the Dashboard, on each screen.
- Three new e2e flows, added to `e2e/run-org.sh`. They kill the app on the step, reopen it and continue to capture, so they should fail on master's app and pass on this branch's.

validates: org-process-death-session-note org-process-death-wallet-select org-process-death-add-wallet

## For the reviewer

- **Add org is unchanged.** It keeps its own restore from #1418, which brings back the user only. A session note or wallet from a step before Add org is therefore still lost if the process dies on Add org. Moving it onto `CaptureSetupRestorer` would fix that; I left it out to keep this PR to the three crashing screens.
- **The session note is now written to preferences** on every keystroke (`apply()`, asynchronous) and is not deleted when setup ends. It is only read back when a setup step is created with the scope closed.
- `org-process-death-session-note.yaml` asserts the restored note with `assertVisible: {id: SessionNoteField, text: ...}`, the pattern `capture-note.yaml` uses for `TreeNoteField`. If that step fails on CI, check the assertion before the fix.

Details are in `docs/tasks/task_111/README.md`.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
