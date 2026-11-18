# Task 95 - Deleting a user offline crashes the app (#1416)

## Problem

Settings → Delete account, with the device offline, crashed with an `AmazonClientException`.
`UserSelectViewModel.deleteUser` deletes the user locally, queues a message to the admin, and then
calls `messageRepo.syncMessages()`. In `MessagesRepo.syncMessages()`, the per-wallet fetch is
inside a try/catch but `messageUploader.uploadMessages()` is not, so the upload failure escaped
`viewModelScope.launch` and took down the app.

## Fix

`UserSelectViewModel.deleteUser` wraps `messageRepo.syncMessages()` in a try/catch (rethrowing
`CancellationException`) and logs the failure. The user is already deleted and the message is
queued in the database, so the next sync uploads it.

The catch is at the call site, not inside `MessagesRepo.syncMessages()`, because
`SyncDataUseCase.executeTrackedStep` relies on that exception to mark the message sync step as
failed. Swallowing it in the repo would report failed message uploads as successful.

Other callers that can hit the same exception when online and the upload fails
(`SplashScreenViewModel.bootstrap`, `DashboardViewModel.syncMessages`) are left as they are; they
are outside this bug.

## Tests

- `UserSelectViewModelTest`: new test, `syncMessages()` throws after a successful delete → no
  crash, state is `ACCOUNTDELETEDLOCALLY`.
- `e2e/flows/settings-delete-user-offline.yaml`: `known-bug` tag removed and header rewritten, so
  `e2e/run.sh` checks it on CI.
- `settings-delete-user-offline.yaml` matches the "Account Deleted" text (the confirm dialog shares
  the `DialogTitle` tag), then reopens Delete account to check Bob is gone and the app is still up.
- E7 (`settings-delete-user.yaml`) and E8 (`settings-delete-admin.yaml`): their KNOWN BUG notes are
  gone, and they now check the "Account Deleted" dialog and where closing it lands (Settings for a
  non-admin, Signup for the admin) before relaunching.
- `docs/testing/e2e-flow-test-plan.md` finding 6 notes the fix and which flows check each path.

## Review follow-ups not done here

- Online, a failed admin-request upload after a delete is now only logged; the user sees the same
  "Account Deleted" dialog. `DeleteProfileState.ACCOUNTDELETEDANDADMINREQUESTED` exists but is never set.
- `DeleteProfileScreen` calls `SelectUser` on the user being deleted, which stores their id as the
  current user id (`prefs.setUserId`) after they're gone. Older code, separate from this bug.
