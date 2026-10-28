# Task 44 - Remove TreeTrackerViewModelFactory

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

`models/TreeTrackerViewModelFactory` imported a ViewModel from nearly every screen package, so the
model layer depended on all the UI. That would block splitting `models/` into its own module.

## Changes

- 17 screens get their ViewModel with `koinViewModel()` instead of
  `viewModel(factory = LocalViewModelFactory.current)`.
- Deleted `TreeTrackerViewModelFactory`, `LocalViewModelFactory`, the factory's Koin `single` and
  the `Root(viewModelFactory, ...)` parameter.

## Why behavior doesn't change

The factory only called Koin's `get<T>()` for each ViewModel class, using the same `viewModel {}`
definitions `koinViewModel()` resolves. Both use the current `ViewModelStoreOwner` (the Nav3
entry) and the same default key, so screens that share a ViewModel (e.g. the user-select and
profile screens) still share one instance.

## Verification

- `assembleDebug`, `testDebugUnitTest`, `verifyRoborazziDebug` (no screenshot changes),
  `ktlintCheck`, `detekt` pass.
- On an API 37 emulator: fresh install, admin account and 3 trees via AppFunctions, then opened the
  dashboard, language picker, settings, messages and track screens and went back. No crashes.
