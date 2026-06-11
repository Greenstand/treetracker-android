# Task 18 — Unified UI event handling (ShareList pattern)

## Goal

Adopt the ShareList project's UI event handling pattern so that snackbars, navigation,
dialogs and other one-shot UI signals all flow through a single `UIEvent` channel from
ViewModel to Composable, with a centralized `SnackbarController` provided via a
`CompositionLocal`.

## Why

The current code has three different mechanisms for one-shot UI signals:

1. **`NavigationEvent`** — already routed through `BaseViewModel.events` (a `Channel`).
2. **`ConsumableSnackBar` in state** — Dashboard stores a snackbar inside `DashboardState`
   and a `LaunchedEffect(snackBar)` shows it. Snackbar UI is duplicated per screen.
3. **Local `SnackbarHostState` inside a Composable** — signup credentials show errors via
   `remember { SnackbarHostState() }` and ad-hoc `scope.launch { showSnackbar(...) }`.

This is inconsistent and the `Channel` based events have a known cold-start race:
an event emitted from `init` before the collector subscribes is dropped. ShareList
solved this with a `MutableSharedFlow(replay = 5)` plus a `ConsumableEvent` wrapper
that fires at most once.

## Pattern

Adapted from `composeApp/src/commonMain/kotlin/com/oneguygames/sharelist/ui/base/`:

```
ViewModel side                       Composable side
─────────────────                    ─────────────────
sendEvent(ShowSnackbar(...))         HandleUIEvents(viewModel)            ← screen root
sendEvent(NavigationEvent { ... })        ↓
sendEvent(PopBackStackEvent)         routes Navigate / PopBackStack / ShowSnackbar
                                          ↓
                                     LocalSnackbarController.current      ← provided at Root
                                          ↓
                                     SnackbarHost rendered once at Root
```

Key shape:

- **`UiEvent`** — sealed marker interface. Built-in subtypes: `NavigationEvent`,
  `PopBackStackEvent`, `ShowSnackbar`. Features may add their own subtypes and intercept
  them via the optional `onEvent` parameter of `HandleUIEvents`.
- **`TextRef`** — localization-agnostic text reference (`Plain(String)` /
  `Res(@StringRes Int, vararg args)`). Lets ViewModels emit events without holding a
  `Context`.
- **`BaseViewModel.events`** is now a `SharedFlow<ConsumableEvent<UiEvent>>` backed by
  `MutableSharedFlow(replay = 5, extraBufferCapacity = 16)`. The replay buffer survives
  the gap between `init` and the first `HandleUIEvents` collector; `ConsumableEvent`
  ensures replayed events fire only once.
- **`SnackbarController`** — singleton provided via `LocalSnackbarController`. Owns one
  `SnackbarHostState` for the whole app. `HandleUIEvents` sends `ShowSnackbar` events
  into it.
- **`Root`** hosts the controller and renders the global `CustomSnackbar` once, above
  navigation, so any screen can fire a snackbar without wiring its own host.

## Changes

### Infrastructure (new / changed)

- `app/.../viewmodel/TextRef.kt` (new) — `TextRef` sealed interface + `@Composable` resolver
  + `Context.resolve(TextRef)`.
- `app/.../viewmodel/UiEvents.kt` (refactored) — sealed `UiEvent` interface; built-in
  `NavigationEvent`, `PopBackStackEvent`, `ShowSnackbar`. `ConsumableEvent` keeps the
  atomic-consume semantics.
- `app/.../viewmodel/BaseViewModel.kt` (refactored) — `Channel` replaced with
  `MutableSharedFlow(replay = 5, extraBufferCapacity = 16)`; `triggerEvent` renamed to
  `sendEvent` (alias kept).
- `app/.../viewmodel/SnackbarController.kt` (new) + `LocalSnackbarController.kt` (new) —
  central snackbar bus + CompositionLocal.
- `app/.../viewmodel/HandleUIEvents.kt` (refactored) — collects events and dispatches
  Navigate / NavigateUp / ShowSnackbar; accepts optional `onEvent: (UiEvent) -> Boolean`
  for screen-specific events; resolves the `SnackbarController` from the CompositionLocal.
- `app/.../root/Root.kt` (updated) — provides `LocalSnackbarController` and renders a
  global `CustomSnackbar` host.

### Screen migrations

- `dashboard/DashboardViewModel.kt` — drop `snackBar: ConsumableSnackBar?` from state;
  use `sendEvent(ShowSnackbar(TextRef.Res(R.string.sync_failed)))` etc.
- `dashboard/DashboardScreen.kt` — remove local snackbar host + `LaunchedEffect(snackBar)`;
  add `HandleUIEvents(viewModel)`.
- `treeedit/TreeDetailViewModel.kt` — drop `noteSaved` flag and `NoteSavedShown` action;
  emit `ShowSnackbar(TextRef.Res(R.string.tree_note_saved))` after `dao.updateTree`.
- `treeedit/TreeDetailScreen.kt` — remove `Toast.makeText` + `LaunchedEffect(noteSaved)`;
  add `HandleUIEvents(viewModel)`.
- `messages/survey/SurveyViewModel.kt` — emit
  `ShowSnackbar(TextRef.Res(R.string.survey_completed))` when the last question is
  answered (alongside the existing `surveyComplete` flag that still drives the pop-back).
- `messages/survey/SurveyScreen.kt` — remove the local `ShowToastMessage` Composable and
  the `showToast` state; add `HandleUIEvents(viewModel)`.
- `view/ConsumableSnackBar.kt` — deleted (no remaining callers).

### Out of scope (follow-ups)

- `signup/CredentialEntryView.kt` still uses its own `SnackbarHostState`. Migrating it
  to `SnackbarController` requires lifting the email/phone validation into
  `SignupViewModel`. Tracked separately.
- A `BaseViewModelComponent` analogue for sharing the event bus between extracted
  ViewModel logic (ShareList uses this for FAB autocomplete, item suggestions, etc.).
  Not needed yet — open a follow-up if a feature wants extraction.

## Verification

- `./gradlew :app:assembleDebug` — must compile.
- `./gradlew :app:testDebugUnitTest` — existing unit tests pass.
- `./gradlew :app:lintDebug` — no new lint issues.
- Manual run on a debug emulator:
  1. From Dashboard, tap the upload button with 0 trees pending → snackbar
     "Nothing to sync" appears at the bottom (was state-driven, now event-driven).
  2. Trigger a sync that fails (e.g. airplane mode) → "Sync failed" snackbar.
  3. Tree capture → tap shutter without GPS lock → bad-GPS dialog appears
     (unchanged behaviour; dialog is still state-driven).
  4. Rotate the device while a snackbar is showing → no double-fire.

## References

- ShareList architecture doc:
  `/Users/jonathan/AndroidStudioProjects/ShareList/docs/architecture/Patterns.md` §1–4.
- ShareList event infra:
  `composeApp/src/commonMain/kotlin/com/oneguygames/sharelist/ui/base/{UIEvent,EventBus,EventHandler,SnackbarController}.kt`.
