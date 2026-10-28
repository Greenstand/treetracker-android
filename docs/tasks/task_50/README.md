# Task 50 - core:analytics module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move analytics events and exception logging (Firebase Analytics and Crashlytics) into their own
module, so navigation and data code can log without depending on the app.

## Changes

- `analytics/` moves to `core/analytics` with the same package. No code changes.
- `core:analytics` depends on `core:common` (`DeviceUtils`), `core:config` (version) and Firebase.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
