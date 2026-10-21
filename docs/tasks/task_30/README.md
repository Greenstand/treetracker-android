# Task 30 - Contain message upload failures

Fixes audit finding C5 ([task_25](../task_25/README.md)).

## Problem

`MessagesRepo.syncMessages()` fetched messages per wallet inside a try/catch,
but called `messageUploader.uploadMessages()` outside it. An S3 or Cognito
failure (`AmazonClientException`) while uploading queued outgoing messages
escaped to the caller:

- **Crash (UI scopes with no exception handler):** these call sites crash.
  - The Dashboard Messages button (`DashboardViewModel.syncMessages`).
  - Deleting a profile (`UserSelectViewModel.deleteUser`). It queues a
    "delete my account" message and syncs right away.
  - Splash bootstrap (`SplashScreenViewModel.bootstrap` via the splash screen's
    `rememberCoroutineScope`). This crashes on every launch while a message is
    queued and the internet check passes but S3 fails.
- **Blocked sync:** messages are the first step of `SyncDataUseCase`, and
  `executeTrackedStep` rethrows. One message upload failure therefore meant
  device config, users, sessions, trees and locations were never attempted.

## Reproduction

The first versions of the two failure tests were written against the old API,
and both failed before the fix:

- `MessagesRepoTest`: an upload that throws `AmazonClientException` →
  `syncMessages()` threw it.
- `SyncDataUseCaseTest`: `syncMessages()` throws → `deviceConfigUploader.upload`
  (and everything after it) "was not called".

The final tests stub `syncMessages()`'s new `Boolean` result, so they no
longer compile against `master`. The emulator runs below reproduce the crashes
on `master` directly.

## Changes

- `MessagesRepo.syncMessages()` catches upload failures, logs them, and returns
  `false`, or `true` on success. Queued messages stay queued for the next sync.
  - Loading the user list is now inside error handling as well. If it fails, no
    messages are fetched, but queued messages are still uploaded.
  - Fetch errors are logged per wallet and don't affect the result, as before.
  - Only `CancellationException` propagates.
- `SyncDataUseCase` runs the messages step with `executeNonFatalStep`. A thrown
  exception or `false` marks the step failed, with the exception's message, and
  the sync moves on. The overall result is `false` if queued messages weren't
  uploaded, and the debug overlay error reads "Messages failed to upload".
  - If the sync is stopped during this step, the step is marked "cancelled"
    rather than left running or shown as an ordinary failure.
- The UI callers need no changes, since `syncMessages()` no longer throws for
  these errors.

## Tests

- `MessagesRepoTest`:
  - upload failure → returns `false` and doesn't throw;
  - upload success → `true`;
  - a fetch failure still uploads queued messages;
  - a failure loading users doesn't throw and still uploads queued messages;
  - cancellation still propagates.
- `SyncDataUseCaseTest`:
  - `syncMessages()` throws or returns `false` → device config, users, sessions,
    trees and locations still upload, the MESSAGES step is marked failed (with
    the error), and the sync ends with "Messages failed to upload" and `false`;
  - cancellation during the step marks it cancelled.
- `DashboardViewModelTest` stubs `syncMessages()` with its new return value.

## Behavior note

When trees upload but queued messages don't, the worker reports failure, and
the dashboard shows "Sync failed". On `master` this case also ended as failed,
but without uploading anything. Reporting tree and message results separately
would be a follow-up UI change.

## Not in scope

- `syncMessages()` still has no mutex, so concurrent callers can upload the same
  queued messages twice (audit M6/M9).
- The message sync cursor issue (audit H5).

## Device validation

These ran on the emulator with the dummy build keys, so every S3/Cognito call
fails (`AmazonServiceException ... identityPoolId ... ValidationException`).

- **`master` (pre-fix):**
  - Settings → Delete Account → confirm crashed with `FATAL EXCEPTION: main
    com.amazonaws.AmazonServiceException`, from a `Dispatchers.Main.immediate`
    coroutine.
  - The "delete my account" message then stayed queued, and **every
    relaunch crashed** in the splash bootstrap (`MessageUploader.uploadMessageBundle`
    ← `ObjectStorageClient.uploadBundle`). The app was unusable until its data
    was cleared.
- **Fixed build** (a debug build with all five audit fixes combined; only this
  one affects message sync):
  - I installed it over that crash-looping data, with the message still
    queued. It launches normally and logs `Message upload failed`.
  - The Dashboard Messages button, Delete Account, and two more relaunches all
    complete with no crash. The message stays queued for the next sync.
