# KMP and CMP Migration Plan

How to move Treetracker from a single Android module to Kotlin Multiplatform (KMP) with Compose
Multiplatform (CMP) UI, and split it into modules on the way.

Status: proposal (task 40). Nothing here is implemented yet. Sequencing is decided: structure
first (Phases 0 to 3), then ship iOS (Phase 4).

## Where we start

The app is in better shape for this than most Android apps:

| Area | Today | KMP-ready? |
| --- | --- | --- |
| Language | 100% Kotlin, ~25k lines in `app/src/main`, 79 test files | Yes |
| UI | 100% Jetpack Compose (Material 1), no XML layouts | Yes, via CMP |
| Navigation | Navigation 3 with `@Serializable` `NavKey` routes | Yes, JetBrains publishes Nav3 for CMP |
| ViewModels | `androidx.lifecycle` ViewModel, `koinViewModel` | Yes, JetBrains lifecycle artifacts |
| DI | Koin 3.5 | Yes after Koin 4.x |
| Database | Room 2.8.4, two databases (`AppDatabase` v9, `MessageDatabase` v1) | Yes, Room is KMP since 2.7 |
| Serialization | kotlinx.serialization | Yes |
| Dates | kotlinx-datetime 0.5 in 9 files, `SimpleDateFormat`/`java.util` in a few | Mostly |
| Networking | Retrofit, but only one endpoint (`GET messaging/message`) | Small swap to Ktor |
| Uploads | AWS Android SDK S3 + Cognito (`ObjectStorageClient`) | No, needs replacing |
| Prefs | `SharedPreferences` wrapper (`Preferences`, `PrefKeys`) | No, small swap |
| Logging | Timber in 34 files | No, mechanical swap |
| Images | Coil 2 | Coil 3 is KMP |
| Map | MapLibre Android SDK via `AndroidView` | maplibre-compose is KMP |
| Camera | CameraX | No, platform code |
| Location / sensors | `LocationManager`, `SensorManager` (step counter, orientation) | No, platform code |
| Background sync | WorkManager `TreeSyncWorker` | No, platform code |
| Firebase | Analytics, Crashlytics, Remote Config, Installations | Platform code or GitLive SDK |
| Android-only extras | Install referrer, AppFunctions (debug), `AppCompatDelegate` locales | Stay in the Android app |
| Resources | 152 strings in en/pt/sw, drawables, fonts; `R.` used in ~50 files | Move to Compose resources |

The hard parts are the device-facing pieces (camera, location, sensors, background upload, S3)
and keeping existing installs' databases and preferences intact. The UI layer is the easy part.

## Goals

1. Shared business logic and UI in `commonMain`, with Android behaving exactly as it does today.
2. Small, reviewable modules with one-way dependencies, so builds are faster and features are
   isolated.
3. An iOS app becomes possible (and, if we want it, shippable) without a rewrite.
4. Every step ships as its own PR and leaves `master` releasable. No long-lived migration branch.

## Non-goals

- Changing product behavior or visuals during the migration. Roborazzi screenshots must not
  change unless a PR says why.
- A desktop or web target. The module layout keeps it possible, nothing more.
- Moving off Material 1. A Material 3 move can come later as its own project.

## Target module layout

