# Task 54 - core:designsystem module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the theme and the shared Compose components into a module that every feature can depend on.

## Changes

- Into `core/designsystem`, same packages: `theme/`, `view/` (except `LanguageButton.kt`),
  `view/dialogs/` and `utils/PreviewDependencies.kt`. Their `R` imports now point at
  `core:resources`; no other code changes.
- `core:designsystem` exposes `core:resources` and the Compose bundle as `api` dependencies, so
  features get them by depending on the design system.
- `LanguageButton` stays in `app` for now: it gets `LanguagePickerViewModel`, a screen ViewModel.
  It moves with the other shared screen-level UI in a later PR.
- Screenshot tests stay in `app` (they share one base class); they still cover these components.

## Verification

All variants assemble; unit tests (488), screenshot tests (no changes), ktlint, detekt and
spotless pass.
