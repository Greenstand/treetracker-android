# Architecture

## Pattern

The app follows MVVM (Model-View-ViewModel) architecture:

- View: Jetpack Compose UI with reusable components in `view/` package
- ViewModel: Per-screen ViewModels manage UI state and business logic
- Model: Room database + Retrofit API + Repositories as single source of truth

## Dependency Injection

Koin is used for dependency injection with three main modules in `di/`:

- `AppModule.kt`: ViewModels and application-level dependencies
- `RoomModule.kt`: Database and DAOs
- `NetworkModule.kt`: Retrofit, OkHttp, AWS S3 SDK

## Key Technologies

- Language: Kotlin
- UI: Jetpack Compose
- DI: Koin
- Networking: Retrofit + OkHttp
- Storage: AWS S3 (images), Room (local database)
- Async: Kotlin Coroutines
- Camera: CameraX
- Testing: JUnit, MockK, Robolectric, Turbine
- Analytics: Firebase Analytics + Crashlytics
