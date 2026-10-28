# Task 72 - feature:map module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the map screen into its own feature module.

## Changes

- `map/` (3 files) moves to `feature/map` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: MapViewModelTest.
- `feature:map` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.
- Extra dependencies: `maplibre.android.sdk`, `coil.compose`, `kotlinx.serialization.json`, `androidx.compose.material.icons.extended`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
