# Task 13: Add Roborazzi Screenshot Testing

## Summary

Added Roborazzi screenshot testing infrastructure to the project, refactored 18 screens to Root/UI split pattern, and created screenshot tests for 18 screens (40 test variants total).

## Changes

### Gradle Setup
- Added Roborazzi 1.8.0-alpha-5 dependencies to `gradle/libs.versions.toml`
- Added Roborazzi Gradle plugin classpath to root `build.gradle`
- Added plugin, `testOptions.unitTests.includeAndroidResources`, and test dependencies to `app/build.gradle`
- Added `afterEvaluate` sync tasks to copy golden images between `src/test/snapshots/` (version-controlled) and `build/outputs/roborazzi/` (Roborazzi working directory)

### Test Infrastructure
- Created `ScreenshotTest` base class that absorbs all boilerplate (Robolectric runner, native graphics mode, screen size config, compose rule, theme wrapping)
- Tests extend `ScreenshotTest` and use `snapshot { MyComposable() }` for zero-boilerplate screenshot capture
- Uses `@Config(application = android.app.Application::class)` to prevent Koin DI conflicts
- Uses `LocalInspectionMode provides true` to skip ViewModel creation in nested composables

### Screen Refactoring (Root/UI Split Pattern)
Refactored 18 screens to separate ViewModel-aware root composable from pure stateless UI composable:

| Screen | Root | Pure UI |
|--------|------|---------|
| Dashboard | `DashboardScreen` | `Dashboard` |
| Settings | `SettingsScreen` | `Settings` |
| LanguageSelect | `LanguageSelectScreen` | `LanguageSelect` |
| OrgPicker | `OrgPickerScreen` | `OrgPicker` |
| AddOrg | `AddOrgScreen` | `AddOrg` |
| AddWallet | `AddWalletScreen` | `AddWallet` |
| SessionNote | `SessionNoteScreen` | `SessionNote` |
| Announcement | `AnnouncementScreen` | `Announcement` |
| Chat | `ChatScreen` | `Chat` |
| Survey | `SurveyScreen` | `Survey` |
| IndividualMessageList | `IndividualMessageListScreen` | `IndividualMessageList` |
| WalletSelect | `WalletSelectScreen` | `WalletSelect` |
| TreeImageReview | `TreeImageReviewScreen` | `TreeImageReview` |
| Splash | `SplashScreen` | `Splash` |
| SignUp | `SignUpScreen` | `CredentialEntryView`/`NameEntryView` |
| Profile | `ProfileScreen` | `Profile` |
| ImageReview | `ImageReviewScreen` | `ImageReview` |
| DevOptions | `DevOptionsRoot` | `DevOptionsScreen` (already split) |

- Deleted `DashboardPreviewParameter.kt` (Koin DI anti-pattern)

### Screenshot Tests (18 test classes, 40 test variants)
- `DashboardScreenshotTest` (3 variants)
- `SettingsScreenshotTest` (1 variant)
- `LanguageSelectScreenshotTest` (2 variants)
- `OrgPickerScreenshotTest` (2 variants)
- `AddOrgScreenshotTest` (3 variants)
- `AddWalletScreenshotTest` (2 variants)
- `SessionNoteScreenshotTest` (2 variants)
- `AnnouncementScreenshotTest` (3 variants)
- `ChatScreenshotTest` (3 variants)
- `SurveyScreenshotTest` (3 variants)
- `IndividualMessageListScreenshotTest` (3 variants)
- `WalletSelectScreenshotTest` (1 variant)
- `TreeImageReviewScreenshotTest` (1 variant)
- `SplashScreenshotTest` (1 variant)
- `SignUpScreenshotTest` (5 variants)
- `ProfileScreenshotTest` (3 variants)
- `ImageReviewScreenshotTest` (1 variant)
- `DevOptionsScreenshotTest` (1 variant)

Golden images stored in `app/src/test/snapshots/` (~2.7MB total)

### CI Integration
- Added `verifyRoborazziDebug` step to `.github/workflows/pull_request.yml` and `.github/workflows/actions/run-tests/action.yml`
- Screenshot diffs uploaded as artifacts on failure

### Screens Not Tested (require hardware/mocking)
- MapScreen (uses AndroidView with MapLibre)
- TreeCaptureScreen (camera hardware)
- SelfieScreen (camera hardware)
- UserSelectScreen, MessagesUserSelectScreen, ProfileSelectScreen (thin wrappers)
- DeleteProfileScreen (dialog-only)

## Commands
- Record: `./gradlew recordRoborazziDebug`
- Verify: `./gradlew verifyRoborazziDebug`
- Compare: `./gradlew compareRoborazziDebug`
