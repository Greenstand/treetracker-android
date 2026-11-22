# Clear the power-user flag on logout even when Settings hasn't loaded the power user yet (#1436)

## What was wrong

`SettingsViewModel` cleared the `power_user` flag on logout through `state.powerUser`, a copy it
loads in `init`. If logout was confirmed before that load returned, the copy was null, nothing was
written, and the next launch signed in as the previous power user.

## What changed

`SettingsViewmodel.kt`:

- Logout now reads the power user from the database when it runs and clears that user's flag,
  instead of using the cached copy.
- `LogoutConfirmed` navigates to Signup **after** the flag is cleared. Before, it navigated at
  once, and Signup reads the same flag when it opens.
- The read and write run in `NonCancellable`, so clearing Settings' ViewModel during logout can't
  drop the write.

One behaviour difference to be aware of: Signup now opens one database read and one write later
than before (milliseconds).

## Tests

Two new tests in `SettingsViewModelTest` keep the `getPowerUser()` query pending, send the logout,
then let the query return:

- `WHEN LogoutConfirmed before the power user has loaded THEN the power user flag is still cleared`
- `WHEN logout before the power user has loaded THEN the power user flag is still cleared`

**I have not run them**: this session had no Android SDK, so nothing was built or run, with or
without the fix. Without the fix both should fail at the `coVerify(exactly = 1) {
userRepo.setPowerUserStatus(powerUser.id, false) }` line (the old code makes no call when
`state.powerUser` is null), and the first one earlier, at the check that no navigation is sent
while the query is pending. That is from reading the code, not from a failing run; CI's unit-test
job is the first real run.

## 📸 Proof of change

### 🎥 Screen recording / video

No validating flow: the bug only happens when Logout is confirmed while a single Room query is
still running (a few milliseconds), and a tb flow can't time its taps that precisely.

What I considered:

1. A visible consequence exists (after logout, relaunch lands on the Dashboard instead of
   sign-up), so a flow could assert it. But reaching it means Settings → Logout → confirm, three
   screens and two taps after the ViewModel starts its query; the query has always returned by
   then, so the flow would pass before the fix too.
2. `killApp` / process death doesn't help: restoring Settings restarts the query, and the taps
   are still far slower than it.
3. Slowing the query down for the flow would mean adding a delay or hook to the app, which the
   validating-flow rules forbid.

I could not run a candidate flow on both apps to confirm point 1 (no emulator here).
`e2e/flows/settings-logout.yaml` already covers the normal logout path and should still pass; it
is a regression check, not proof of this fix.

Whether this can merge without a validating flow is the maintainers' call. The proof offered
instead is the two unit tests above.

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

### 🖼️ Screenshots

- [x] This change has **no user-visible / UI effect** on screen layout, so screenshots are not applicable.

Fixes #1436 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
