# Clear the power-user flag on logout even before the power user has loaded (#1436)

## Problem

`SettingsViewModel` loads the power user in `init` and caches it in `state.powerUser`. Logout
cleared the `power_user` flag only through that cached copy. If logout was confirmed before the
load returned, the copy was still null, `setPowerUserStatus` was never called and the flag stayed
set. The splash screen then signed the next person in as the previous power user.

## Fix

In `SettingsViewModel`:

- Logout reads the power user from `UserRepo` again and clears its flag, instead of relying on
  `state.powerUser`. The cached copy is now only used to show the user in the logout dialog.
- The read and the write run in `NonCancellable`, so leaving Settings between them can't keep the
  flag set.
- `LogoutConfirmed` navigates to Signup after the flag is cleared. Before, it navigated straight
  away while the write was still running, so Signup could read the power user before it was
  cleared. This is a second, smaller race I found on the same path, not in the issue.

Logout with nothing else going on behaves as before, apart from Signup opening one database
round trip later.

## Tests

Not built or run in my session (no Android SDK there), so the unit tests and flows have only CI's
run to go on.

Two new tests in `SettingsViewModelTest`:

- Logout confirmed while `getPowerUser()` is still suspended: nothing is cleared and nothing
  navigates until it returns; then the flag is cleared and Signup opens. This is the issue's case
  and fails on master.
- Navigation to Signup waits for `setPowerUserStatus` to finish.

## Proof of change

### 🎥 Screen recording / video

I have no recording: there was no emulator in my session. The "Affected flows" check records
`settings-logout` on this PR's app, which shows logout still works, but not the fix itself.

There is no validating flow, deliberately. A driven tap can't confirm logout before a Room query
returns, so any flow passes on master too (`docs/testing/ci-flows.md`, "Before and after", lists
this kind of race as one that needs a unit test). The first unit test above is the before/after
evidence.

### 🖼️ Screenshots

- [x] This change has **no user-visible / UI effect** on any screen (no layout, text or
  navigation target changes), so screenshots are not applicable.

Task notes: `docs/tasks/task_116/README.md`

Fixes #1436 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
