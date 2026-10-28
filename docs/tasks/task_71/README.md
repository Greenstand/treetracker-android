# Task 71 - feature:treeedit module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the tree list and tree edit screens into its own feature module.

## Changes

- `treeedit/` (5 files) moves to `feature/treeedit` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: TreeDetailViewModelTest, TreeListViewModelTest.
- `feature:treeedit` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.
- `core:data` now exposes `kotlinx-datetime` as an `api` dependency: its public types (trees,
  sessions, messages) use `kotlinx.datetime.Instant`, so modules that read them need it on the
  classpath.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
