# Task 65 - feature:orgpicker module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the organization picker and add-organization screens into its own feature module.

## Changes

- `orgpicker/` (4 files) moves to `feature/orgpicker` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: AddOrgViewModelTest, OrgPickerViewModelTest.
- `feature:orgpicker` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
