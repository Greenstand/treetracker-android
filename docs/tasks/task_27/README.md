# Task 27 - Count unsynced trees by upload flag

Fixes audit finding C2 ([task_25](../task_25/README.md)).

## Problem

The dashboard's "trees to sync", the Sync button gate, the worker's progress
notification, and `TreesToSyncHelper` all counted trees with
`photo_url IS NULL`. A tree whose image uploaded but whose bundle PUT failed has
`photo_url` set and `uploaded = 0`, so it counted as synced. The dashboard then
showed 0 remaining. In non-debug builds the Sync button refused to run
("nothing to sync"), which stranded those trees.

## Changes

- `TreeTrackerDAO` and `TreeDAO`: the eight `photo_url`-based count queries are
  replaced with `uploaded`-flag queries. There are new Flow variants
  `getUploadedTreeCaptureCountFlow`, `getUploadedTreeCountFlow`,
  `getNonUploadedTreeCaptureCountFlow` and `getNonUploadedTreeCountFlow`,
  alongside the existing suspend ones. The two DAOs stay in step, per #1235.
- `DashboardViewModel`, `TreesToSyncHelper` and `TreeSyncWorker` now count by
  `uploaded`, the same flag the uploader selects on
  (`getAll*IdsToUpload`).
- `TreeSyncWorker` recounts the stored "trees to sync" total when it starts.
  That total was only refreshed at a few points (session end, tree delete, and
  splash when unset). So right after updating from a build that counted by
  `photo_url`, the sync notification used a stale total, e.g.
  "Uploading trees (0/0)" with 3 trees to upload.

## Behavior notes

- "Synced" on the dashboard now means the tree's bundle was uploaded, not just
  its photo.
- During a sync, the progress count now moves when each bundle of up to 50
  trees finishes, rather than after each image.
- Counts also move the other way on devices with migrated 1.x data.
  `MIGRATION_3_4` set `tree_capture.uploaded` from `is_synced` but never set
  `photo_url`. Already-synced legacy trees were therefore counted as "remaining"
  forever, which also counted toward the 2000-tree reminder dialog. They now count
  as synced.
- The `SYNC_BUTTON_CLICKED` / `STOP_BUTTON_CLICKED` analytics values (synced,
  unsynced, total) now use the same `uploaded`-based meaning, so those charts
  have a break at this release.

## Tests

- `UnsyncedTreeCountIntegrationTest` runs:
  - a real in-memory Room DB;
  - the real `TreeUploader`, with mocked storage whose bundle PUT throws;
  - the real `TreesToSyncHelper`.

  It checks that the PUT was attempted and that the tree ends with `photo_url`
  set and `uploaded = 0`. It then checks that the tree still counts as left to
  sync.
  - `DeviceUtils` is mocked, as in the other uploader tests. An earlier version
    of this test didn't mock it, so `UploadBundle` failed on the missing
    Application context before the PUT was reached.
  - Against `master`, the test gets through the PUT and then fails with
    `expected:<1> but was:<0>`. That is the dashboard's "nothing to sync" bug.
- `TreeTrackerDaoTest`: a real in-memory Room DB with trees that have an
  uploaded image but no uploaded bundle. These count as not uploaded, in both the
  v2 `tree` and legacy `tree_capture` tables, for both the suspend and Flow
  queries, through both `TreeTrackerDAO` and `TreeDAO`.
- `DashboardViewModelTest` and `TreesToSyncHelperTest` now stub the renamed
  queries.

## Device validation

Emulator, a debug build of `master` and a debug build with all five audit fixes, both on the same
database: 3 v2 trees with `photo_url` set and `uploaded = 0` (image uploaded,
bundle failed), plus 1 fully uploaded legacy tree.

| Build | Dashboard "synced" | Dashboard "remaining" |
| --- | --- | --- |
| `master` (pre-fix) | 4 | **0** |
| fixed build | 1 | **3** |

In non-debug builds, the pre-fix "0 remaining" is what makes the Sync button
say "nothing to sync".

**Stale total after upgrading.**
- Running `master` on that database stores 0 as the "trees to sync" total.
- After upgrading in place, an earlier version of this branch showed 3
  remaining on the dashboard, but the sync notification read **"Uploading trees
  (0/0)"**.
- With the worker's recount, starting a sync refreshes the stored total from 0
  to 3, so the notification's total matches the dashboard.
