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
state.
