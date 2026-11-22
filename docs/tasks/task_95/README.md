# Task 95 - Deleting a user while offline crashes the app (#1416)

## Problem

Settings → Delete account → pick a user → confirm, with the device offline, crashed the app with
an `AmazonClientException`. The user was already removed from the device.

`UserSelectViewModel.deleteUser` deletes the user, saves a message asking the admin to delete the
account, then calls `messageRepo.syncMessages()`. In `syncMessages()` the message fetch is inside a
try/catch but `messageUploader.uploadMessages()` is not, so when Cognito can't be reached the
exception left the `viewModelScope` coroutine uncaught.

## Fix

- `userselect/UserSelectViewModel.kt`: `deleteUser` catches and logs a failure of
  `syncMessages()` (cancellation is rethrown). The delete is local and already done, and the
  request to the admin is saved with `shouldUpload = true`, so the next sync sends it.

`MessagesRepo.syncMessages()` is unchanged and still throws when the upload fails:
`SyncDataUseCase` relies on that to report the message step of a sync as failed
(`MessagesRepoTest`, "sync messages propagates message upload failures").

## Tests

- `UserSelectViewModelTest`: when `syncMessages()` throws after a successful delete, nothing
  escapes, the state is `ACCOUNTDELETEDLOCALLY` and the admin message was saved.
- `e2e/flows/settings-delete-user.yaml` (E7): asserts the "Account Deleted" dialog, that closing it
  returns to Settings, and that Bob is gone from the Delete account list, before and after a
  relaunch.
- `e2e/flows/settings-delete-admin.yaml` (E8): asserts the "Account Deleted" dialog, that closing
  it opens Signup, and that back from Signup exits the app.
- `docs/testing/e2e-flow-test-plan.md`: E7/E8 rows and findings 2 and 6 updated.

## Not changed

`SplashScreenViewModel` and `DashboardViewModel` also call `syncMessages()` without a try/catch.
They check for a connection first, so they are not hit offline, but an upload that fails while
online would still crash there.

## Verification

No Android SDK or emulator in the agent container: nothing was built or run. The unit tests and
the "Android end-to-end" check on the PR run the test and the flows (the e2e run is offline).