```
build-logic/                      Gradle convention plugins (kmp-library, cmp-feature, android-app, ...)

core/
  common/        commonMain   dispatchers, Result types, time (TimeProvider), UUIDs, logging facade
  model/         commonMain   domain models: Tree, User, Session, Org, Location, Message, ...
  database/      commonMain   Room KMP: entities, DAOs, AppDatabase, MessageDatabase, migrations
  datastore/     commonMain   Preferences, PrefKey/PrefKeys/UserPrefKey on DataStore or Settings
  network/       commonMain   Ktor client, MessagesApi, request/response DTOs
  storage/       commonMain   ObjectStorage interface; Android + iOS upload implementations
  platform/      commonMain   interfaces for location, sensors, device info, connectivity, files;
                              androidMain + iosMain implementations
  analytics/     commonMain   Analytics + ExceptionLogger interfaces; Firebase impls per platform
  data/          commonMain   repos and uploaders: UserRepo, OrgRepo, MessagesRepo, TreeCapturer,
                              SessionTracker, *Uploader, usecases/, SyncDataUseCase
  designsystem/  commonMain   theme/, view/ components, fonts, shared drawables, Colors
  resources/     commonMain   strings (en/pt/sw) as Compose resources (or fold into designsystem)
  navigation/    commonMain   Routes, Navigator, flow controllers, RouteRegistry
  testing/       commonMain   fakes, MainDispatcherRule equivalent, test data builders

feature/
  splash/  signup/  userselect/  dashboard/  orgpicker/  walletselect/  profile/
  settings/  languagepicker/  messages/  map/  devoptions/
  capture/       capture/, camera/, treeheight/, treeedit/, sessionnote/ (the add-tree flow)
  overlay/       debug overlays (sync, sensors)

shared/          commonMain   the app shell: root/Host (NavDisplay + entryProvider), Koin graph,
                              App() composable. Depends on every feature. Exposes
                              MainViewController() for iOS.

androidApp/      Android      TreeTrackerApplication, activities, manifest, build types and
                              BuildConfig, WorkManager, install referrer, AppFunctions (src/debug),
                              Firebase setup, google-services, signing
iosApp/          Xcode        SwiftUI shell that hosts MainViewController(), Info.plist, Firebase,
                              background task registration
```

Rules:

- `feature:*` depends on `core:*`, never on another feature. Cross-feature navigation goes
  through route types in `core:navigation`.
- `core:model` and `core:common` depend on nothing in the project.
- Only `androidApp` and `iosApp` know about build types, secrets and SDK keys. They hand config
  to shared code through a `AppConfig` data class registered in Koin (replaces `BuildConfig` in
  the 9 files that read it).
- Platform services are interfaces in `commonMain`, bound in Koin per platform. Prefer this over
  `expect class`; use `expect`/`actual` only for small functions (`currentTimeMillis`,
  `platformName`, etc.).

Mapping today's packages:

| Today | Goes to |
| --- | --- |
| `database/`, `models/messages/database/` | `core:database` |
| `preferences/` | `core:datastore` |
| `models/messages/network/`, `api/models/` | `core:network` |
| `api/ObjectStorageClient` | `core:storage` |
| `analytics/` | `core:analytics` |
| `models/location/`, `StepCounter`, `DeviceOrientation`, `utilities/GpsUtils`, `DeviceUtils` | interfaces in `core:platform`, logic in `core:data` |
| `models/` (repos, uploaders, capturer, feature flags, org config), `usecases/` | `core:data` (+ plain types to `core:model`) |
| `models/organization/InstallReferrer*` | `androidApp` (interface in `core:data`) |
| `background/` | `androidApp` (worker) calling `SyncDataUseCase` from `core:data` |
| `theme/`, `view/`, `utils/` UI helpers | `core:designsystem` |
| `navigation/` | `core:navigation` |
| `root/` | `shared` |
| `viewmodel/`, `models/TreeTrackerViewModelFactory` | deleted; Koin `viewModel {}` in each feature |
| each screen package | its `feature:*` module |
| `activities/`, `application/`, `di/` Android bits | `androidApp` |
| `src/debug` AppFunctions | `androidApp/src/debug` |

## Library swaps

