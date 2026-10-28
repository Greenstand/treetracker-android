# Task 60 - feature:languagepicker module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the language picker screen into its own feature module.

## Changes

- `languagepicker/` (1 file) moves to `feature/languagepicker` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- `feature:languagepicker` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.
- New convention plugin `treetracker.android.feature`: library + Compose, depends on `core:ui`, Koin, lifecycle-compose, and the shared test helpers. Each feature's build file is just its namespace.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
