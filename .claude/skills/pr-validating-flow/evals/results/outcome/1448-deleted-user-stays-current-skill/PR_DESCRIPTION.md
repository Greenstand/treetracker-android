# Clear the current user when that user is deleted (#1448)

- [x] Make sure to open a GitHub issue as a bug/feature request before writing your code!
- [x] Ensure the tests are added/updated (if necessary)
- [ ] Ensure the linter passes — **not run**: this session has no Android SDK, so nothing was compiled, linted or run locally. CI is the first build of this branch.
- [x] Appropriate docs were updated (`docs/tasks/task_116/README.md`)

## What was wrong

Settings → Delete account picks a user with `UserSelectAction.SelectUser`, which makes them the
current user (`prefs.setUserId(user.id)` and `CaptureSetupScopeManager.getData().user`).
`DeleteUser` removed the row but reset neither, so both kept pointing at a deleted user.

## The change

- `UserSelectViewModel.deleteUser`: after a successful delete, the current user id and the capture
  setup user are set to null, each only if it still is the deleted user. A failed delete changes
  nothing. `selectedUser` in the screen state is kept, because the "Account Deleted" dialog shows
  it and uses its `isPowerUser` to choose between Signup and Settings.
- `Preferences.userId`: a getter for the current user id, needed for that comparison.

## 📸 Proof of change

### 🎥 Screen recording / video

**No validating flow: nothing on screen reads either stale value, so a flow cannot fail on the old
app because of this bug.** There is no video in this PR. Whether it can merge without one is the
maintainers' call; I'm asking for that here rather than attaching a recording that proves nothing.

What I tried (by reading the code; there is no emulator in this session):

1. **A later screen that reads the current user id.** It only affects `UserPrefKey` paths, and the
   app has no `UserPrefKey` (`asUserPref()` / `UserPrefKey(` are unused in `app/src/main`).
2. **A later screen that reads `CaptureSetupData.user`.** It is read only when capture setup
   finishes (`SessionTracker.startSession`, `CaptureSetupNavigationController.advance`). Every
   path there goes through capture user select or signup, which set the user again, and Dashboard
   → Track opens a fresh setup scope.
3. **Relaunch / process death.** Both values are in memory only, so they are gone either way.
4. The existing `settings-delete-user.yaml` covers this path and should pass before and after; it
   shows nothing broke, not that the bug is fixed.

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

**Proof instead: unit tests** in `UserSelectViewModelTest`:

- `GIVEN the selected user is the current user WHEN delete succeeds THEN the current user id and the capture setup user are cleared`
- `GIVEN the selected user is the current user WHEN delete fails THEN the current user is kept`
- `GIVEN another user is the current user WHEN delete succeeds THEN the current user is kept`
- `GIVEN the capture setup scope is closed WHEN delete succeeds THEN only the current user id is cleared`

The first and last are the ones that need the fix: without the `clearCurrentUser` call they should
fail on `verify { preferences.setUserId(null) }`. **I have not run them, with or without the fix**
(no SDK here), so there is no failing/passing output to show yet; the PR's unit-test check is the
first run.

### 🖼️ Screenshots

- [x] This change has **no user-visible / UI effect**, so screenshots are not applicable.

## Not changed, worth a look

A capture user select entry that stays alive after the capture screen's profile icon keeps its own
`selectedUser`. If that user is later deleted in Settings, its Next arrow stays enabled and
tapping it fails in `SessionTracker.startSession` (before this PR with "Could not find user of
id", after it on `user!!`). It is a stale selection in a different ViewModel, traced in code only,
and left for its own issue.

Fixes #1448 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
