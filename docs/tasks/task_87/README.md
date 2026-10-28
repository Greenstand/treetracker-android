# Task 87 - feature:onboarding module

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Move first-run onboarding (splash, language picker, sign-up) into one `feature:onboarding` module. Feature modules are one per feature, not one per
screen; they don't depend on each other and open other features' screens through the routes in
`core:navigation`.

## Changes

- Into `feature/onboarding`, same packages, no code changes: `splash/`, `languagepicker/`
  (`LanguageSelectScreen`; its ViewModel is in `core:ui` with the language button), `signup/` and
  `onboarding/OnboardingModule.kt`.
- Its unit tests move with it: `SignupViewModelTest`, `SplashScreenViewModelTest`.
- Its Koin module (ViewModels and `navigation<Route>` screen registrations) moves with it; `app`
  still lists it in `di/FeatureModules.kt`. Its screenshot tests stay in `app`.
- Sign-up no longer touches capture: it hands the new user back through the navigator (task 81),
  so onboarding doesn't depend on capture.

## Verification

All variants assemble; unit tests (including `ScreenRegistrationTest`), screenshot tests (no
changes), ktlint, detekt and spotless pass.