| From | To | Notes |
| --- | --- | --- |
| Jetpack Compose (androidx) | Compose Multiplatform (`org.jetbrains.compose`) | Same APIs. Material 1 is available in CMP. |
| `androidx.navigation3` | JetBrains `navigation3-ui` | Routes already `@Serializable`; register subclasses for non-JVM saved state. |
| `androidx.lifecycle` ViewModel/compose | `org.jetbrains.androidx.lifecycle` | Drop `LiveData` (5 files) for `StateFlow`. |
| Koin 3.5 | Koin 4.x (`koin-core`, `koin-compose-viewmodel`) | `koin-android` only in `androidApp`. |
| Retrofit + OkHttp | Ktor client (OkHttp engine on Android, Darwin on iOS) | One endpoint today, so this is small. |
| AWS Android SDK S3 + Cognito | See "Image uploads" | The biggest open decision. |
| `SharedPreferences` | DataStore Preferences KMP or `multiplatform-settings` | Must read the existing prefs file on Android. |
| Timber | Kermit | Mechanical; keep a Crashlytics log writer. |
| Coil 2 | Coil 3 | Package rename `coil` -> `coil3`. |
| MapLibre Android + `AndroidView` | maplibre-compose | Check feature parity of `LibreMap.kt` first. |
| accompanist-permissions | small `PermissionController` interface (or moko-permissions) | Accompanist is deprecated anyway. |
| `java.util.UUID` (12 files) | `kotlin.uuid.Uuid` | |
| `java.io.File` (9 files) | kotlinx-io / Okio `Path` | Image paths, upload payloads. |
| `SimpleDateFormat`, `java.util.Date` | kotlinx-datetime 0.7 (`kotlin.time.Instant`) | 0.7 moved `Instant`; do the bump in its own PR. |
| `R.string` / `R.drawable` / `R.font` | Compose resources (`Res.string.*`) | Keep en/pt/sw. App name and launcher icons stay in `androidApp/res`. |
| Firebase Android SDK | Interfaces in `core:analytics`, Android impl as today, iOS impl via Firebase iOS SDK (or GitLive `firebase-kotlin-sdk`) | Remote Config feeds `FeatureFlags`. |
| mockk in tests | Fakes in `commonTest`; mockk stays in `androidUnitTest` | mockk is JVM-only. |

## Phases

Each phase is a series of small PRs. Each PR keeps the app building, passing tests and
releasable, with unchanged screenshots.

### Phase 0: Build foundation

1. Upgrade AGP to 9.x. AGP 9 doesn't allow the KMP plugin and `com.android.application` in the same
   module, and it brings the `com.android.kotlin.multiplatform.library` plugin used for every
   shared module. It also unblocks AppFunctions alpha09+ (needs AGP 9.1 and compileSdk 37).
2. Convert `build.gradle`/`settings.gradle` to Kotlin DSL.
3. Add `build-logic/` with convention plugins: `treetracker.android.application`,
   `treetracker.kmp.library`, `treetracker.cmp.feature`, `treetracker.room`, plus shared
   ktlint/spotless/detekt setup.
4. Rename `:app` to `:androidApp` (keep the application ID, namespace and signing). Optional; can
   wait until Phase 1 is underway.

### Phase 1: Modularize on Android, no KMP yet

Split the code into the target modules as plain Android library modules first. This gets the
dependency graph right without also fighting multiplatform problems.

1. Break cycles first. `models/` imports UI packages (`TreeTrackerViewModelFactory`,
   `SessionTracker`, `ConvergenceConfiguration` reach into screen packages). Remove the
   ViewModel factory in favour of Koin and move any shared types down into the model layer.
2. Extract leaf modules bottom-up, one PR each: `core:common`, `core:model`, `core:database`,
   `core:datastore`, `core:network`, `core:analytics`, `core:storage`, `core:platform`,
   `core:designsystem`, `core:navigation`, `core:data`.
