# Task 118 - Double-tap guard drops a quick first tap after signup (#1450)

## Bug

After adding a new user in capture setup (user select, signup, selfie, Approve), signup opens
Add org. A tap on Add org's Next within ~300 ms of the screen appearing was silently ignored. The
e2e flow B6 (`e2e/flows/capture-new-user.yaml`) failed intermittently on master because tb taps
Next as soon as `OrgNameField` shows.

`DoubleTapGuard` (#1427) dropped any press within the double-tap timeout of the last navigation.
That works when a tap navigates straight away (user select's Next onto Add org's Next, H8). After
signup, though, the navigation follows the camera activity's result and a DB write, so the last
tap in the app's window was the selfie button, long before. No double tap was possible.

## Fix

- `Navigator.recordPress` keeps the event uptime of the latest finger down or release, and each
  stack change remembers it (`pressBeforeLastNavigation`). The navigators an entry gets share this state with
  `scopedTo`.
- `Modifier.recordPressesFor(navigator)` on the root `Box` in `Host.kt` records every finger down
  and its release in the app's navigation host once, in the Initial pass, without consuming it. This is the least
  invasive place: one line covers every screen, whether or not it has a guard. The alternative was
  hooking `MainActivity.dispatchTouchEvent`, which has no navigator and sees non-Compose touches.
- `DoubleTapGuard.isSecondTapOfDoubleTap` judges a press against that tap when it is known: a
  double tap's second tap goes down within the double-tap timeout of the first tap's release
  (as in Android's `GestureDetector`). When the host has
  recorded presses but none before the navigation (no tap led there, e.g. after MainActivity was
  recreated while the camera was open), the press is meant for the screen. When no press was ever
  recorded (tests, other hosts), it keeps the old rule (time since the navigation).

## Tests

- `DoubleTapGuardTest`: new cases for a quick first tap after a navigation no recent tap caused
  (allowed), a real double tap with the first tap known (dropped), a double tap whose navigation
  waited on async work (dropped), and a keyboard action after such a navigation (allowed). All
  existing cases are unchanged.
- `DoubleTapGuardUiTest` (Compose, real touch events through `recordPressesFor` and the guard):
  a quick double tap is dropped, a double tap whose first tap was held is dropped (the timeout
  runs from the release), and a slow second tap goes through.
- e2e: B6 (`capture-new-user.yaml`) and the H8 double-tap flows run in "Android end-to-end". B6 was
  intermittent, so one green run doesn't prove the fix; the unit tests do.

## Review (independent subagent)

- Fixed: the window ran from the first tap's down rather than its up, so a held first tap made a
  real double tap slip through. The recorder now also records the release (test first:
  `DoubleTapGuardUiTest`).
- Fixed (nit): a redundant `millisSinceLastNavigation` check in the guard.
- Not fixed, by design: presses in other windows (dialogs, the camera activity) aren't recorded;
  the dialog buttons that navigate don't sit where the next screen's guarded button is. Multi-touch:
  only a gesture's first pointer is recorded, as in the existing guard.
- Not added: a test through the real `Host` that fails by assertion on master. It needs the signup
  screen, a fake activity result registry and Robolectric's `SystemClock` kept in step with the
  Compose test clock; on master the guard can't know the earlier tap at all. `DoubleTapGuardUiTest`
  covers the recorder and guard together; `Host.kt`'s one-line install is covered by the B6 and H8
  e2e flows.

## How the pieces fit (kept here; the code has no comments)

- `recordPressesFor` listens in the Initial pass, so it sees a finger down and its release before
  the screen's own click handler runs; a click navigates on the release, so the release time is
  already recorded when the navigation snapshots it.
- The double-tap timeout is measured from the first tap's release to the second tap's down, as in
  Android's `GestureDetector`, so a first tap held for a while still counts.
- A press with no event of its own (the keyboard's Go) is judged as happening now.
- `DoubleTapGuardUiTest` injects both taps of each case as one gesture sequence on the root, so
  their event times are exact.

## Review 2 (independent subagent, at 3b55a22c)

- Fixed (test first, 0db55ac): after MainActivity is recreated while the camera is open, the new
  `Navigator` has no press recorded before signup opens Add org, so the guard fell back to the old
  rule and dropped a quick first Next again. Now, once the host has recorded any press, "no press
  before the navigation" means no tap led there (`Navigator.recordsPresses`).
- Not fixed, outside what this PR claims (speculative): taps in other windows aren't recorded. A
  double tap on the keyboard's Go, or on an `AlertDialog` button, whose second tap lands on the
  next screen's guarded button is now judged against the last in-app touch, where master dropped
  it within 300 ms of the navigation. Not seen in practice; the keyboard usually still covers that
  spot while it hides.
- Not fixed, by design: a navigation that takes longer than the timeout after its tap is not
  followed by a "double tap" by Android's definition.
- Not added: a test of `Host.kt`'s one-line install of the recorder (only the B6 and H8 e2e flows
  cover it).
