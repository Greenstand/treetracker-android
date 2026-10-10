# Task 19 — Route navigation through the UiEvent system

Stacked on [task_18](../task_18/README.md) / PR #1295. Branch
`refactor/ui-events-navigation` cuts off `refactor/ui-events-handler`.

## Goal

After task_18, navigation events (`NavigationEvent`, `NavigateUpEvent`) and the
`HandleUIEvents` dispatcher existed but only **one** ViewModel actually used them
(`TreeCaptureViewModel`). Every other screen still navigated in one of three ways:

1. **Action-handler-driven** — `onHandleAction = { action -> when (action) { is NavigateX -> navController.throttledNavigate(...); else -> viewModel.handleAction(action) } }`.
2. **State-flag-driven** — ViewModel sets a Boolean (`isDeleted`, `surveyComplete`,
   `canNavigateForward`), Composable runs `LaunchedEffect(flag) { if (flag) navController.x() }`.
   The flag exists only to trigger the nav — an event masquerading as state.
3. **Composable-callback-driven** — Composables receive `onNavigateBack` / `onSelect`
   lambdas directly; the navigation lives in the root Composable.

This task migrates the screens in groups (1) and (2) where the move is mechanical and
the ViewModel already has an `Action` sealed class. Group (3) is left as a follow-up
because the screens lack an `Action` enum for navigation and adding one bloats the diff.

## Changes

### Infrastructure

- **`viewmodel/UiEvents.kt`** — rename `NavigateUpEvent` → `PopBackStackEvent` (the
  project's idiom is `popBackStack` not `navigateUp`). NavigateUpEvent had no callers,
  so the rename is free.
- **`viewmodel/HandleUIEvents.kt`** — dispatch `PopBackStackEvent` via
  `navController.throttledPopBackStack()` instead of `navigateUp()`.
- **`viewmodel/BaseViewModel.kt`** — new protected helpers:
  ```kotlin
  protected fun navigate(route: Any)
  protected fun navigate(route: Any, builder: NavOptionsBuilder.() -> Unit)
  protected fun popBackStack()
  ```
  All three route through the existing `throttledNavigate` / `throttledPopBackStack`
  extensions (`utilities/NavigationUtils.kt`) so the 300ms debounce still applies.

### State-flag screens — drop the flag, emit the event

| Screen | State field dropped | New behavior |
|---|---|---|
| `messages/survey/SurveyViewModel` | `surveyComplete`, `shouldNavigateBack` | emit `popBackStack()` directly after `saveSurveyAnswers` / at first-question back |
| `treeedit/TreeDetailViewModel` | `isDeleted` | emit `popBackStack()` after `dao.deleteTreeById`; added `NavigateBack` action so back-arrow also routes through the event |
| `capture/TreeImageReviewViewModel` | `canNavigateForward` | emit `NavigationEvent { CaptureFlowScopeManager.nav.navForward(this) }`; back action emits `NavigationEvent { …navBackward(this) }`. Added `HandleUIEvents(viewModel)` to the screen (was missing) |

Screens drop their `LaunchedEffect(state.flag)` blocks and the `LocalNavHostController.current`
binding where it's no longer needed.

### Action-handler screens — move nav into `handleAction`

For each, the screen used to switch on action type and call `navController.x` directly.
Now the screen just calls `viewModel::handleAction` and the ViewModel emits the nav event.

| Screen | Migrated actions |
|---|---|
| `settings/Settings*.kt` | `NavigateToProfile`, `NavigateToEditTrees`, `NavigateToMap`, `NavigateToDeleteAccount`, `NavigateBack`, `LogoutConfirmed` (with `popUpTo(graph.id)` to clear back-stack on logout) |
| `messages/announcementmessage/Announcement*.kt` | `NavigateBack`. `OpenLink` stays in the screen (it's an external `Intent`, not nav — fired via `LocalContext.current`) |
| `devoptions/DevOptionsRoot.kt` + `DevOptionsViewModel.kt` | `NavigateBack` |
| `orgpicker/OrgPicker*.kt` | `NavigateNext` |

### Tests

- `TreeDetailViewModelTest`: `DeleteTree …` tests now assert that `PopBackStackEvent`
  was emitted (replacing the `isDeleted` flag assertion). New `NavigateBack emits pop-back event` test.
- `SurveyViewModelTest`: the `shouldNavigateBack` test now asserts `PopBackStackEvent`.

## Out of scope (next stacks)

- **Dashboard's residual nav** — `DashboardScreen` still has `throttledNavigate(OrgRoute)`,
  `UserSelectRoute`, etc. mixed with a `showDialog` `mutableStateOf` local to the
  Composable. Lifting that requires deciding whether the dialog state lives in the
  ViewModel — separate PR.
- **SignUp + Splash** — both run `scope.launch { repo.x(); navController.navigate(...) }`
  inside the Composable, and Splash has permission-callback-driven nav. Migrating means
  lifting the repo calls into the ViewModel.
- **CaptureFlow / CaptureSetup scope-managed chain** — TreeCapture's `BackHandler`, the
  remaining `CaptureSetupScopeManager.nav.*` calls from `UserSelectScreen`,
  `SessionNoteScreen`, `WalletSelectScreen`, plus the camera screens (`SelfieScreen`,
  `ImageReviewScreen`). The migration pattern is identical for every screen in the chain
  — a focused PR will read better than mixing them in here.
- **Callback-passing screens** — `ChatScreen`, `IndividualMessageListScreen`,
  `TreeListScreen`, `TreeEditUserSelectScreen`, `MessagesUserSelectScreen`. These don't
  have an `Action` sealed class for navigation (the inner Composables receive `onSelect`
  lambdas directly). Introducing actions is mechanical but spans many files.
- **`CredentialEntryView` snackbars** — carried over from task_18.

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` — clean.
- `./gradlew :app:testDebugUnitTest` — passes (including updated SurveyViewModelTest
  and TreeDetailViewModelTest).
- `./codeAnalysis.sh` — clean.
- Manual on emulator:
  1. **Survey** — open a survey from Messages → answer all questions → screen pops back,
     "Survey completed" snackbar shows.
  2. **Tree detail** — Settings → Edit Trees → pick a user → pick a tree → tap delete →
     confirm → screen pops back.
  3. **Tree image review** — Track flow → tap shutter → review screen → approve →
     forward nav into capture flow; reject → backward nav.
  4. **Settings** — open from dashboard → tap each row → each navigates correctly; back
     arrow returns to dashboard. (Debug build only) Logout → returns to signup, back-stack cleared.
  5. **Announcement** — open an announcement message → back arrow pops back.
  6. **Dev Options** — debug build → from Splash gear → opens → back arrow pops back.
  7. **Org Picker** — splash flow → arrives at org picker → "next" pops back.
  8. Rotate device mid-flow on each of the above — no double-navigation (the
     `ConsumableEvent` guard already prevents replay).