3. Extract features, smallest and most isolated first: `languagepicker`, `settings`, `profile`,
   `orgpicker`, `walletselect`, `userselect`, `splash`, `signup`, `messages`, `devoptions`,
   `dashboard`, `map`, then `capture` last (it's the largest and most device-bound).
4. Move tests with the code they test. Screenshot tests move to each feature module
   (Roborazzi runs per module).
5. Add a dependency check (e.g. module graph assertion plugin) so features can't depend on
   each other.

Exit: `:app` is just the shell; every package lives in a module; build and tests unchanged.

### Phase 2: Swap Android-only libraries, still Android-only

Do every library swap from the table above while the code still compiles for Android only. Each
swap is independently testable on a phone, which is much safer than doing it during the KMP move.

Suggested order (cheapest and least risky first):

1. Timber -> Kermit. `java.util.UUID` -> `kotlin.uuid`. `LiveData` -> `StateFlow`.
2. kotlinx-datetime 0.5 -> 0.7, remove `SimpleDateFormat`/`java.util.Date`.
3. Koin 3.5 -> 4.x.
4. Coil 2 -> 3.
5. Retrofit -> Ktor for `MessagesApi`.
6. `SharedPreferences` -> DataStore/Settings, with a migration that reads the existing
   `SharedPreferences` file so users keep their state (logged-in user, org, language, session).
7. `java.io.File` -> Okio/kotlinx-io in image and upload code.
8. Hide device APIs behind `core:platform` interfaces: `LocationProvider`,
   `StepCounter`, `OrientationSensor`, `ConnectivityChecker`, `DeviceInfo`,
   `ImageStore`, `PermissionController`, `LocaleController` (wraps `AppCompatDelegate`).
   Existing code becomes the Android implementation.
9. Hide CameraX behind a `CameraPreview` composable slot + `CameraController` interface, so the
   capture feature's shared code only sees "take photo -> path".
10. Room: rewrite the manual migrations in `Migrations.kt` (4->5, 5->6, 6->7, which use
    `SupportSQLiteDatabase`) to the `SQLiteConnection` API, and keep the 7->8 auto-migration.
    Switch DAOs to `suspend`/`Flow` only. Add migration tests from every shipped schema
    (3, 4, 6, 7, 8 -> 9) before changing drivers.
11. Strings and drawables -> Compose resources.
12. MapLibre -> maplibre-compose.
13. AndroidX Nav3/lifecycle -> JetBrains artifacts.

Exit: no `android.*` or Android-only library imports outside `androidMain`-to-be code
(`core:platform` Android impls, `core:storage` Android impl, `androidApp`). A lint/detekt rule
or a simple grep in CI enforces this.

### Phase 3: Turn modules into KMP, Android target only

1. Switch each module to the `treetracker.kmp.library` / `treetracker.cmp.feature` convention
   plugin with only the `androidTarget()`. Move code to `commonMain`, Android implementations to
   `androidMain`. Go bottom-up, same order as Phase 1.
2. Tests: logic tests with fakes go to `commonTest`. Tests that need mockk, Robolectric or
   Roborazzi stay in `androidUnitTest`.
3. Room on Android: keep the same database file names and schema; use the Android SQLite driver
   first, consider `BundledSQLiteDriver` later for parity with iOS. Run the migration tests again.
4. Add the `shared` module and move `root/Host` there behind an `App()` composable.
   `MainActivity` just calls `setContent { App() }`.

Exit: the Android app is built from KMP modules, behaves the same, and `commonMain` compiles
for the JVM-free metadata target.

### Phase 4: iOS target

1. Add `iosArm64()` and `iosSimulatorArm64()` to the convention plugins. Fix whatever fails to
   compile (usually leftover JVM APIs: `String.format`, `synchronized`, reflection,
   `System.currentTimeMillis`).
2. Create `iosApp` (SwiftUI shell, `ComposeUIViewController` from `shared`), Koin start-up,
   `AppConfig` per environment (dev/test/prod, matching Android build types).
3. iOS implementations of `core:platform`: CoreLocation, CoreMotion (step counter,
   orientation), connectivity (`NWPathMonitor`), file storage, permissions, locale.
4. Camera on iOS: AVFoundation preview via `UIKitView`, capture to a file. Port or replace the
   blur detection.
5. Map on iOS: maplibre-compose's iOS backend.
6. Uploads and background sync on iOS: `BGTaskScheduler` + background `URLSession` for images.
   iOS gives far less background time than WorkManager, so expect a "sync when app is open"
   fallback.
7. Firebase on iOS (Analytics, Crashlytics, Remote Config).
8. CI: macOS runner that builds the iOS framework and runs `iosSimulatorArm64Test`. TestFlight
   lane in fastlane (we already use fastlane for Android).

9. App Store setup: Apple developer account and bundle IDs per environment, privacy manifest
   and location/camera/motion usage strings, App Store review notes on background location.

Exit: iOS app can sign up a planter, capture a tree with photo + GPS, and upload it to the
test environment, then ships to TestFlight testers and the App Store.

### Phase 5: Clean up

- Remove leftover Android-only fallbacks and the CI grep allowlist.
- Update `architecture.md`, `package-structure.md`, `common-commands.md`, `testing.md` and the
  agent docs (`agent-app-functions.md`, PR media skill) for the new module paths.
- Consider Material 3, desktop/JVM target for fast screenshot tests, etc.

## Image uploads (open decision)

`ObjectStorageClient` uses the AWS Android SDK with a Cognito identity pool to put images in S3.
That SDK has no iOS/KMP equivalent we can call from common code. Options:

1. **Interface + per-platform SDK (lowest risk).** `ObjectStorage` interface in `commonMain`;
   Android keeps the current SDK, iOS uses AWS SDK for Swift / Amplify, bridged in from Swift.
   No backend change; two implementations to maintain.
2. **Pre-signed URLs from the Treetracker API (cleanest).** The backend hands out a pre-signed
   PUT URL; the app uploads with Ktor. One shared implementation, no AWS credentials in the app.
   Needs a backend endpoint.
3. **Cognito + SigV4 by hand over Ktor.** Shared code, no backend change, but we own the AWS
   signing code.

Recommendation: since iOS will ship, ask the backend team about option 2 now, so it can be
ready by Phase 4. In Phase 2, wrap today's code behind the `ObjectStorage` interface (option 1's
first half) either way. If option 2 isn't possible, fall back to option 1 on iOS.

