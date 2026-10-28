# Task 89 - feature:capture module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move tree capture (capture setup and the capture flow) into one `feature:capture` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/capture`, same packages, no code changes: 
  - the screens: `capture/` (camera capture, tree image review, `CaptureModule.kt`),
    `treeheight/`, `sessionnote/`, `walletselect/`, `orgpicker/`, and capture's own user select
    screen (`userselect/UserSelectScreen.kt`, which starts capture setup);
  - the flows: `CaptureSetupNavigationController`, `CaptureFlowNavigationController`,
    `FlowNavigationController`, their Koin scopes and state (`models/setupflow/`,
    `models/captureflowdata/`) and `TreeCapturer`;
  - `permissions/` (the location, camera and activity recognition prompt; only capture uses it).
- Its unit tests move with it: `AddOrgViewModelTest`, `AddWalletViewModelTest`, `CaptureSetupFlowTest`, `OrgPickerViewModelTest`, `PermissionViewModelTest`, `SessionNoteViewModelTest`, `TreeCaptureViewModelTest`, `TreeCapturerTest`, `TreeHeightViewModelTest`, `TreeImageReviewViewModelTest`, `WalletSelectViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.
- Extra dependency: `accompanist.permissions`.
- Capture's flow state lives only here: nothing outside `feature:capture` can read or change it
  (tasks 81 and 82 removed the last outside users).

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
