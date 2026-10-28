# Package Structure

All code is under `org.greenstand.android.TreeTracker`. Packages didn't change when the app was
split into modules ([Modules](modules.md)), so a package can tell you what something is, and this
table tells you which module it's in.

| Package | What it holds | Module |
| --- | --- | --- |
| `activities/` | `TreeTrackerActivity` (main) | `app` |
| `activities/` | `ImageCaptureActivity`, `CaptureImageContract` | `core:ui` |
| `application/` | Application class and start-up | `app` |
| `root/` | `Root` and `Host` (the `NavDisplay`; features register their screens) | `app` |
| `di/` | app-wide Koin modules and `FeatureModules.kt` (the list of feature modules) | `app` |
| `appfunctions/` | agent AppFunctions (debug and dev builds, `src/debug`) | `app` |
| `config/` | generated `BuildConfig` (endpoints, keys, version) | `core:config` |
| `models/` `FeatureFlags` | build-type feature switches | `core:config` |
| `resources/` | generated `R` for strings, drawables, fonts | `core:resources` |
| `utilities/`, `utils/` | device, GPS, image and validation helpers | `core:common` (`TimeProvider` in `core:data`, `PreviewDependencies` in `core:designsystem`) |
| `preferences/` | `Preferences` and keys | `core:preferences` |
| `database/` | Room `AppDatabase`, entities, DAOs, migrations | `core:database` |
| `analytics/` | analytics events and exception logging | `core:analytics` |
| `api/` | S3 object storage client and upload request types | `core:network` |
| `models/messages/network/` | messaging API (Retrofit) | `core:network` |
| `navigation/` | routes (`Route`), `Navigator` and navigation results, deep links | `core:navigation` |
| `navigation/` | capture and setup flow controllers | `feature:capture` |
| `viewmodel/` | `BaseViewModel`, actions, UI events, snackbars | `core:navigation` |
| `models/` | plain types (`User`, org data, messages, locations) | `core:model` |
| `models/` | repositories, uploaders, sensors, location, session tracking | `core:data` |
| `models/setupflow/`, `models/captureflowdata/`, `models/TreeCapturer` | capture setup and capture flow state | `feature:capture` |
| `usecases/` | use cases | `core:data` |
| `background/` | sync worker and notifications | `core:data` |
| `theme/`, `view/` | theme and shared Compose components | `core:designsystem` |
| `userselect/` (`UserSelect`, `UserSelectViewModel`), `camera/` | shared user picker, camera, selfie and image review screens | `core:ui` |
| `ui/` | `uiModule`: Koin registrations for `core:ui` | `core:ui` |
| `view/LanguageButton`, `languagepicker/LanguagePickerViewModel` | language button | `core:ui` |
| `splash/`, `languagepicker/` (`LanguageSelectScreen`), `signup/`, `onboarding/` | onboarding | `feature:onboarding` |
| `dashboard/` | the home screen | `feature:dashboard` |
| `capture/`, `treeheight/`, `sessionnote/`, `walletselect/`, `orgpicker/`, `permissions/`, `userselect/UserSelectScreen` | tree capture and its setup steps | `feature:capture` |
| `treeedit/`, `map/`, `trees/` | tree list, tree editing, map | `feature:trees` |
| `settings/`, `profile/` | settings and profiles | `feature:settings` |
| `messages/` | messages | `feature:messages` |
| `devoptions/`, `overlay/`, `devtools/` | developer tools (debug builds) | `feature:devtools` |

Each feature's Koin module (ViewModels and screen registrations) sits in one of its packages, for
example `capture/CaptureModule.kt` or `trees/TreesModule.kt`.

A few non-UI classes from screen packages live in `core:data` so several features can use them:
`dashboard/TreesToSyncHelper`, `devoptions/Configurator` and `DevPrefKeys`, and the overlay
trackers (`DebugOverlayManager`, `SensorDiagnosticsTracker`, `SyncProgressTracker`).
