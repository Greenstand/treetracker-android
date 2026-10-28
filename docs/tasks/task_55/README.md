# Task 55 - core:data module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the data and domain layer out of `:app`, so feature modules can use repositories and use
cases without depending on the app. These classes reference each other in loops (for example
`OrgRepo` -> `RouteRegistry`, the capture scopes <-> the flow controllers), so they move together
in one PR. It's a pure move: no logic changes.

## Changes

- Into `core/data`, same packages:
  - the rest of `models/`: user, org and message repos, uploaders, `TreeCapturer`,
    `SessionTracker`, location and sensor capture, feature/org config, install referrer, the
    capture and setup flow scopes;
  - `models/messages/database/` (`MessageDatabase`), with its exported schema moved from
    `app/schemas` to `core/data/schemas`;
  - `usecases/`;
  - `CaptureFlowNavigationController`, `CaptureSetupNavigationController`,
    `FlowNavigationController`;
  - `utilities/TimeProvider`, `dashboard/TreesToSyncHelper`, `devoptions/Configurator` and
    `DevPrefKeys`, and the non-UI overlay trackers (`DebugOverlayManager`,
    `SensorDiagnosticsTracker`, `SyncProgressTracker`).
- `core:data` exposes the core modules it builds on as `api` dependencies.
- `InstallReferrerOrgTest` moves too, since it tests an `internal` function. The other data tests
  stay in `app` until the `core:testing` PR (they use `FakeFileGenerator` / `MainCoroutineRule`).

## Verification

- All variants assemble; unit tests (488, unchanged), screenshot tests (no changes), ktlint,
  detekt and spotless pass. No schema files change.
- Emulator: fresh-install smoke test has no crashes, and dashboard, language and settings are
  pixel-identical to the PR 6 build (the user cards differ only in the random test phone number).
- Upgrade test: PR 6 build with an account and 3 trees, then this build installed on top; the
  dashboard still shows the 3 trees.
