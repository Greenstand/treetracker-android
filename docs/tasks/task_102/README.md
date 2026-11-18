# Task 102 - Double-tapping Next on user select skips Add org (#1423)

## Bug

Double-tapping Next on user select opened Add org, then capture on top of it, and started a
session. Both taps ran `CaptureSetupNavigationController.navForward`. The first advanced the setup
index to its last step (Add org) and navigated. The second saw the index at the end and took the
"setup complete" branch: it enabled sensors, started a session, closed the setup scope and
navigated to capture. The origin throttle in `Navigator` only gates the final `throttledNavigate`,
not the side effects before it, and the second call could reach it before the first navigation
landed.

## Fix

- `Navigator.canNavigate`: read-only view of the existing throttle check, so a caller can tell
  whether a throttled navigation from it would go through.
- `CaptureSetupNavigationController.navForward` returns early when an earlier `navForward` is still
  running or when its navigator's screen is no longer on top. The body moved to a private
  `advance`, which `navFromNewUserCreation` calls directly, so that path is unchanged.

## Tests

- `CaptureSetupNavigationControllerTest`: a second `navForward` after the first navigated is
  dropped; a `navForward` while one is still running (session start suspended) is dropped; a
  `navForward` from a screen not on top does nothing.
- `NavigatorTest`: `canNavigate` follows whether the origin is on top.
- `DoubleTapGuardTest`: the cross-screen second-tap guard.
- `e2e/flows/sad-double-tap-setup.yaml` is no longer tagged `known-bug`, so the e2e run checks it.

## Cross-screen second taps (found on CI)

CI's emulator is 320x640. There the next screen's button sits where the tapped one was, so the
second tap of a double tap presses a button on the new screen. That's a first tap from a new
screen, so the origin throttle lets it through:

- Track -> user select's add-user button opened signup.
- Next on user select -> Add org's Next finished setup and opened capture. This is what the flow
  actually hit; the same-screen `navForward` guard above alone did not fix it.

`navigation/DoubleTapGuard.kt` handles both. Its `modifier` goes on the screen root and records the
event time of each finger down; `isSecondTapOfDoubleTap(navigator)` is true when that press went
down within the system double-tap timeout (`ViewConfiguration.getDoubleTapTimeout()`, 300 ms) of
the last navigation, or before it. `Navigator.millisSinceLastNavigation` (shared by every scoped
view of the navigator) gives the navigation time. `UserSelect` checks it before add-user, and
`AddOrgScreen` before Next. Tested in `DoubleTapGuardTest` and `NavigatorTest`.

Two things it had to get right, both found on CI:

- Count from the last navigation, not from when the screen was composed. Otherwise a screen that
  was not just navigated to (the UI tests render it and click at once) drops the tap.
- Use the press's event time, not when the click is handled. On a busy emulator Test Buddy's
  double tap took over 500 ms to arrive; its event times still say "double tap", but by wall
  clock the second tap came more than 300 ms after the navigation and got through.

## Self-review

A review of this task's own commits (not #1424's recorder commits) found no high-severity bugs. Fixed:

- Add org's keyboard Go could reuse an old press: a tap on the org field within 300 ms of arriving
  made a later Go look like a second tap. Each check now uses up the recorded press.
- Tests: renamed a test whose name contradicted its assertion; added tests for the used-up press,
  a late-arriving second tap, and `navForward` running again after an earlier call threw.

Left as is:

- Wallet select, Add wallet and Session note have the same bottom-right Next but no guard. Orgs with
  those steps in their setup flow could hit the same cross-screen double tap. The default flow is
  user select -> Add org, and those screens are outside #1423; worth a follow-up issue.
- A double tap on Add org's back arrow decrements the setup index twice (`navBackward`). That
  predates this change and is #1422's area.
- `advance` starts the session before a navigation that system back could still beat. That
  predates this change.
- The flow's `assertNotVisible: {id: CaptureButton}` adds little, but the issue asks not to change
  the flow's assertions.
