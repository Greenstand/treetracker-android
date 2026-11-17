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
- `e2e/flows/sad-double-tap-setup.yaml` is no longer tagged `known-bug`, so the e2e run checks it.

## Out of scope

The flow header used to also mention double-tapping Track on small screens (320x640), where the
second tap lands on user select's add-user button. CI runs a normal-sized emulator, and that's a
different screen's first tap, which the origin throttle allows by design. Not changed here.
