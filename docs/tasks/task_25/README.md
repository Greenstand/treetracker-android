# Task 25 - Address review findings on upload failure logging

## Goal

Fix the problems found while reviewing the upload failure logging PR (issue
#1317). The PR must only add logging: upload behavior has to stay exactly as it
is on `master`.

## Problem

The reviews ran the PR against `master` on an emulator with induced failures:

1. A tree whose photo failed to upload was sent without a photo, marked
   uploaded and had its local photo deleted (`master` kept it pending).
2. Network failures were labelled `server_failure`: the AWS SDK wraps them in
   `AmazonClientException`, which the `IOException` checks never matched.
3. `failure_type` was always empty in Crashlytics: it was cleared right after
   `recordException`, but Crashlytics reads custom keys later, on a background
   thread.
4. Behavior changes: one failed tree bundle ended the whole sync, cancelling a
   sync left `is_syncing=true`, and each failure was reported 3 times.

A second review asked to keep the diff to logging only: keep the existing
catches and forward to the error tracking, and use a single catch where one is
added.

## Changes

- `TreeUploader`, `UploadImageUseCase`, `UploadLocationDataUseCase`,
  `SyncDataUseCase` and `TreeSyncWorker` are back to their `master` logic. The
  only additions are the calls that forward failures to `ExceptionDataCollector`.
- `PlanterUploader`, `SessionUploader`, `DeviceConfigUploader` and
  `MessageUploader` had no error handling; each gets a single
  `catch (e: Exception)` that records the failure and rethrows it.
- `ExceptionDataCollector.recordFailure(throwable, message)` classifies the
  failure itself (`classify`). `AmazonServiceException` is `server_failure`, an
  `AmazonClientException` caused by an `IOException` is `network_failure`.
  Cancellations are ignored.
- `failure_type` is no longer cleared after each failure, so Crashlytics keeps
  the most recent upload failure type.
- `SyncDataUseCase` skips its own error log for failures that were already
  recorded, so a failure is not reported once per layer.

## Out of scope

- Attaching `failure_type` to a single event with
  `recordException(Throwable, CustomKeysAndValues)` needs Crashlytics 19.x, i.e.
  a Firebase BoM upgrade (currently 32.8.0). Left for a separate change.
- A failed photo is reported twice: by `UploadImageUseCase` and by the bundle
  catch in `TreeUploader`.

## Verification

- `./gradlew :app:detekt :app:ktlintCheck` pass.
- Unit tests for the classifier, `recordFailure` and the `TreeUploader`
  behaviors above.
