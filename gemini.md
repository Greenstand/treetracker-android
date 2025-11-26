# Gemini Project: treetracker-android

## Project Summary

This is the Android application for the Greenstand Treetracker open source project. The app is used to capture tree growth data in the field, which helps to establish employment for people in extreme poverty through tree planting. The data collected by the app is sent through a data pipeline for verification.

## Key Technologies

*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose
*   **Architecture:** MVVM (Model-View-ViewModel) is likely, given the use of `ViewModel` and `LiveData`.
*   **Dependency Injection:** Koin
*   **Networking:** Retrofit, OkHttp, AWS S3 SDK
*   **Database:** Room
*   **Asynchronous Programming:** Kotlin Coroutines
*   **Image Handling:** CameraX
*   **Testing:** JUnit, MockK, Robolectric, Turbine
*   **Analytics:** Firebase Analytics, Firebase Crashlytics

## Project Structure

The project is a standard Android application with a single `app` module.

*   `app/src/main/java/org/greenstand/android/TreeTracker`: The main source code for the application.
    *   `activities`: Contains the main Android activities.
    *   `analytics`: Code for analytics and exception logging.
    *   `api`: Code for interacting with the Treetracker API and object storage.
    *   `application`: The main `Application` class.
    *   `background`: Background tasks, such as data synchronization.
    *   `camera`: Camera and image review screens.
    *   `capture`: Screens related to tree capture.
    *   `dashboard`: The main dashboard screen.
    *   `database`: Room database entities, DAOs, and migrations.
    *   `di`: Dependency injection modules for Koin.
    *   `models`: Data models used throughout the application.
    *   `navigation`: Navigation controllers.
    *   `view`: Reusable UI components.
    *   `viewmodels`: ViewModels for the different screens.
*   `app/src/main/res`: Android resources, such as layouts, drawables, and strings.
*   `app/src/test`: Unit tests.
*   `build.gradle`: The top-level Gradle build file.
*   `app/build.gradle`: The app-level Gradle build file, containing dependencies and build configurations.

## Build Variants

The application has several build variants:

*   `dev`: For development, with less strict requirements for tree data.
*   `debug`: For debugging.
*   `beta`: For testing.
*   `prerelease`: A pre-release version.
*   `release`: The production version.

Each build variant has its own configuration for API endpoints and other settings.

## App Architecture

The app follows a Model-View-ViewModel (MVVM) architecture pattern.

*   **View:** The UI is built using Jetpack Compose. The `view` package contains reusable UI components, and screens are defined in their respective feature packages (e.g., `dashboard`, `capture`).
*   **ViewModel:** `ViewModel`s are responsible for preparing and managing data for the UI. They are located in their respective feature packages (e.g., `DashboardViewModel`, `TreeCaptureViewModel`).
*   **Model:** The data layer is composed of a Room database, a remote API accessed via Retrofit, and repositories that provide a single source of truth for the app's data.

**Dependency Injection:**

Dependency injection is managed by Koin. The `di` package contains the Koin modules:

*   `appModule`: Provides ViewModels and other dependencies.
*   `roomModule`: Provides the Room database and DAOs.
*   `networkModule`: Provides network-related dependencies like Retrofit and OkHttp.

## Build Commands

To build the application, you will need to get the `treetracker.keys.properties` file from the team.

**Build Variants:**

You can build any of the defined build variants using the following Gradle tasks:

*   `./gradlew assembleDev`
*   `./gradlew assembleDebug`
*   `./gradlew assembleBeta`
*   `./gradlew assemblePrerelease`
*   `./gradlew assembleRelease`

**Deployment:**

The project uses Fastlane for deployment. The following commands are available:

*   `bundle install --path vendor/bundle`
*   `fastlane install_plugins`
*   `firebase login`

The `README.md` also mentions the following Gradle tasks for publishing to the Play Store:

*   `bootstrapReleasePlayResources`
*   `generateReleasePlayResources`
*   `publishListingRelease`

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
