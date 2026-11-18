# Task 109 - A deleted user stays the current user

Follow-up found in the review of PR #1429 (bug #1416).

## Problem

Settings → Delete account uses `UserSelectViewModel`. `DeleteProfileScreen` sends
`UserSelectAction.SelectUser` for the user picked for deletion, and `SelectUser` makes that user
the current user: it calls `prefs.setUserId(user.id)` and sets
`CaptureSetupScopeManager.getData().user`. `UserSelectAction.DeleteUser` then deletes the user's
row, but resets neither. After a successful delete, the app's current user id (which keys every
`UserPrefKey`) and the capture-setup user still point at a user who no longer exists.

## Proof (failing test first)

`UserSelectViewModelTest`:
`GIVEN a user selected for deletion WHEN delete succeeds THEN the deleted user is no longer the current user`.
It selects a user, deletes them, and asserts that the last id written with `prefs.setUserId` and
the capture-setup user are no longer that user. Before the fix it fails with
"Deleted user <id> is still the current user id in Preferences".

## Fix

_Pending: added in the fix commit._
