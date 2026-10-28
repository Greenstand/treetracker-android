# Task 86 - feature:messages module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move the messages screens into one `feature:messages` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/messages`, same packages, no code changes: `messages/` (the planter picker for
  messages, message lists, chat, announcements, surveys and `MessagesModule.kt`).
- Its unit tests move with it: `AnnouncementViewModelTest`, `ChatViewModelTest`, `IndividualMessageListViewModelTest`, `SurveyViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
