# Task 59 - core:testing module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Most unit tests use `MainCoroutineRule` or `FakeFileGenerator`, which lived in `app`'s test
sources, so tests had to stay in `app` even after their code moved. Put the shared helpers in a
module every module's tests can use, and move tests next to the code they test.

## Changes

- New `core:testing` library: `MainCoroutineRule`, `utils/FakeFileGenerator` and the LiveData
  test helper (`utils/LiveDataTestUtil.kt`, was `LiveDataUtilTest.kt`; same functions). Modules add
  it with `testImplementation(project(":core:testing"))`.
- Tests move to the module that has their code, same packages:
  - `core:data`: the `models/` tests (except `TreeCapturerTest`, which stays with capture in
    `app`) and the `usecases/` tests, `TreesToSyncHelperTest`, and
    `TreeTrackerDaoTest` (it uses `FakeFileGenerator`, which needs the data models);
  - `core:ui`: `SelfieViewModelTest`, `UserSelectViewModelTest`, `LanguagePickerViewModelTest`.
    `UserSelectViewModelTest` drops its check that the shared picker leaves capture setup alone:
    `core:ui` can't see capture setup any more, so the compiler now guarantees it.
- The feature ViewModel tests stay in `app` until their feature moves.

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and
spotless pass.
