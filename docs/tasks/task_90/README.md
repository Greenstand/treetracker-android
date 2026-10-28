# Task 90 - Module rules, app shell cleanup and docs

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`). The last
PR of the Phase 0 + 1 stack.

## Goal

Finish Phase 1: keep the new module graph one-way, slim the app shell's dependency list, and
document how the codebase is laid out now and how features talk to each other.

## Changes

- Build rules (`build-logic/.../ModuleRules.kt`): the build fails, with a pointer to the docs, if a
  feature module depends on another feature module, or a core module depends on a feature module
  or `app`.
- `app/build.gradle.kts` now lists only the libraries the app shell uses itself (Koin, Compose,
  Firebase, WorkManager, AppFunctions and so on). AWS, CameraX, MapLibre, Room, Coil, Retrofit
  converters, accompanist, kotlinx-datetime (still a test dependency of `app`), Play install
  referrer and others come from the modules that use them. The Room schema `ksp` argument is gone
  from `app` (no Room code there now).
- Docs:
  - new `docs/engineering/modules.md`: the module map (one feature module per feature), what the
    app shell holds, how features talk to each other (routes in `core:navigation`, screens
    registered per feature with Koin, navigation results, flows owned by one feature),
    dependency rules, convention plugins, how to add a screen or a feature, where tests live;
  - `package-structure.md`: which module each package is in;
  - `architecture.md`: `koinViewModel()` and `LocalNavigator` (replacing the removed
    `LocalViewModelFactory` and `LocalNavHostController`), and how Koin and screen registration
    work now;
  - `navigation-flows.md`: sign-up handing a new planter back to the picker that opened it;
  - `testing.md` and `common-commands.md`: per-module tests and shared test helpers;
  - `kmp-cmp-migration.md`: Phases 0 and 1 marked done, the target layout updated to one module
    per feature, Koin 4 and the `feature:capture` decision marked done, and how the built layout
    differs from the plan;
  - `CLAUDE.md` links the modules doc.

## Verification

- Each rule was tried by adding a forbidden dependency (`feature:trees` -> `feature:capture`,
  `core:data` -> `feature:capture`, `core:ui` -> `app`); each fails configuration with its
  message.
- `app`'s release and debug runtime classpaths are identical before and after trimming.
- All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless
  pass.
