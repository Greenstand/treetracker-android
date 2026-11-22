# Task 103 - Logout before the power user loads leaves the power-user flag set

Follow-up found in the self-review of PR #1426 (bug #1415). The bug was already there; #1426 didn't cause it.

## Bug

`SettingsViewModel` loads the power user in `init` with `userRepo.getPowerUser()` and caches it in
`state.powerUser`. `SettingsAction.Logout` clears the flag only through that cached copy:

```kotlin
currentState.powerUser?.let { userRepo.setPowerUserStatus(it.id, false) }
```

If the user confirms logout before the load finishes, `state.powerUser` is still `null`, so nothing
is cleared. The `power_user` column stays `1`, and the splash screen (`getPowerUser()`) treats the
next person who opens the app as the previous power user.

## Test (lowest level that shows it)

`SettingsViewModelTest`:
`WHEN logout before the power user has loaded THEN the power-user flag is still cleared`.
The `getPowerUser()` mock suspends on a `CompletableDeferred`, so `LogoutConfirmed` runs while
`state.powerUser` is still `null`. The test then lets the load finish and asserts that
`setPowerUserStatus(id, false)` was called. A unit test is enough because the race is entirely in
the ViewModel. An e2e flow can't reliably tap Logout inside the few milliseconds the Room query takes.

## Fix (#1436)

In `Logout`, read the power user from the repository when logging out instead of using the cached
state.## Review follow-up: not fixed here

An independent review found a second way to lose the write. It was already on master, and it is not
the bug this task fixes. `LogoutConfirmed` launches the clear in `viewModelScope`, then navigates with
`popUpToRoot()`. That pops the Settings entry, and once its exit transition ends, the ViewModel is
cleared and `viewModelScope` is cancelled. On a slow database, the `getPowerUser()` /
`setPowerUserStatus()` calls can be cancelled and `power_user` stays `1`. A possible fix is to run them in
`withContext(NonCancellable)`, or an app-scoped write. It is left for a separate issue, as is process
death before the write commits.

There is no validating e2e flow for this task (`pr-validating-flow` skill, "When no flow can show the
change"). What was tried:

1. A consequence a user can see: yes. With the flag left set, a relaunch after logout opens the
   Dashboard as the previous power user (`SplashScreenViewModel.isInitialSetupRequired()` is
   `getPowerUser() == null`) instead of the language picker.
2. Reachable offline: yes, sign-up and logout need no server.
3. Hit reliably by a driven UI: no. Logout only clears the cached `state.powerUser`, so the bug needs
   Logout to be confirmed (two taps: Logout, then the dialog's OK) before the `getPowerUser()` Room
   query that starts when Settings opens returns, which takes milliseconds. Nothing reachable from
   Settings changes who the power user is, so the cache can't go stale any other way.
4. Candidate flow "Logout then relaunch opens setup" (sign up, Settings, Logout, OK, kill, relaunch,
   expect the language picker), run with the Flow runner on the app at 4edc726 (before) and at
   7580725 (after): passed on both, verdict "does not demonstrate the change"
   (https://github.com/Greenstand/treetracker-android/actions/runs/45073108272).

The proof is the unit test, which fails without the fix and passes with it.