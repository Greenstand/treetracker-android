# Task 118 - E2E flows for sync and upload

## Goal

Cover the Sync and upload flows of the end-to-end test plan (issue #1396, section D of
`docs/testing/e2e-flow-test-plan.md`) with Test Buddy flows in `e2e/flows/`.

## Problem

CI runs offline with dummy storage keys, so no upload can ever succeed there and a sync that
completes cannot be tested against the real S3 client.

## Changes

- `FakeObjectStorage` (debug source set) replaces S3 with a stand-in where every upload succeeds,
  after an optional delay. `ObjectStorageClient.s3Client` is no longer private so debug builds can
  install it; release builds never do.
- The `treetracker-e2e://` screen links take `trees=N` (adds N trees, in batches of 500) and
  `storage=ok|slow` (installs the fake). Both finish before the screen renders. Only debug builds
  accept them: dev builds compile the same activity and reach testers.
- Flows, tag `sync`: `sync-happy-path` (D1), `sync-stop` (D3), `sync-offline-retry` (D4) and
  `sync-reminder-dialog` (D5), `sync-nothing-to-sync` (D2). D3 uses 122 trees, as trees upload in bundles of 50. D4's "online"
  half is the fake storage, as CI has no network.
- D2 (nothing to sync): debug builds skip that check, so `FeatureFlags.releaseSyncChecks` turns it
  back on. It is off by default and only the `syncChecks=release` link parameter sets it, in debug
  builds. `sync-nothing-to-sync` syncs the seeded trees first, then expects "Nothing to Sync".
- The dashboard snackbar had its tag on the container, so a flow could not match its message. The
  message text now has its own tag, `SnackbarMessage`.
- `e2e/run-sync.sh` resets and seeds the device before each sync flow, and `e2e/run.sh` runs it
  after the org flows. The CI flow runner (`e2e/ci/run-flows.sh`, `e2e/ci/affected.py`) seeds sync
  flows the same way.

## Verification

- `./gradlew ktlintCheck detekt` pass.
- `./gradlew :app:testDebugUnitTest`: 2750 of 2754 pass, including `FakeObjectStorageTest`. The 4
  failures run on a Windows machine, in classes this task does not touch (`AgentAccountCreatorTest`
  and `AgentTreeCapturerTest` compare Unix paths, `AppDatabaseMigrationTest` cannot open Robolectric's
  temp database).
- `tb check --lint` on the five flows: no warnings.
- `bash e2e/run-sync.sh -s <emulator>` on an API 36 emulator: D1, D3, D4 and D5 pass in four runs in
  a row, and all five (with D2) in a fifth.
- `DashboardViewModelSyncTest` covers the `releaseSyncChecks` flag.
- Not yet run on API 35, which CI uses.
