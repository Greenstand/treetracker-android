# Task 61 - feature:settings module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the settings screen into its own feature module.

## Changes

- `settings/` (2 files) moves to `feature/settings` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: SettingsViewModelTest.
- `feature:settings` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.
- The feature convention plugin now also adds `androidx.core:core-ktx` (several features use `ContextCompat`).
- Extra dependencies: `maplibre.android.sdk`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
