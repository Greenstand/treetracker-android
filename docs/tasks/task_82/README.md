# Task 82 - Capture owns its flows

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Make capture setup and the capture flow belong to capture alone, so they can move into
`feature:capture` instead of a core module. After the previous PR the only code outside capture
that still reached into capture setup state was `SessionTracker`; this PR removes that, registers
capture's flow state in capture's Koin module, and adds tests for flows that pass through other
features and come back.

## Changes

- `SessionTracker.startSession(userId, organizationName, sessionNote)` takes the session details
  instead of reading capture setup state. `CaptureSetupNavigationController` passes them in.
- Capture's Koin scopes (setup data and controller, capture flow data and controller,
  `TreeCapturer`) move from `AppModule` into `captureModule`.
- `startCaptureSetup(user, navigator)`: starting setup from user select is a plain function, so
  tests can drive it.
- New `CaptureSetupFlowTest`, using the real flow controller:
  - user select -> language picker -> back -> wallet -> org -> session note -> capture, starting
    the session with the right user, org and note;
  - sign-up returns a new planter to user select, and setup carries on with them;
  - going back to user select and on again starts setup from the first step.
- Files that move with capture later (not moved yet, packages unchanged): the flow controllers in
  `navigation/`, `models/setupflow/`, `models/captureflowdata/`, `models/TreeCapturer.kt`,
  `userselect/UserSelectScreen.kt` and `permissions/`.

## Verification

- All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
- `CaptureSetupFlowTest` fails if setup doesn't start through the flow controller (checked by
  breaking it on purpose).
- Emulator (API 37): smoke test matches; adding a planter from capture and from profiles behaves as
  in the previous PR; a real capture (user select -> org -> camera -> approve) saves the tree and
  the dashboard shows it.
