# Task 76 - Permission prompt in core:ui

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

`permissions/` isn't a screen: it's `PermissionRequest`, a prompt for location, camera and activity
recognition (with its rationale and denied dialogs and `PermissionViewModel`) that the tree
capture screen shows. Task 64 made it a feature module, which would make `feature:capture` depend
on another feature. Move it to `core:ui`, where shared screen-level UI lives.

## Changes

- `permissions/` (main and test) moves from `feature/permissions` to `core/ui`, same package, no
  code changes. `feature:permissions` is removed.
- `core:ui` depends on Accompanist permissions.

## Verification

All variants assemble; unit tests (492), screenshot tests (no changes), ktlint, detekt and
spotless pass.
