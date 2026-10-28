# Task 84 - feature:settings module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move settings and the profile screens into one `feature:settings` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/settings`, same packages, no code changes: `settings/` (the settings screen and
  `SettingsModule.kt`) and `profile/` (profile select, profile, delete profile).
- Its unit tests move with it: `SettingsViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.
- Extra dependency: `maplibre.android.sdk` (settings initialises MapLibre).

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
