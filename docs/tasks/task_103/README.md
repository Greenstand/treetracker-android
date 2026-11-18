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
the ViewModel. An e2e flow can't reliably tap Logout before the Room query returns.

## Fix (#1436)

In `Logout`, read the power user from the repository when logging out instead of using the cached
state.

## Review follow-up: logout cancelled by leaving Settings

An independent review found a second way to lose the write, which was already on master.
`LogoutConfirmed` launches the clear in `viewModelScope` and then navigates with `popUpToRoot()`. That
pops the Settings entry, and once its exit transition ends, the ViewModel is cleared and
`viewModelScope` is cancelled. If the database is slow (the same condition this bug needs), the
`getPowerUser()` / `setPowerUserStatus()` calls are cancelled and `power_user` stays `1`.

Test: `WHEN logout and the screen is popped before the power user has loaded THEN the power-user flag
is still cleared` cancels `viewModelScope` while `getPowerUser()` is suspended. Fix: run the read and
write in `withContext(NonCancellable)`. If the process is killed before the write commits, the flag
can still stay set; that case is rare and not covered here.