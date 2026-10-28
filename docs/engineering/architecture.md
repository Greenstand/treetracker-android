# Architecture

## Pattern

The app follows MVVM (Model-View-ViewModel) architecture, split into core and feature modules
(see [Modules](modules.md)):

- View: Jetpack Compose UI with reusable components in `view/` (`core:designsystem`)
- ViewModel: Per-screen ViewModels manage UI state and business logic
- Model: Room database + Retrofit API + Repositories as single source of truth

## ViewModel / UI Layer

### BaseViewModel

All ViewModels extend `BaseViewModel<T : Action>`, which provides:

- **`handleAction(action: T)`** — abstract method that each ViewModel overrides to process UI actions
- **`MutableStateFlow`** — holds the screen's UI state, with a dedicated `updateState` function for safe updates
- **`state`** — a `val` that exposes the current state value directly (shorthand for `_state.value`)

### Action Pattern

Each screen's ViewModel declares a **sealed class** that implements the `Action` interface. This sealed class enumerates every UI interaction the screen supports. The ViewModel's `handleAction` dispatches on this sealed class using a `when` statement:

```kotlin
sealed class DashboardAction : Action {
    object OnSettingsClicked : DashboardAction()
    data class OnPlanterSelected(val id: Long) : DashboardAction()
}

class DashboardViewModel : BaseViewModel<DashboardAction>() {
    override fun handleAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.OnSettingsClicked -> { /* ... */ }
            is DashboardAction.OnPlanterSelected -> { /* ... */ }
        }
    }
}
```

### Composable Convention

Non-root (inner) UI composables accept a single **`onHandleAction: (ActionType) -> Unit`** lambda typed to the screen's action sealed class. This gives the composable a single callback for all user interactions — the composable triggers it with the appropriate action, and the root composable wires it to the ViewModel:

```kotlin
// Root composable — connects ViewModel to UI
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    DashboardContent(state = state, onHandleAction = viewModel::handleAction)
}

// Non-root composable — pure UI with single callback
@Composable
fun DashboardContent(
    state: DashboardState,
    onHandleAction: (DashboardAction) -> Unit,
) {
    Button(onClick = { onHandleAction(DashboardAction.OnSettingsClicked) }) {
        Text("Settings")
    }
}
```

## UI Layer: Root / UI Split

Every screen composable is split into two layers:

| Layer | Naming | Stateful? | Role |
|-------|--------|-----------|------|
| **Root** | `FooScreen` | Yes | Injects ViewModel, observes state, wires navigation |
| **UI** | `Foo` | No | Pure rendering from state + event callbacks |

### Why

Stateless UI composables are easy to preview and test without a ViewModel, DI framework, or navigation graph. Previews can pass fake state directly. Snapshot and Compose UI tests render the UI composable with controlled inputs and assert on the output without mocking infrastructure.

### Root composable (stateful)

The root composable is the screen a feature registers for its route (see [Modules](modules.md)). It:

1. Resolves the ViewModel with `koinViewModel()`
2. Collects state from the ViewModel (`observeAsState` for LiveData, `collectAsState` for Flow)
3. Reads `LocalNavigator` for navigation
4. Delegates all rendering to the stateless UI composable, mapping ViewModel calls and navigation actions into lambda callbacks

```kotlin
// DashboardScreen.kt — root (stateful)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current

    Dashboard(
        state = state,
        onSyncClicked = { viewModel.sync() },
        onCaptureClicked = { navigator.navigate(UserSelectRoute) },
        // ...
    )
}
```

### UI composable (stateless)

The UI composable accepts all data as parameters and communicates user actions through `() -> Unit` callbacks. It has no direct references to ViewModels, navigation controllers, or CompositionLocals (other than theme).

```kotlin
// DashboardScreen.kt — UI (stateless)
@Composable
fun Dashboard(
    state: DashboardState,
    onSyncClicked: () -> Unit = { },
    onCaptureClicked: () -> Unit = { },
    // ...
) {
    Scaffold { /* render from state */ }
}
```

### Previews

Previews target the stateless UI composable with hardcoded state. `PreviewDependencies` (in `utils/`, module `core:designsystem`) provides the minimal `CompositionLocalProvider` wrapper (nav controller, theme) needed for previews that use shared components like `ActionBar` or `LanguageButton`.

```kotlin
@Preview
@Composable
fun DashboardPreview() {
    PreviewDependencies {
        Dashboard(
            state = DashboardState(treesRemainingToSync = 51, treesSynced = 146),
        )
    }
}
```

### Snapshot and Compose UI testing

Roborazzi screenshot tests and Robolectric Compose UI tests both render the stateless UI composable directly, passing controlled state and asserting on the rendered output. This avoids the need to mock ViewModels, Koin, or the navigation graph in the majority of tests.

```kotlin
// Snapshot test
composeTestRule.setContent {
    PreviewDependencies {
        Dashboard(state = DashboardState(treesSynced = 42))
    }
}

// Compose UI test
composeTestRule.onNodeWithText("42").assertIsDisplayed()
```

### Applying the pattern to new screens

When adding a new screen:

1. Create the stateless UI composable first (`Foo`), accepting a state data class and event callbacks
2. Build the `@Preview` against that composable
3. Create the root composable (`FooScreen`) that wires the ViewModel and navigation
4. Write snapshot and UI tests against the stateless composable

When refactoring an existing screen that doesn't yet follow this split, extract the UI body into a separate composable with the same name minus the `Screen` suffix.

## Dependency Injection

Koin (4.2) is used for dependency injection. Each feature module has its own Koin module with its
ViewModels and the screens for its routes (`navigation<SomeRoute> { ... }`, Koin's Navigation 3
support); `core:ui` has `uiModule` for the shared screens. The app shell (`app`, package `di/`)
starts Koin with:

- `AppModule.kt`: application-level dependencies (repositories, uploaders, sensors, use cases)
- `RoomModule.kt`: Database and DAOs
- `NetworkModule.kt`: Retrofit, OkHttp, AWS S3 SDK
- `FeatureModules.kt`: the list of every feature's Koin module

`Host` draws screens with `koinEntryProvider()`, so it never lists them. How features open each
other's screens and hand results back is in [Modules](modules.md#how-features-talk-to-each-other).

## Key Technologies

- Language: Kotlin
- UI: Jetpack Compose
- DI: Koin
- Networking: Retrofit + OkHttp
- Storage: AWS S3 (images), Room (local database)
- Async: Kotlin Coroutines
- Camera: CameraX
- Testing: JUnit, MockK, Robolectric, Roborazzi, Turbine
- Analytics: Firebase Analytics + Crashlytics
