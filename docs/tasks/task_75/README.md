# Task 75 - feature:dashboard module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the dashboard into its own feature module.

## Changes

- `dashboard/` (2 files) moves to `feature/dashboard` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: DashboardViewModelTest.
- `feature:dashboard` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.
- Extra dependencies: `androidx.work.runtime.ktx`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
