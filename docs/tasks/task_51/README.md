# Task 51 - core:network module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the code that talks to servers into its own module: the S3 object storage client, the
upload request types, and the messaging API (Retrofit service, builder, request and response
types).

## Changes

- `api/` and `models/messages/network/` move to `core/network` with the same packages.
- `MessagesRepo.saveMessage`: `message.survey` is now read into a local `val` after the null
  check. Kotlin can't smart-cast a property declared in another module, and the logic is the
  same.
- Tests for the uploaders and `MessagesRepo` stay in `app` with the code they test.

## Verification

All variants assemble; unit tests (including `MessagesRepoTest`), screenshot tests (no changes),
ktlint, detekt and spotless pass.
