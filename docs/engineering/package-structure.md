# Package Structure

All code is under `org.greenstand.android.TreeTracker`. Packages didn't change when the app was
split into modules ([Modules](modules.md)), so a package can tell you what something is, and this
table tells you which module it's in.

| Package | What it holds | Module |
| --- | --- | --- |
| `activities/` | `TreeTrackerActivity` (main) | `app` |
| `activities/` | `ImageCaptureActivity`, `CaptureImageContract` | `core:ui` |
| `application/` | Application class and start-up | `app` |
| `root/` | `Root` and `Host` (the `NavDisplay` for every screen) | `app` |
| `di/` | Koin modules, including feature ViewModels | `app` |
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
| `navigation/` | routes, `Navigator`, deep links | `core:navigation` |
| `navigation/` | capture and setup flow controllers | `core:data` |
| `viewmodel/` | `BaseViewModel`, actions, UI events, snackbars | `core:navigation` |
| `models/` | plain types (`User`, org data, messages, locations) | `core:model` |
| `models/` | repositories, uploaders, sensors, location, flows | `core:data` |
| `usecases/` | use cases | `core:data` |
| `background/` | sync worker and notifications | `core:data` |
| `theme/`, `view/` | theme and shared Compose components | `core:designsystem` |
| `userselect/`, `camera/`, `permissions/` | shared user picker, camera and permission prompt | `core:ui` |
| `view/LanguageButton`, `languagepicker/LanguagePickerViewModel` | language button | `core:ui` |
| `capture/`, `dashboard/`, `devoptions/`, `languagepicker/`, `map/`, `messages/`, `orgpicker/`, `overlay/`, `profile/`, `sessionnote/`, `settings/`, `signup/`, `splash/`, `treeedit/`, `treeheight/`, `walletselect/` | screens | `feature:<package>` |

A few non-UI classes from screen packages live in `core:data` so several features can use them:
`dashboard/TreesToSyncHelper`, `devoptions/Configurator` and `DevPrefKeys`, and the overlay
trackers (`DebugOverlayManager`, `SensorDiagnosticsTracker`, `SyncProgressTracker`).
