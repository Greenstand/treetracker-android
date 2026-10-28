# Task 78 - Module rules, app shell cleanup and docs

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`). The last
PR of the Phase 0 + 1 stack.

## Goal

Finish Phase 1: keep the new module graph one-way, slim the app shell's dependency list, and
document how the codebase is laid out now.

## Changes

- Build rules (`build-logic/.../ModuleRules.kt`): the build fails, with a pointer to the docs, if a
  feature module depends on another feature module, or a core module depends on a feature module
  or `app`.
- `app/build.gradle.kts` now lists only the libraries the app shell uses itself (Koin, Compose,
  Firebase, WorkManager, AppFunctions and so on). AWS, CameraX, MapLibre, Room, Coil, Retrofit
  converters, Play install referrer and others come from the modules that use them. The Room
  schema `ksp` argument is gone from `app` (no Room code there now).
- Docs:
  - new `docs/engineering/modules.md`: the module map, what the app shell holds, dependency
    rules, convention plugins, how to add a feature, where tests live;
  - `package-structure.md`: which module each package is in;
  - `architecture.md`: `koinViewModel()` and `LocalNavigator` (replacing the removed
    `LocalViewModelFactory` and `LocalNavHostController`), and where DI and previews live;
  - `testing.md` and `common-commands.md`: per-module tests and shared test helpers;
  - `kmp-cmp-migration.md`: Phase 0 and 1 marked done, with how the built layout differs from
    the plan;
  - `CLAUDE.md` links the modules doc.

## Verification

- Each rule was tried by adding a forbidden dependency (feature to feature, core to feature, core
  to app); each fails configuration with its message.
- `app`'s release and debug runtime classpaths are identical before and after trimming.
- All variants assemble; unit tests (492, same as before the stack split them up), screenshot
  tests (no changes), ktlint, detekt and spotless pass.
- Emulator (API 37) on this build:
  - first sign-up through the UI, including the selfie tutorial, ends on the dashboard;
  - smoke test (dashboard, language, settings, messages, track): no crashes, and the screens are
    pixel-identical to the PR 6 build apart from the random test phone number;
  - upgrade from the PR 6 build with an account and 3 trees keeps the trees;
  - the map renders, and a tree captured through the camera flow is saved after GPS converges.
