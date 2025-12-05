# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Treetracker Android is a field data collection app for the Greenstand open source project. The app captures tree growth data in the field, establishing employment for people in extreme poverty through tree planting. Data flows through a pipeline to a verification service.

## Prerequisites

**Required:** Request the `treetracker.keys.properties` file from the #android_chat Slack channel before building. This file contains AWS S3 credentials, API client IDs, and secrets for all build variants.

## Build Variants

Five build variants exist, each with different API endpoints and configurations:

- **dev**: Development variant with relaxed tree data accuracy requirements. Use this for local development.
- **debug**: Standard debug build with test environment settings and blur detection enabled.
- **beta**: Testing variant pointing to test environment. Used for internal releases to testers.
- **prerelease**: Pre-production build using production environment.
- **release**: Production build with minification enabled and production API endpoints.

## Common Commands

### Building
```bash
# Build dev variant (recommended for development)
./gradlew assembleDev

# Build other variants
./gradlew assembleDebug
./gradlew assembleBeta
./gradlew assemblePrerelease
./gradlew assembleRelease
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run tests for specific variant
./gradlew testDevUnitTest
./gradlew testDebugUnitTest

# Run instrumented tests on connected device
./gradlew connectedDebugAndroidTest
```

### Code Quality
```bash
# Run all checks (includes ktlint, spotless, detekt)
./gradlew check

# Run ktlint only
./gradlew ktlintCheck

# Format code with ktlint
./gradlew ktlintFormat

# Run detekt static analysis
./gradlew detekt

# Apply spotless formatting
./gradlew spotlessApply
```

### Deployment (Fastlane)
```bash
# Setup fastlane
bundle install --path vendor/bundle
fastlane install_plugins
firebase login
```

## Architecture

### Pattern
The app follows **MVVM (Model-View-ViewModel)** architecture:

- **View**: Jetpack Compose UI with reusable components in `view/` package
- **ViewModel**: Per-screen ViewModels manage UI state and business logic
- **Model**: Room database + Retrofit API + Repositories as single source of truth

### Dependency Injection
Koin is used for dependency injection with three main modules in `di/`:

- **AppModule.kt**: ViewModels and application-level dependencies
- **RoomModule.kt**: Database and DAOs
- **NetworkModule.kt**: Retrofit, OkHttp, AWS S3 SDK

### Key Technologies
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **DI**: Koin
- **Networking**: Retrofit + OkHttp
- **Storage**: AWS S3 (images), Room (local database)
- **Async**: Kotlin Coroutines
- **Camera**: CameraX
- **Testing**: JUnit, MockK, Robolectric, Turbine
- **Analytics**: Firebase Analytics + Crashlytics

## Navigation Flows

### Capture Setup Flow
Managed by `CaptureSetupNavigationController`. Dynamic flow defined by the user's organization:

1. **Dashboard** → **UserSelect** (user selection or creation)
2. Organization-specific setup screens (varies by org)
3. **TreeCapture** (final destination)

When setup completes, step counter, session tracker, and GPS updates are started before entering tree capture.

### Tree Capture Flow
Managed by `CaptureFlowNavigationController`. Dynamic flow configured per organization that can include:

- **TreeCapture**: Main capture screen
- **TreeHeightScreen**: Height measurement (if enabled)
- **TreeImageReview**: Review captured photos
- **SessionNote**: Add notes to capture session
- **TreeDBH**: Diameter at breast height (if enabled)

### Messaging Flow
- **MessagesUserSelect** → **IndividualMessageList** → **Chat**/**Announcement**/**Survey**

## Data Sync Architecture

Background sync is handled by `TreeSyncWorker` (a `CoroutineWorker`) that runs `SyncDataUseCase`. The sync process executes in this order:

1. **Sync Messages**: `MessagesRepo.syncMessages()`
2. **Upload Device Config**: `DeviceConfigUploader.upload()`
3. **Upload User Data**: `PlanterUploader.upload()` (includes user images)
4. **Upload Session Data**: `SessionUploader.upload()`
5. **Upload Trees**: `TreeUploader.uploadLegacyTrees()` and `TreeUploader.uploadTrees()`
6. **Upload Location Data**: `UploadLocationDataUseCase.execute()`

### Upload Pattern
All uploaders follow this pattern:
1. Fetch data from Room database
2. Upload images to AWS S3 using `UploadImageUseCase` (if applicable)
3. Update local data with image URLs
4. Create JSON bundle with upload data
5. Upload bundle to AWS S3 via `ObjectStorageClient`
6. Update local data with bundle ID and mark as uploaded
7. Delete local image files (if applicable)

