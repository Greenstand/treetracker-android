# Modules

The app is split into Gradle modules: an app shell, core modules and feature modules. Kotlin
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
  navigation/        Navigation 3 routes, Navigator, deep links, BaseViewModel and UI events
  designsystem/      theme and shared Compose components (view/, theme/)
  data/              repositories, uploaders, sync worker, sensors and location, use cases,
                     capture flow controllers, MessageDatabase
  ui/                shared screen-level UI: user picker, camera and selfie screens,
                     ImageCaptureActivity, language button, permission prompt
  testing/           shared test helpers: MainCoroutineRule, FakeFileGenerator, LiveData helpers
feature/
  languagepicker/  settings/  sessionnote/  treeheight/  orgpicker/
  walletselect/  splash/  signup/  profile/  messages/  treeedit/  map/  devoptions/
  overlay/  dashboard/  capture/
```

## What the app shell holds

- `TreeTrackerApplication`, `TreeTrackerActivity` and the manifest (activities, services,
  permissions, `app_name` per build type).
- `root/`: `Root` and `Host`, the `NavDisplay` that maps every route to its feature screen.
- `di/`: the Koin modules, including every feature's ViewModel definitions.
- `src/debug`: the agent AppFunctions (debug and dev builds only).
- The Roborazzi screenshot tests (`app/src/test/.../screenshot`), which render screens from
  every feature.

## Dependency rules

- Features depend on core modules, never on other features. To open another feature's screen,
  navigate to its route from `core:navigation`. The build fails if a feature module depends on
  another feature module.
- Core modules never depend on features or on `app`.
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
| `treetracker.android.feature` | feature modules: library + Compose + `core:ui`, Koin and test helpers |
| `treetracker.code-quality` | ktlint, detekt and the spotless license header (applied by the others) |

## Adding a feature module

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
3. Add its route to `core:navigation` (`Routes.kt`), its entry to `root/Host.kt`, and its
   ViewModels to `di/AppModule.kt`.
4. Use `org.greenstand.android.TreeTracker.resources.R` for strings and drawables.

## Tests

Unit tests live in the module whose code they test (`<module>/src/test`). Modules share helpers
through `testImplementation(project(":core:testing"))` and
`testImplementation(testFixtures(project(":core:navigation")))` (`fakeNavigator`, `FakeClock`).
Feature modules get both from the feature plugin.

```bash
./gradlew test                                  # every module
./gradlew :core:data:testDebugUnitTest          # one module
./gradlew :feature:dashboard:testDebugUnitTest
```

## Next

Phase 2 of the [KMP and CMP migration plan](kmp-cmp-migration.md) swaps the Android-only
libraries inside these modules; Phase 3 turns them into Kotlin Multiplatform modules.
