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
- `docs/testing/e2e-flow-test-plan.md` finding 6 notes the fix.
