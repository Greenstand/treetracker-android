# Task 26 - Isolate tree upload failures

Fixes audit finding C1 ([task_25](../task_25/README.md)).

## Problem

Trees upload in windows of 50, and each window was all-or-nothing. One failed
image upload (missing file, `photoPath!!`, `error("No imageUrl")`, or an S3
error) was swallowed by a catch-all in `windowedTreeUpload`. The whole window
was skipped, and the same window was retried and skipped again on every sync.
The other 49 trees never uploaded. `SyncDataUseCase.treeUpload` then marked the
step complete, so the worker reported success.

## Changes

- **Per-tree image uploads.** `TreeUploader` uploads each tree image on its own
  (`uploadTreeImage`). A null path, a null URL, or an exception affects only that
  tree, and each failure is logged once.
- **Per-tree bundle requests.** Each tree's bundle request is also built on its
  own (`buildTreeRequests`). A tree whose request can't be built is left out of
  the bundle, for example a legacy tree whose planter check-in is missing, or a
  v2 tree whose session can't be loaded.
- **What a bundle contains.** Only trees that are actually in the uploaded bundle
  are marked uploaded and have their local image deleted. Every other tree keeps
  `uploaded = 0` and is retried on the next sync. A window with nothing to bundle
  skips the bundle PUT entirely.
- **Cancellation.** `CancellationException` is rethrown instead of swallowed by
  the per-window catch-all. Later windows were already skipped once the job was
  cancelled.
- **Sync result.** `SyncDataUseCase.treeUpload` returns whether every tree was
  uploaded. If trees remain, it marks the step failed rather than complete. The
  later steps (locations) still run, `execute` returns `false`, and the worker
  reports failure instead of success. The debug overlay's progress is now
  "total minus remaining" rather than "trees attempted".

## Tests

- `TreeUploaderTest`. The other trees are still bundled, marked uploaded and
  cleaned up when one tree:
  - has an image upload that returns null, and is left out of the bundle JSON;
  - has an image upload that throws;
  - has no local path;
  - is a legacy tree whose image fails;
  - is a legacy tree whose request can't be built (no planter check-in);
  - is a v2 tree whose session can't be loaded.

  Also: if every image fails, no bundle is uploaded, and cancellation propagates.
- `SyncDataUseCaseTest`:
  - Trees left pending: returns `false`, fails the TREES step with "Some trees
    failed to upload", and still uploads locations.
  - Legacy trees left: the TREES step still runs.
  - Trees captured mid-sync while one keeps failing: the new ones upload and the
    step fails.
- 10 of the 11 new tests fail when run against `master`. The exception, "every
  image fails, so no bundle is sent", pins down behavior `master` already had.

## Not in scope

- A tree whose image file is gone for good is still retried on every sync.
  It no longer blocks other trees, but it keeps the sync reporting failure.
  Recording a per-tree error or retry count needs a schema change.
- `PlanterUploader` has the same all-or-nothing pattern (`photoUrl!!`, audit H6).
- Sessions upload before trees, and a failed session upload still stops the
  sync before any tree is attempted. `SessionUploader` sends a bundle even when
  there are no sessions (audit L3).

## Device validation

This could not be reproduced on the emulator. With the dummy build keys, every
S3 upload fails, and the sync stops at the session upload step before reaching
trees (see "Not in scope"). That held even with a database whose legacy trees
already had uploaded images, one of them with a missing planter check-in. The
unit tests above run the real `TreeUploader` logic against mocked storage.
