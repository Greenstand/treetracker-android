# Task 111 - Double tap on Next skips org-configured setup steps (#1442)

Follow-up to task 102 (#1423), which left this open in its self-review.

## Bug

Every capture setup screen's Next is the bottom bar's right arrow. The second tap of a double tap
on one screen's Next lands on the next screen's Next. It comes from a new screen that is already
on top, so the origin throttle in `Navigator` and `navForward`'s same-screen guard both let it
through, and setup moves on a second step.

User select and Add org were guarded with `navigation/DoubleTapGuard.kt` in #1423. Session note,
wallet select and add wallet called `CaptureSetupNavigationController.navForward` directly. For an
org whose setup flow is `[user-select, session-note]`, a double tap on user select's Next skipped
the session note, started the session and opened capture.

## Fix

`SessionNoteScreen`, `WalletSelectScreen` and `AddWalletScreen` do what `AddOrgScreen` does: the
guard's `modifier` goes on a `Box` around the screen, and Next is dropped when
`isSecondTapOfDoubleTap(navigator)` says its press went down within the double-tap timeout of the
navigation that opened the screen. Nothing else on those screens is guarded (back, add wallet,
picking a wallet).

## Tests

- `e2e/flows/org-double-tap-session-note.yaml` (C6): seeded `session-note` org, double tap on user
  select's Next, the session note stays open, a note is typed and a tap of its own on Next reaches
  capture. Listed in `e2e/run-org.sh`.
- `e2e/run-org.sh` names each flow's results after the flow, not the variant, now that two flows
  share the `session-note` variant. The four existing result names do not change.
- The guard's logic is covered by the existing `DoubleTapGuardTest`. No Robolectric test was added
  for the three screens: `ComposeUiTest`'s navigator clock steps 1 s on every read, so a click in
  such a test is never inside the 300 ms window.

Nothing was built or run locally (no Android SDK or emulator in the session); CI is the first run.

## Left as is

- The keyboard's Go on session note and add wallet goes through the same check, as on Add org. Go
  has no press of its own, so it is judged by the last press on the screen: if that was a tap on
  the text field within 300 ms of the screen opening and nothing else was pressed since, the first
  Go is dropped (the next one works). A person can't tap that soon after a screen appears.
- No flow for wallet select / add wallet: their Next is disabled until a wallet is picked or a
  name typed, so a driven double tap has nothing to hit there.
