# Task 95 - Deleting a user while offline crashes the app (#1416)

## Problem

Settings → Delete account → pick a user → confirm, with the device offline, closed the app with
an `AmazonClientException` (Cognito can't be reached). The user was already deleted locally, so
the "Account Deleted" dialog and the return to Settings (or to Signup, for the admin) never
happened.

`UserSelectViewModel.deleteUser` deletes the user, saves a message asking the admin to delete the
account, then calls `messageRepo.syncMessages()` in `viewModelScope`. `syncMessages` catches
errors while fetching, but `messageUploader.uploadMessages()` is outside that try/catch, so the
upload's exception was uncaught on the main scope.

## Fix

- `userselect/UserSelectViewModel.kt`: `deleteUser` catches a failed `syncMessages()` (not
  cancellation) and logs it with Timber. The request message was saved with `shouldUpload = true`
  before the sync, so it goes out with the next sync.

`MessagesRepo.syncMessages` still throws when the upload fails. `SyncDataUseCase` runs it as the
tracked "Message Sync" step and reports the failure from that exception, and
`MessagesRepoTest` (`sync messages propagates message upload failures`) asserts it. Swallowing
it in the repo would make a failed upload look like a successful sync step.

Not changed: Splash and the Dashboard call `syncMessages()` unguarded too, but only when the
internet check passes. That is the "online with dummy keys" case noted in the test plan, not this
issue.

## Tests

- `UserSelectViewModelTest`: `GIVEN the device is offline WHEN delete succeeds THEN the failed
  message sync does not escape (#1416)`. `syncMessages()` throws; the state is still
  `ACCOUNTDELETEDLOCALLY` and nothing is left uncaught in `viewModelScope` (`runTest` fails the
  test on an uncaught exception).
- `e2e/flows/settings-delete-user-offline.yaml`: the validating flow. Offline, delete Bob, read the
  "Account Deleted" dialog, close it, land on Settings, check the Delete account list has only
  the admin, back to the Dashboard.
- `settings-delete-user.yaml` (E7) and `settings-delete-admin.yaml` (E8) are untouched. Their
  header comments still describe the crash and their `stopApp`/`launchApp` workaround still
  passes; tidying them is a follow-up, kept out of this PR so the before/after run only picks
  the new flow.
- `docs/testing/e2e-flow-test-plan.md`: E7 row and findings 2 and 6 updated.

## Verification

No Android SDK or emulator in the agent container: nothing was built or run here, neither the
unit test nor the flow. The PR's checks run them ("Affected flows (annotated video)" runs the
flow on the merge base's app and on this branch's app).
