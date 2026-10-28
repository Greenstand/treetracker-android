# Task 52 - core:navigation module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the navigation plumbing every screen uses into its own module, so feature modules can
navigate without depending on each other or on the app.

## Changes

- Into `core/navigation`, same packages: `navigation/` `Routes`, `Navigator`, `RouteRegistry`,
  `OrgDeepLink`, `ScreenTrackingNavEntryDecorator`, and all of `viewmodel/` (`BaseViewModel`,
  `Action`, UI events, `SnackbarController`, `TextRef`). No code changes.
- Their tests (`NavigatorTest`, `OrgDeepLinkTest`, `RouteRegistryTest`) move too.
- `FakeNavigator.kt` (`fakeNavigator`, `FakeClock`) becomes a test fixture of `core:navigation`,
  so this module's tests and `app`'s ViewModel tests share it.
- Stay in `app`: `CaptureFlowNavigationController`, `CaptureSetupNavigationController` and
  `FlowNavigationController`. They drive the capture flows using domain classes (`OrgRepo`,
  `TreeCapturer`, sensors), so they move with the data layer.

## Verification

- All variants assemble; unit tests (40 in `core:navigation`, 430 in `app`), screenshot tests (no
  changes), ktlint, detekt and spotless pass.
- Emulator smoke test: no crashes.
