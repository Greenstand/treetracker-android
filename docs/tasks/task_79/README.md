# Task 79 - Upgrade Koin to 4.2

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Upgrade Koin from 3.5.6 to 4.2.2, so the next PRs can register each feature's screens with
Koin's Navigation 3 support, and so the app uses Koin's multiplatform Compose APIs ahead of KMP.

## Changes

- `koin` 3.5.6 -> 4.2.2. Adds `koin-compose-navigation3` to the Koin bundle (used by the next PR).
- Screens get ViewModels with `org.koin.compose.viewmodel.koinViewModel` (the multiplatform
  version) instead of `org.koin.androidx.compose.koinViewModel`.
- `SelfieScreen` used the removed `getViewModel`; it now uses `koinViewModel` (same ViewModel,
  same scope: one per back stack entry).
- `AppModule` uses `org.koin.core.module.dsl.viewModel` (the Koin 4 DSL).

## Verification

All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
