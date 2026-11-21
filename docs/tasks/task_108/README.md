# Task 108 - Deleting a user never reports the admin delete request (#1445)

Follow-up from the review of #1429 (bug #1416, see task_95).

## Problem

`UserSelectViewModel.deleteUser` deletes the user locally, sets `ACCOUNTDELETEDLOCALLY`, queues a
message asking the admin to delete the account, and runs `messageRepo.syncMessages()`.
`DeleteProfileState.ACCOUNTDELETEDANDADMINREQUESTED` was never set anywhere, so the state stayed
the same whether that upload worked or not:

| Case | Before #1429 | After #1429 | After this task |
| --- | --- | --- | --- |
| Online, upload succeeds | `ACCOUNTDELETEDLOCALLY` | `ACCOUNTDELETEDLOCALLY` | `ACCOUNTDELETEDANDADMINREQUESTED` |
| Online, upload fails | crash | `ACCOUNTDELETEDLOCALLY`, logged | `ACCOUNTDELETEDLOCALLY`, logged |
| Offline | crash | `ACCOUNTDELETEDLOCALLY`, logged | `ACCOUNTDELETEDLOCALLY`, logged |

Offline and a failed online upload take the same path: `MessageUploader.uploadMessageBundle`
throws before `markMessagesAsUploaded`, so the message keeps `shouldUpload = true` and the next
message sync (dashboard sync, splash bootstrap) uploads it.

## Fix

After `syncMessages()` returns, the state moves from `ACCOUNTDELETEDLOCALLY` to
`ACCOUNTDELETEDANDADMINREQUESTED`. It only moves from `ACCOUNTDELETEDLOCALLY`, so a sync that
returns after another action changed the state does not overwrite it. In the app today nothing can
change it mid-sync (the "Account Deleted" dialog can't be dismissed), so this is a guard, not a fix
for a reachable case. The failure path is unchanged: no crash, message queued,
`ACCOUNTDELETEDLOCALLY`.

## Limits (not changed here)

- The sync runs in `viewModelScope`, and the ViewModel belongs to the `DeleteProfileRoute` entry.
  Tapping OK on the "Account Deleted" dialog pops that entry, which cancels a sync still running.
  The state then never reaches `ACCOUNTDELETEDANDADMINREQUESTED`. If the S3 put had already
  finished, the message stays `shouldUpload = true` and the next sync sends it again. Fixing that
  means running the sync outside the screen (app scope or WorkManager).
- `uploadMessages` uploads every queued message and fails if any bundle fails. If the admin message
  uploaded but another bundle failed, the state stays `ACCOUNTDELETEDLOCALLY`. It errs towards
  "still pending"; a normal return always means the admin message was uploaded.

## Product decision

`DeleteProfileScreen` shows the same "Account Deleted" dialog for both states, and that is kept.
The user sees no difference; the state is now accurate for any UI that wants to tell them the
request is still pending (that would need new strings and translations, so it is left to a
maintainer).

## Tests

- `UserSelectViewModelTest`: new test, upload succeeds → `ACCOUNTDELETEDANDADMINREQUESTED`. Failed
  on CI before the fix (#1443, commit 7b46414).
- New test: state changed during the upload → kept.
- The old success test asserted `ACCOUNTDELETEDLOCALLY` after a successful sync, which was the bug;
  it now checks only the call order.
- The existing failure test (sync throws → `ACCOUNTDELETEDLOCALLY`, no crash) is unchanged.
- `e2e/flows/settings-delete-user-offline.yaml` is unchanged; the dialog it checks shows for both
  states.
