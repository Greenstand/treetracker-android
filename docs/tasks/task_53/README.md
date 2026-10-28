# Task 53 - core:model module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Give the plain domain types their own dependency-free module, so the design system and features
can use them (for example `User` in `SelectableItemDetail`) without depending on the data layer.

## Changes

- Into `core/model`, same packages: `models/user/User.kt`, `models/messages/Data.kt`,
  `models/organization/OrgData.kt` and `OrgJsonKeys.kt`, `models/location/Locations.kt`,
  `models/captureflowdata/CaptureFlowData.kt`. These files import nothing else from the app. No
  code changes.
- The rest of `models/` (repos, uploaders, sensors, flows) stays in `app` and moves with the data
  layer.

## Verification

All variants assemble; unit tests (488 across modules), screenshot tests (no changes), ktlint,
detekt and spotless pass.
