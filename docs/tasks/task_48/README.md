# Task 48 - core:database module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the Room database (`AppDatabase`: entities, DAOs, converters, migrations and exported
schemas) into its own module. It depends on nothing else in the app.

## Changes

- `database/` moves to `core/database` with the same package. No code changes.
- `app/schemas/...AppDatabase` moves to `core/database/schemas`; `core:database` runs the Room
  compiler with that schema location.
- `AppDatabaseMigrationTest` and `ConvertersTest` move with it. `TreeTrackerDaoTest` stays in
  `app` for now because it uses `FakeFileGenerator`, which builds both entities and app models; a
  later PR in this stack moves shared fakes into a `core:testing` module.
- `MessageDatabase` (in `models/messages/database`) stays in `app` for now. It depends on the
  message domain and network types, so it moves with the messages code.
- `kotlin-test` is in the shared `testing` bundle; `app` tests were getting it transitively.
- `.geminiignore` covers the new schema folder.

## Verification

- All variants assemble; unit tests (including the 13 migration and converter tests, now in
  `core:database`), screenshot tests (no changes), ktlint, detekt and spotless pass. No schema
  files change.
- Emulator: fresh-install smoke test has no crashes. Upgrade test: installed the PR 6 build, added
  an account and 3 trees, installed this build on top; the dashboard still shows the 3 trees.
