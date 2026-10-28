# Task 70 - feature:messages module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the messages screens into its own feature module.

## Changes

- `messages/` (10 files) moves to `feature/messages` with the same package. `R` imports point at
  `core:resources`; no other code changes.
- Its unit tests move with it: AnnouncementViewModelTest, ChatViewModelTest, IndividualMessageListViewModelTest, SurveyViewModelTest.
- `feature:messages` uses the `treetracker.android.feature` convention plugin. `app` depends on it;
  its ViewModels stay registered in `app`'s Koin module, and its screenshot tests stay in `app`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
