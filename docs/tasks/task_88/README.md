# Task 88 - feature:dashboard module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the dashboard (home screen) into one `feature:dashboard` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/dashboard`, same packages, no code changes: `dashboard/` (`DashboardScreen`,
  `DashboardViewModel` and `DashboardModule.kt`; `TreesToSyncHelper` is already in `core:data`).
- Its unit tests move with it: `DashboardViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.
- Extra dependency: `work.runtime.ktx` (the dashboard watches the sync worker).

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
