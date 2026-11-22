# Task 111 - Double tap on Next skips org-configured setup steps (#1442)

## Bug

Every capture setup screen's Next is the bottom bar's right arrow, so the second tap of a double
tap on one screen's Next lands on the next screen's Next. That tap is a first tap from a new
screen that is already on top, so it passes the origin throttle in `Navigator` and the
same-screen guard in `CaptureSetupNavigationController.navForward`. #1423 (task 102) put
`navigation/DoubleTapGuard.kt` in front of user select's add-user and Add org's Next only. The
steps an org can add to its setup flow still called `navForward` straight from Next:

- Session note: Next is always enabled. With a setup flow of `[user-select, session-note]`, a
  double tap on user select's Next skipped the note, started the session and opened capture.
- Wallet select, add wallet: Next is disabled until a wallet is picked or a name typed, so the
  second tap only goes through once Next is live. Same path, lower risk.

## Fix

`SessionNoteScreen`, `WalletSelectScreen` and `AddWalletScreen` put `DoubleTapGuard.modifier` on
their root and drop Next when `isSecondTapOfDoubleTap(navigator)` is true, as `AddOrgScreen`
does. Back, add wallet and the other actions are unchanged.

Each screen takes the guard as a parameter (default `rememberDoubleTapGuard()`), so a UI test can
pass one with its own clock and timeout. `Host.kt` still calls the screens with no arguments.

## Tests

- `SessionNoteScreenUiTest`, `WalletSelectScreenUiTest`, `AddWalletScreenUiTest`: Next pressed
  right after a navigation is dropped; Next pressed well after it calls `navForward` once. The
  existing tests (no navigation before the tap) still expect Next to go through.
- `e2e/flows/org-session-note-double-tap.yaml` (seed `session-note`, added to `e2e/run-org.sh`):
  double tap Next on user select, the session note is shown and capture is not; one more tap on
  Next then opens capture.
- `docs/testing/e2e-flow-test-plan.md` (H8 row, finding 6), `e2e/README.md`.

## Not verified here

The container has no Android SDK or emulator: nothing was compiled, and neither the unit tests
nor the flow were run. `python3 -m unittest discover -s e2e/ci -p 'test_*.py'` passes (58 tests).
CI has to show the rest.

## Left as is

The keyboard's Go on these screens goes through the same check as Next, as on Add org. A tap on
the text field within the double-tap timeout of arriving makes the next Go look like a second tap
and it is dropped once; the following Go or Next works. That is `DoubleTapGuard`'s existing
behaviour, not changed in this task.
