# Task 77 - feature:capture module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the tree capture screens into its own feature module.

## Changes

- `capture/` (4 files) moves to `feature/capture` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: TreeCaptureViewModelTest, TreeImageReviewViewModelTest.
- `feature:capture` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.
- Extra dependencies: `accompanist.permissions`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
