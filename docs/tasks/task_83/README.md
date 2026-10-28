# Task 83 - feature:devtools module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the developer tools (debug builds) into one `feature:devtools` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/devtools`, same packages, no code changes: `devoptions/` (the developer options
  screen), `overlay/` (the sync and sensor overlays drawn over the app) and
  `devtools/DevToolsModule.kt`.
- Its unit tests move with it: `DevOptionsViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.
- New `treetracker.android.feature` convention plugin: library + Compose + `core:ui`, Koin
  (including `koin-compose-navigation3`), and the shared test helpers.

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
