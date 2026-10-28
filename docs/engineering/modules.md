# Modules

The app is split into Gradle modules: an app shell, core modules and feature modules. There is
one feature module per feature (capture, onboarding, settings...), not one per screen. Kotlin
packages didn't change when code moved into modules, so a class keeps its package (for example
`org.greenstand.android.TreeTracker.models.UserRepo`) whichever module it's in.

## Layout

```
app/                 the app shell
build-logic/         Gradle convention plugins
core/
  config/            build-type config: BuildConfig (endpoints, keys, version) and FeatureFlags
  resources/         Android resources: strings (en, pt, sw), drawables, fonts, themes, icons
  common/            small helpers: DeviceUtils, GPS and image utils, validation, extensions
  model/             plain domain types: User, Org data, message types, location data
  preferences/       SharedPreferences wrapper and keys
  database/          Room AppDatabase: entities, DAOs, migrations, exported schemas
  analytics/         analytics events and exception logging (Firebase)
  network/           S3 object storage client, request types, messaging API (Retrofit)
  navigation/        routes, Navigator (and navigation results), deep links, BaseViewModel, UI events
  designsystem/      theme and shared Compose components (view/, theme/)
  data/              repositories, uploaders, sync worker, session tracking, sensors and location,
                     use cases, MessageDatabase
  ui/                shared screen-level UI: the user picker, camera, selfie and image review
                     screens, ImageCaptureActivity, language button
  testing/           shared test helpers: MainCoroutineRule, FakeFileGenerator, LiveData helpers
feature/
  onboarding/        splash, language picker, sign-up
  dashboard/         the home screen
  capture/           capture setup (planter, org, wallet, session note), the capture flow (camera,
                     image review, tree height), their flow controllers and state, permissions
  trees/             tree list, tree detail and edit, the map
  settings/          settings and the profile screens
  messages/          message lists, chat, announcements, surveys
  devtools/          developer options and the debug overlays (debug builds)
```

## What the app shell holds

- `TreeTrackerApplication`, `TreeTrackerActivity` and the manifest (activities, services,
  permissions, `app_name` per build type).
- `root/`: `Root` and `Host`, the `NavDisplay`. `Host` doesn't know any screen: it draws whatever
  screen a feature registered for the route on top of the back stack.
- `di/`: the app-wide Koin modules (`AppModule`, `RoomModule`, `NetworkModule`) and
  `FeatureModules.kt`, the list of every feature's Koin module.
- `src/debug`: the agent AppFunctions (debug and dev builds only).
- The Roborazzi screenshot tests (`app/src/test/.../screenshot`), which render screens from
  every feature, and `ScreenRegistrationTest`.

## How features talk to each other

Features never depend on each other; the build fails if one does. Everything they share goes
through `core:navigation`:

- **Routes.** Every screen's route is in `core:navigation` (`Routes.kt`), and all of them
  implement the sealed `Route` interface. Any feature can open any screen by navigating to its
  route, without depending on the feature that owns it.
- **Screens register themselves.** Each feature's Koin module declares the screens for its routes
  with Koin's Navigation 3 support, next to its ViewModels:

  ```kotlin
  val treesModule =
      module {
          viewModel { MapViewModel(get()) }
          navigation<MapRoute> { MapScreen() }
          navigation<TreeDetailRoute> { route -> TreeDetailScreen(treeId = route.treeId) }
      }
  ```

  `Host` uses `koinEntryProvider()`, which collects every registration. `ScreenRegistrationTest`
  fails if a route has no screen, or if two modules register the same route or ViewModel.

There are three ways one feature leads to another:

1. **Go somewhere.** A button navigates to another feature's route: Settings opens `MapRoute`,
   the dashboard opens `MessagesUserSelectRoute`.
2. **Detour and come back.** The other feature does its job, saves what it needs to (a repository
   or preferences), and goes back. The language picker works this way when opened from the
   language button: it saves the language and pops back to whichever screen opened it.
3. **Hand something back.** A screen goes back with a value for the screen underneath:
   `navigator.popBackStackWithResult(key, value)`, read with `navigator.takeResult(key)`. A value
   is read once and is dropped if the back stack changes first. Sign-up uses this: opened from a
   user picker, it goes back with the new planter's id (`SignupFlowRoute.NewUserId`), and the
   screen that opened it decides what happens next. Capture's user select starts capture setup
   with that planter; the profile list just shows them.

A flow belongs to one feature, which owns its state. Capture setup and the capture flow are in
`feature:capture` with their controllers and Koin scopes; other features can send people into a
capture flow by route, and capture can send people out (to the language picker, to sign-up), but
only capture reads or changes its flow state. `CaptureSetupFlowTest` walks the journeys that leave
capture and come back.

## Dependency rules

- Features depend on core modules, never on other features (enforced by the build).
- Core modules never depend on features or on `app` (enforced by the build).
- Shared UI that several features need goes in `core:ui` (if it needs data or ViewModels) or
  `core:designsystem` (if it's a plain component).
- `core:data` exposes the core modules it builds on as `api`, so features get repositories,
  models, preferences and navigation through `core:ui`.

## Convention plugins

Every module applies one of the plugins in `build-logic/`:

| Plugin | For |
| --- | --- |
| `treetracker.android.application` | `app`: SDK levels, Java 11 + desugaring, code quality |
| `treetracker.android.library` | core modules: the same, plus the app's `beta`, `dev` and `prerelease` build types |
| `treetracker.android.compose` | turns on Compose |
| `treetracker.android.feature` | feature modules: library + Compose + `core:ui`, Koin (with Navigation 3 support) and test helpers |
| `treetracker.code-quality` | ktlint, detekt and the spotless license header (applied by the others) |

## Adding a screen or a feature

A new screen in an existing feature:

1. Add its route to `core:navigation` (`Routes.kt`), implementing `Route`.
2. Register it in the feature's Koin module: `navigation<NewRoute> { NewScreen() }`, plus its
   ViewModel.

A new feature module:

1. Create `feature/<name>/build.gradle.kts`:

   ```kotlin
   plugins {
       id("treetracker.android.feature")
   }

   android {
       namespace = "org.greenstand.android.TreeTracker.feature.<name>"
   }
   ```

2. `include(":feature:<name>")` in `settings.gradle.kts`, and
   `implementation(project(":feature:<name>"))` in `app/build.gradle.kts`.
3. Give it a Koin module with its ViewModels and screens, and add that module to
   `app/.../di/FeatureModules.kt`.
4. Use `org.greenstand.android.TreeTracker.resources.R` for strings and drawables.

## Tests

Unit tests live in the module whose code they test (`<module>/src/test`). Modules share helpers
through `testImplementation(project(":core:testing"))` and
`testImplementation(testFixtures(project(":core:navigation")))` (`fakeNavigator`, `FakeClock`).
Feature modules get both from the feature plugin.

```bash
./gradlew test                                  # every module
./gradlew :core:data:testDebugUnitTest          # one module
./gradlew :feature:capture:testDebugUnitTest
```

## Next

Phase 2 of the [KMP and CMP migration plan](kmp-cmp-migration.md) swaps the Android-only
libraries inside these modules; Phase 3 turns them into Kotlin Multiplatform modules.
