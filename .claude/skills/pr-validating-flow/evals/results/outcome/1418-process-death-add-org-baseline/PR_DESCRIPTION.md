# fix(setup): restore capture setup after process death on Add org (#1418)

## Problem

On Add org (Track → pick user → Next), if Android kills the app in the background, reopening it
from recents crashes with `IllegalStateException: CaptureSetupScope not open`.

Navigation 3 restores the back stack with Add org on top, but the capture setup scope (selected
user, org name, destination wallet, session note) only lived in memory. Only user select opens it,
and user select is not shown on restore, so `AddOrgViewModel`'s init calls
`CaptureSetupScopeManager.getData()` with no scope open.

## Fix

Add org now comes back, and Next continues to capture.

- The setup data is saved with the activity's state, next to the back stack
  (`CaptureSetupSnapshot`: user id, destination wallet, session note, org name).
- `Root` reopens the scope from it before it shows the restored back stack, in the step where it
  already reloads the org. The user is read from the database again by id. Nothing happens when
  the scope is still open (rotation).
- Fallback: if a setup step that needs the user is on top of the restored stack and there is no
  user (nothing saved, or the user was deleted), `Host` takes those steps off the stack, so the app
  shows user select or the Dashboard, not a crash.

The other setup steps that read the same scope (wallet select, add wallet, session note) are
covered by the same change.

## Not in this PR

- Text typed in the org field but not submitted is not restored; the field shows the current org's
  name, as on a first visit.
- Process death on the capture screens: #1419, #1420.

## Tests

- `CaptureSetupScopeManagerTest`: snapshot and restore.
- `RestoredSetupStepsTest`: the back stack fallback.
- `RootUiTest`: Add org restored through `Root` with the scope closed between save and restore.
- New flow `e2e/flows/sad-process-death-add-org.yaml` (H5): the issue's steps, then Next opens
  capture and back returns to the Dashboard.

Not run locally: the session had no Android SDK or emulator, so the code was not compiled, and
the unit tests and the flow have only CI to run them.

## 📸 Proof of change

### 🎥 Screen recording / video

None attached: no emulator was available to record one. The new flow is a validating flow, so the
"Affected flows" check records it on the base app (expected to crash on reopen) and on this
branch's app, and posts both videos in its PR comment.

### 🖼️ Screenshots

- [ ] This change has **no user-visible / UI effect**

No screen changes; the visible effect is that Add org is shown after reopening, where the app
used to crash.

Fixes #1418

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
