# Task 24 - Migrate from Navigation 2 to Navigation 3

## Goal

Replace Jetpack Navigation 2 (`androidx.navigation:navigation-compose`) with
Navigation 3 (`androidx.navigation3`) across the whole app, preserving every
existing navigation behavior.

## Changes

- Bumped Compose 1.7.5 → 1.10.5 and compileSdk 35 → 36 (required by
  navigation3-ui 1.1.1); `material-icons-extended` pinned to 1.7.8 (its last
  published version). Landed as an isolated first commit.
- All 29 routes in `navigation/Routes.kt` now implement `NavKey` (still
  `@Serializable`).
- New `navigation/Navigator.kt`: app-owned back stack operations replacing
  `NavHostController` — `navigate` with a `NavOptions` DSL (`popUpTo<T>`,
  `popUpToRoot()` replacing `popUpTo(graph.id)`, `launchSingleTop`),
  `popBackStack`, `popBackStackTo<T>(inclusive)`, and throttled variants
  keeping the 300 ms debounce from the deleted `utilities/NavigationUtils.kt`.
  Exposed via `LocalNavigator` (replacing `LocalNavHostController`).
- Nav2's "only navigate while RESUMED" guard is replaced by the debounce plus
  an origin-is-top check in `HandleUIEvents`: ViewModel navigation events only
  execute when the emitting entry is the top of the back stack (via
  `LocalNavEntryContentKey`).
- `root/Host.kt`: `NavHost` → `NavDisplay` with `rememberNavBackStack`,
  saveable-state + ViewModel-store decorators (per-entry ViewModel scoping),
  and explicit 500 ms fade transition specs matching the previous look.
- `trackedComposable` replaced by a custom
  `rememberScreenTrackingNavEntryDecorator()` (`NavEntryDecorator`) that keeps
  Crashlytics screen breadcrumbs and provides `LocalNavEntryContentKey`.
- Deep link (`app://mobile.treetracker.org/org?...`) reimplemented manually
  (Nav3 has no deep link support): `navigation/OrgDeepLink.kt` parses the
  launch intent in `TreeTrackerActivity` into the `SplashRoute` start key.
  Cold-start-only, matching pre-migration behavior.
- `ImageCaptureActivity`'s standalone selfie NavHost migrated to its own
  `NavDisplay`.
- `CaptureFlowNavigationController` / `CaptureSetupNavigationController` and
  ~25 screens switched from `NavHostController` to `Navigator`; route
  resolution (`RouteRegistry`, `FlowNavigationController`) tightened from
  `Any` to `NavKey`.
- `NavigationEvent` now carries a `suspend Navigator.() -> Unit`; existing
  ViewModel tests pass unchanged. New `NavigatorTest` covers popUpTo
  (inclusive/exclusive/absent), singleTop replace, popUpToRoot, popBackStackTo,
  root-pop refusal, and the throttle window via a fake clock.
- Removed `androidx-navigation-compose` from the version catalog; zero
  `androidx.navigation.` imports remain.

## Emulator smoke test findings

- `NavDisplay` crashed on launch with "No NavigationEventDispatcher was
  provided": Navigation 3's back handling needs a `NavigationEventDispatcherOwner`.
  Fixed by bumping `androidx.activity` to 1.12.0 (where `ComponentActivity`
  implements the owner) and planting the view-tree owner manually in
  `TreeTrackerActivity` — `AppCompatActivity.setContentView` (appcompat 1.7.x)
  predates navigationevent and only plants the four older view-tree owners.
- Verified on a Pixel 7 API 35 emulator: cold start, splash auto-navigation,
  signup-flow back handling, and deep-link cold start
  (`OrgLink: Deeplink received: orgId=..., orgName=...`) all work; no crashes.

## Behavior notes

- `launchSingleTop` is implemented as replace-top: a match with different
  arguments recreates the entry (unobservable for current call sites).
- A navigation issued 300–500 ms after the previous one (mid-fade) now
  succeeds where the old RESUMED check would drop it.
- Predictive back gesture previews now animate with the same 500 ms fade.

## Verification

- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug :app:ktlintCheck :app:detekt :app:lintDebug` all pass.
- Manual QA checklist: fresh-install signup flow (incl. selfie capture
  activity), existing-user splash → dashboard, capture loop (no stack growth),
  settings logout / delete-profile stack clears, deep link cold start via
  `adb shell am start -a android.intent.action.VIEW -d "app://mobile.treetracker.org/org?id=X&name=Y"`,
  rotation + process-death restore on a deep screen, double-tap nav buttons.
