# Task 111 - Double-tap guard drops a quick first tap after signup (#1450)

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

- `Navigator.recordPress` keeps the event uptime of the latest finger down, and each stack change
  remembers it (`pressBeforeLastNavigation`). The navigators an entry gets share this state with
  `scopedTo`.
- `Modifier.recordPressesFor(navigator)` on the root `Box` in `Host.kt` records every finger down
  in the app's navigation host once, in the Initial pass, without consuming it. This is the least
  invasive place: one line covers every screen, whether or not it has a guard. The alternative was
  hooking `MainActivity.dispatchTouchEvent`, which has no navigator and sees non-Compose touches.
- `DoubleTapGuard.isSecondTapOfDoubleTap` judges a press against that tap when it is known: a
  double tap's second tap goes down within the double-tap timeout of its first tap. When no tap is
  known (tests, other hosts), it keeps the old rule (time since the navigation).

## Tests

- `DoubleTapGuardTest`: new cases for a quick first tap after a navigation no recent tap caused
  (allowed), a real double tap with the first tap known (dropped), a double tap whose navigation
  waited on async work (dropped), and a keyboard action after such a navigation (allowed). All
  existing cases are unchanged.
- e2e: B6 (`capture-new-user.yaml`) and the H8 double-tap flows run in "Android end-to-end". B6 was
  intermittent, so one green run doesn't prove the fix; the unit tests do.
