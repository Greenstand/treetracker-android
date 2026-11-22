# Reset the current user and capture setup user when that user is deleted

Fixes #1448.

## Problem

In Settings → Delete account, picking a user sends `UserSelectAction.SelectUser`, which makes them
the current user (`Preferences.setUserId`) and the capture setup user
(`CaptureSetupScopeManager.getData().user`). `DeleteUser` then removed the user's row but reset
neither, so both kept pointing at a user who no longer exists.

## Change

- `UserSelectViewModel.deleteUser`: after a successful delete, the current user id and the capture
  setup user are set to null, each only if it is the deleted user. The capture setup user is only
  touched while the setup scope is open.
- `Preferences.userId`: new read-only property for the current user id, used for that check.
- `state.selectedUser` is deliberately kept: the "Account deleted" dialog shows it and uses its
  `isPowerUser` to choose between Signup and Settings.
- Docs: `docs/tasks/task_116`, one line in `preferences-and-prefkeys.md`.

## Testing

- New unit tests in `UserSelectViewModelTest`:
  - a successful delete resets both values and keeps `selectedUser`
  - a failed delete resets nothing
  - deleting a user who is not the current one leaves the current user alone
  - with the setup scope closed, only the current user id is reset
- **Not run locally.** The session that wrote this had no Android SDK, so neither the build nor
  the unit tests ran before CI.

## No validating flow

There is no `validates:` line and no new e2e flow, because the fix changes nothing on screen: no
`UserPrefKey` is in use in the app today, and capture setup always passes through user select,
which sets the user again. This is the "stale state behind the UI" case that
`docs/testing/ci-flows.md` says a flow cannot show, so the unit tests are the evidence. The
existing `settings-delete-user`, `settings-delete-user-offline` and `settings-delete-admin` flows
cover the delete path for regressions.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