## Package Structure

Main packages under `org.greenstand.android.TreeTracker`:

- `activities/`: Android activities
- `analytics/`: Analytics and exception logging
- `api/`: Retrofit API interfaces and object storage clients
- `application/`: Application class and initialization
- `background/`: Background workers (sync, notifications)
- `camera/`: Camera capture and image review screens
- `capture/`: Tree capture screens and logic
- `dashboard/`: Main dashboard
- `database/`: Room entities, DAOs, migrations
- `di/`: Koin dependency injection modules
- `messages/`: Messaging features (chat, announcements, surveys)
- `models/`: Data models, repositories, business logic
- `navigation/`: Navigation controllers for app flows
- `signup/`: User registration
- `userselect/`: User selection screen
- `usecases/`: Use case classes for business operations
- `view/`: Reusable Compose UI components

## Testing

Tests are located in `app/src/test/` with structure mirroring main source. Key test utilities:

- **MainCoroutineRule.kt**: Custom rule for testing coroutines
- **FakeFileGenerator.kt**: Generate test files for upload testing
- Unit tests use MockK for mocking, Turbine for Flow testing

Run tests for a single class:
```bash
./gradlew test --tests "org.greenstand.android.TreeTracker.capture.TreeCaptureViewModelTest"
```

## Screen Flows

The app has several main screen flows, which are controlled by `CaptureSetupNavigationController` and `CaptureFlowNavigationController`.

**1. Onboarding and Signup:**

*   The app starts with the `SplashScreen`.
*   From the splash screen, the user might be taken to the `Dashboard`, `Language` selection, or the `SignupFlow`.
*   The `SignupFlow` is used for new user registration.

**2. Capture Setup Flow:**

*   This flow is managed by `CaptureSetupNavigationController`.
*   It starts from the `Dashboard` and navigates to `UserSelect`.
*   If a new user is created, it follows a dynamic path defined by the user's organization to set up the capture process.
*   This flow ends by navigating to the `TreeCapture` screen.

**3. Tree Capture Flow:**

*   This flow is managed by `CaptureFlowNavigationController`.
*   It starts on the `TreeCapture` screen.
*   The flow for capturing tree data is dynamic and defined by the user's organization. It can include screens like `TreeHeightScreen`, `TreeImageReview`, and `SessionNote`.
*   After the capture is complete, the tree data is saved, and the user is returned to the `TreeCapture` screen to capture another tree.
*   The user can navigate back to the `Dashboard` from this flow.

**4. Messaging Flow:**

*   This flow allows users to communicate with each other.
*   It starts from `MessagesUserSelect`, then goes to `IndividualMessageList`.
*   From the message list, the user can navigate to a `Chat` screen, an `Announcement` screen, or a `Survey` screen.

## Upload Business Logic

The data upload process is managed by the `TreeSyncWorker`, a `CoroutineWorker` that runs in the background. The worker triggers the `SyncDataUseCase`, which orchestrates the entire upload process.

The upload process is as follows:

1.  **Sync Messages:** `MessagesRepo.syncMessages()` is called to sync messages.
2.  **Upload Device Config:** `DeviceConfigUploader.upload()` is called to upload the device configuration.
3.  **Upload User Data:** `PlanterUploader.upload()` is called to upload user data, including user images.
4.  **Upload Session Data:** `SessionUploader.upload()` is called to upload session data.
5.  **Upload Trees:** `TreeUploader.uploadLegacyTrees()` and `TreeUploader.uploadTrees()` are called to upload legacy and new trees, respectively.
6.  **Upload Location Data:** `UploadLocationDataUseCase.execute()` is called to upload location data.

**General Upload Pattern:**

The uploaders (`TreeUploader`, `PlanterUploader`, `SessionUploader`) follow a similar pattern:

1.  **Fetch Data:** Get the data to be uploaded from the local Room database.
2.  **Upload Images (if applicable):** Upload images to AWS S3 using the `UploadImageUseCase` and get the image URL.
3.  **Update Local Data:** Update the local data with the image URL.
4.  **Create JSON Bundle:** Create a JSON bundle containing the data to be uploaded.
5.  **Upload Bundle:** Upload the JSON bundle to AWS S3 using the `ObjectStorageClient`.
6.  **Update Local Data:** Update the local data with the bundle ID and mark it as uploaded.
7.  **Delete Local Images (if applicable):** Delete the local image files that have been uploaded.

