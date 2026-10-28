# Task 47 - core:preferences module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the `preferences` package (the `SharedPreferences` wrapper and its keys) into its own module.
It has no dependencies on the rest of the app, so it's the first plain code module.

## Changes

- `preferences/` moves to `core/preferences` with the same package. No code changes.
- `app` depends on `:core:preferences`.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
