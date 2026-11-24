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

`SettingsViewModelTest` had
`WHEN logout before the power user has loaded THEN the power-user flag is still cleared`: the
`getPowerUser()` mock suspended on a `CompletableDeferred`, so `LogoutConfirmed` ran while
`state.powerUser` was still `null`. A unit test was enough because the race was in the ViewModel. An
e2e flow can't reliably tap Logout inside the few milliseconds the Room query takes. The fix below
replaced that test, because logout no longer touches the power user.

## Fix (#1436): logout no longer touches the power user

The maintainer decided that a device's power user is permanent: once set, nobody else can take it
over, whoever signs in after a logout. So logout no longer clears the power-user flag at all, and the
race above can't happen, because there is nothing to clear.

Logout instead sets a "logged out" flag, `UserRepo.LOGGED_OUT` (`greenstand/system-settings/logged-out`
in `Preferences`), written with `commit()` so it survives the app being killed right after:

- `SettingsViewModel` `Logout` calls `userRepo.setLoggedOut(true)` and closes the dialog. No coroutine,
  so leaving Settings can't cancel it.
- The splash screen opens Signup when the device has a power user and is logged out
  (`SplashScreenViewModel.isLoggedOut()`), instead of the Dashboard.
- On Signup after a logout (`SignUpState.isLoggedOut`), signing in as an existing user or signing up
  a new one sends `SignupAction.LogBackIn` (clears the flag) and opens the Dashboard. The new user is a
  regular user, because `createUser` only makes someone power user when there is none.
- With no power user (a fresh install), sign-up still makes the first person the power user, as before.

The logout dialog still shows the power user's card from `state.powerUser`.

History: logout came from #1143 (issue #1130), which cleared the flag on purpose so the next person to
sign in became the power user. This replaces that behaviour.

## Proof

- Validating flow `e2e/flows/settings-logout-keeps-power-user.yaml`: Ada signs up, logs out, Bob signs
  up, and the logout dialog must still show Ada. Before this change Bob became the power user.
- `e2e/flows/settings-logout-relaunch.yaml`: after logout, a relaunch opens Signup (before: the
  language picker, because the device had no power user); after signing back in, a relaunch opens the
  Dashboard.
- Unit tests: `SettingsViewModelTest` (logout sets the flag and never calls `setPowerUserStatus`),
  `UserRepoTest` (the flag is read and committed), `SignupViewModelTest` (`LogBackIn`),
  `SplashScreenViewModelTest` (`isLoggedOut`).
- Existing flows `settings-logout.yaml` (E6) and `settings-logout-back.yaml` are unchanged and must
  keep passing: signing back in as Ada after a logout still reaches the Dashboard.

## Not changed

Deleting the power user's account (Settings, Delete account) still removes that row, which leaves the
device with no power user. Whether that should be allowed is a separate question.
