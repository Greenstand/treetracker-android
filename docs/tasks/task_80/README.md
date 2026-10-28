# Task 80 - Each feature registers its own screens

Part of the KMP/CMP migration, Phase 1 (see `docs/engineering/kmp-cmp-migration.md`).

## Goal

Let each feature register the screens for its routes, and its ViewModels, in its own Koin
module, using Koin's Navigation 3 support. `Host` no longer lists every screen: it draws whatever
screen a feature registered for the route on top of the back stack. When features become modules,
each one carries its Koin module with it and the app only lists the modules.

## Changes

- New Koin modules, one per feature, each next to the code it will move with:
  `onboardingModule` (splash, language, sign-up), `captureModule` (org and wallet setup, session
  note, capture, image review, tree height, user select), `dashboardModule`, `settingsModule`
  (settings and profiles), `treesModule` (tree list and detail, map), `messagesModule`,
  `devToolsModule`, and `uiModule` for the shared selfie and image review screens.
  Each one declares its ViewModels and `navigation<SomeRoute> { ... }` for its screens.
- `di/FeatureModules.kt` lists them; `TreeTrackerApplication` loads them with the other modules.
- `Host` uses `koinEntryProvider()` instead of its own `entryProvider { ... }` list.
- The ViewModel definitions move out of `AppModule` into the feature modules (no other changes).
- Routes now implement a sealed `Route` interface (it extends `NavKey`), so tests can list them.
- New `ScreenRegistrationTest`: every route has a screen registered, and no route or ViewModel is
  registered twice (this would otherwise only fail when someone navigates there).

## Verification

- All variants assemble; unit tests, screenshot tests (no changes), ktlint, detekt and spotless pass.
- `ScreenRegistrationTest` fails if a route's screen is removed or registered twice (checked by
  breaking the dashboard registration on purpose).
- Emulator (API 37): smoke test (dashboard, language, settings, messages, track) matches the
  previous build's screenshots; sign-up through the UI works.
