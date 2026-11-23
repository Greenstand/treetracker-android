# Task 109 - A deleted user stays the current user

Follow-up found in the review of PR #1429 (bug #1416).

## Problem

Settings → Delete account uses `UserSelectViewModel`. `DeleteProfileScreen` sends
`UserSelectAction.SelectUser` for the user picked for deletion, and `SelectUser` makes that user
the current user: it calls `prefs.setUserId(user.id)` and sets
`CaptureSetupScopeManager.getData().user`. `UserSelectAction.DeleteUser` then deletes the user's
row, but resets neither. After a successful delete, the app's current user id (which keys every
`UserPrefKey`) and the capture-setup user still point at a user who no longer exists.

No production code builds a `UserPrefKey` today, so the stale `Preferences` id has no visible
effect yet. The capture-setup user is the half that matters: capture setup screens read it.

### Where a user sees it

Found in the second independent review of #1444:

1. A capture ends on user select through the capture screen's profile button
   (`CaptureFlowNavigationController.goToUserSelect`), which leaves the back stack as `[UserSelect]`.
2. The user highlights Ada and taps the back arrow. That pushes the Dashboard (there's no Dashboard
   entry to pop to), so the stack is `[UserSelect, Dashboard]` and the user select's view model
   stays alive.
3. Settings → Delete account. Its own `UserSelectViewModel` opens a new capture setup scope, picks
   Bob (making him the setup user) and deletes him.
4. Track pushes `UserSelectRoute` again. Navigation 3 keys view models by content key, so it gets
   the kept view model. Its `resumeSetup()` does nothing, because a scope is open, so Ada is still
   highlighted.
5. Next sets up capture for whoever the setup scope holds.

On master that is the deleted Bob, and the app crashes when setup finishes:
`IllegalStateException: Could not find user of id 2` in `SessionTracker.createSession`
(Flow runner run 45096848552, `crash.txt`). With only the clear above, the scope holds nobody:
Add org loses setup and returns to the Dashboard, and a setup flow of only user select would crash on
`user!!` in `SessionTracker.startSession`.

## Proof (failing test first)

`UserSelectViewModelTest`:
`GIVEN a user selected for deletion WHEN delete succeeds THEN the deleted user is no longer the current user`.
It selects a user, deletes them, and asserts that the last id written with `prefs.setUserId` and
the capture-setup user are no longer that user. Before the fix it failed in CI (issue #1448, run 44240549081) with
`AssertionError: Deleted user 122 is still the current user id in Preferences. Illegal value: <122>.`

To get that message, a temporary commit set `testLogging { exceptionFormat = 'full' }` in `app/build.gradle`; without it Gradle
printed only `AssertionError at UserSelectViewModelTest.kt:264`. It was removed before review.

## Fix

`UserSelectViewModel` remembers the id that `SelectUser` made current (`currentUserId`). After
`userRepo.deleteUser` succeeds, `clearCurrentUser(deletedUser)`:

- calls `prefs.setUserId(null)` if the deleted user is the one this view model made current;
- sets `CaptureSetupScopeManager.getData().user` to `null` if it is the deleted user.

A failed delete changes nothing. A user who is current but not the one deleted is left alone. That
check is defensive: only `DeleteProfileScreen` sends `DeleteUser`, and it always sends `SelectUser`
first (`ProfileScreen`'s by-id view model never deletes).

The delete suspends on Room, so the capture setup scope can in principle close before the clear
runs, and `CaptureSetupScopeManager.getData()` throws when it is closed. The clear checks
`CaptureSetupScopeManager.isOpen` first, so the delete still updates the state and queues the
admin request.

Next on the capture user select (`UserSelectScreen`) now sends `SelectUser` for the highlighted
user before moving on, so capture is always set up for the user shown as selected.

## Tests

- The failing test above, unchanged, now passes.
- `GIVEN a user selected for deletion WHEN delete fails THEN they stay the current user`.
- `GIVEN another user is current WHEN a user opened by id is deleted THEN the current user is kept`.
- `GIVEN the setup scope closes while the delete runs WHEN delete succeeds THEN it does not crash and
  still asks admin`. Before the `isOpen` check it failed in CI (run 44311599051, 1830 tests, 1
  failed) at the deleted-state assertion, because the unguarded `getData()` threw.
- The tests that let `syncMessages()` succeed accept either deleted state, so they keep passing
  with #1443, which moves a successful sync to `ACCOUNTDELETEDANDADMINREQUESTED`.
- `UserSelectViewModelTest.setUp` stubs `CaptureSetupScopeManager.isOpen` to true. `open()` is
  stubbed there, so the real getter would report a closed scope while `getData()` returns data.
  A future `resumeSetup` test in that class must stub it to false.
- `ReturnToUserSelectFromCaptureUiTest`:
  `after another user is deleted in Settings, Next on the returning user select sets up capture for
  the highlighted user` drives steps 1–5 above with the app's `NavDisplay` decorators. Before the
  `UserSelectScreen` change it failed in CI (2752 tests, 1 failed) at the setup-user assertion.
- Validating flow `e2e/flows/settings-delete-user-then-capture.yaml`: steps 1–5 on the real app,
  then one tree captured, which must count on Ada's card. On master it fails where capture should
  open, because the app crashes.
- e2e `settings-delete-user.yaml` (E7) and `settings-delete-admin.yaml` (E8) are unchanged and must
  keep passing in the "Android end-to-end" check.
