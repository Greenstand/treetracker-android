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
- `DoubleTapGuardTest`: the add-user tap guard's timing.
- `e2e/flows/sad-double-tap-setup.yaml` is no longer tagged `known-bug`, so the e2e run checks it.

## Track double tap on small screens

The flow also double-taps Track, and CI's emulator is 320x640. There the second tap lands on user
select's add-user button, which sits where Track was, and opens signup. That's a first tap from
a new screen, so the origin throttle lets it through. `Navigator.millisSinceLastNavigation` (shared by
every scoped view of the navigator) says how long ago the stack last changed, and `UserSelect`
ignores an add-user tap that comes within the system double-tap timeout
(`ViewConfiguration.getDoubleTapTimeout()`, 300 ms) of that (`isSecondTapOfDoubleTap`, tested in
`DoubleTapGuardTest` and `NavigatorTest`). It counts from the last navigation, not from when the
screen was composed, so a user select that was not just navigated to (as in the UI tests) is not
affected. Only the add-user button is guarded; picking a user and Next are unchanged.