## Risks and how we handle them

| Risk | Mitigation |
| --- | --- |
| Existing users lose data on upgrade (DB or prefs) | Migration tests for every shipped schema; prefs migration that reads the old file; install-over-old-version check on a real device for every phase's first release |
| Screens change subtly when moving to CMP or Compose resources | Roborazzi screenshots must stay identical; PR media before/after for UI PRs |
| Big-bang PRs stall | One module or one library per PR; master releasable after each |
| maplibre-compose or CMP Nav3 missing something we use | Spike each before its Phase 2 step; keep the Android `AndroidView` map as a fallback in `androidMain` if needed |
| iOS background upload limits | Design sync as resumable and foreground-friendly; don't promise WorkManager parity |
| Camera blur detection is Android-specific | Keep it behind the `CameraController` interface; iOS can ship without it at first (it's already off in beta builds) |
| Build gets slower with many modules | Convention plugins, configuration cache, build cache; avoid `api` deps |
| AppFunctions/install referrer have no iOS meaning | They stay in `androidApp` behind interfaces; iOS binds no-op or skips them |

## Testing at each step

- Each PR: the moved module's own tests, plus direct dependents (`./gradlew :core:database:test`,
  `:feature:capture:verifyRoborazziDebug`, etc.). Full test runs before merging a phase's last PR.
- Database: migration tests from schemas 3, 4, 6, 7, 8 to current, on both drivers.
- Manual on device at the end of each phase: install over the latest Play Store build, check
  logged-in user, unsynced trees, capture, upload, messages, language switch.
- Agent AppFunctions (debug builds) give a quick scripted smoke test: create account, create
  trees, sync.

## Decisions to make before starting

1. ~~Is iOS a real goal?~~ **Decided: structure first, then ship iOS.** Phases 0 to 3 come
   first and stand on their own value (modules, faster builds, cleaner boundaries). Phase 4 is
   committed work that starts once Phase 3 is done, ending in an iOS app in the App Store.
   Because iOS will ship, Phases 1 to 3 should already pick KMP-friendly options (for example
   the upload approach below) rather than Android-only stopgaps.
2. **Upload approach** (see above).
3. **Preferences library:** DataStore (Google-backed, async) vs `multiplatform-settings`
   (synchronous, closer to today's `Preferences` API, so less churn). Leaning
   `multiplatform-settings` because `Preferences` is read synchronously in many places.
4. **Granularity of `feature:capture`.** One module for the whole add-tree flow, or split camera
   and tree height out. Start with one; split if it grows.

## Rough size

Assuming one developer with agent help, and small PRs:

| Phase | Size |
| --- | --- |
| 0 Build foundation | ~1 week |
| 1 Modularize | 2-3 weeks |
| 2 Library swaps | 3-4 weeks |
| 3 KMP (Android only) | 1-2 weeks |
| 4 iOS | 4-6 weeks, mostly camera, uploads, background sync, and store setup |
| 5 Clean up | ongoing |

Library versions above should be checked against the latest stable releases when each phase
starts; KMP libraries move quickly.
