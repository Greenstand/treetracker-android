# Task 106 - Double tap on Next skips org-configured setup steps (#1442, follow-up to #1423)

## Bug

#1423 (PR #1427) guarded user select's Next and Add org's Next against double taps with
`navigation/DoubleTapGuard.kt`. The setup steps an org can add to its capture setup flow were left
out: wallet select, add wallet and session note call `CaptureSetupNavigationController.navForward`
straight from Next.

Every setup screen's Next is the bottom bar's right arrow. The second tap of a double tap on the
previous screen's Next lands on the new screen's Next. It comes from a new screen that is on top,
so neither the origin throttle in `Navigator` nor `navForward`'s same-screen guard stops it, and
setup moves on a second step. For an org whose setup flow is `[user-select, session-note]`, a
double tap on user select's Next skips the session note and opens capture. Session note's Next is
always enabled, so that's the reachable case. Wallet select's Next is enabled only once a wallet
is picked, and add wallet's only once a name is typed, so they're guarded for the same reason when
their Next is live.

Found in the self-review of PR #1427.

## Test (step 1, before the fix)

`app/src/test/java/.../uitest/navigation/SetupNextDoubleTapUiTest.kt` (Robolectric Compose UI)
renders each screen behind a navigator that last navigated 0 ms ago (the screen was just opened)
and taps Next. Without the fix Next reaches `navForward`, and the test fails with
"Next on <screen> acted on the second tap of the double tap that opened it, so capture setup moved
on again and skipped a step". Control tests tap long after the screen opened and expect one
`navForward`.

CI on the test-only commit (fbfd1ed) failed exactly those three tests, with that message;
the controls passed.

## Fix

`SessionNoteScreen`, `WalletSelectScreen` and `AddWalletScreen` do what `AddOrgScreen` does: wrap
the screen in a `Box` with `rememberDoubleTapGuard().modifier` and skip `navForward` when
`isSecondTapOfDoubleTap(navigator)` says Next is the second tap of the double tap that opened the
screen. No new mechanism.

Edge-case tests added with the fix: keyboard Go on session note and on add wallet still continues
setup, since the guard judges an action with no press of its own as happening now. These check the
wiring only; how the guard judges and uses up a press is covered by `DoubleTapGuardTest`.

Known behaviour, same as user select and Add org: every stack change counts as a navigation, pops
included, so a Next pressed within the double-tap timeout of returning to one of these screens
(Back from add wallet, say) is dropped, and a second tap works.

## Review (independent subagent)

Verdict: valid and in scope; no bugs. Findings and what was done:

- Risk: the UI tests record no press on the navigator, so they cover the guard's "time since the
  navigation" rule, not the "press vs the tap before the navigation" rule PR #1451 adds. The tests
  can't use `Navigator.recordPress` until #1451 merges; the class doc now says which rule they
  cover. Follow-up once #1451 lands: one variant that records the tap before the navigation.
- Nit: the session note Go test tapped the field first, which made it look like it tested the
  guard using up that press. It couldn't, since the navigator's clock and the press clock are
  separate. Renamed to "session note keyboard Go continues setup" and the field tap removed.
- Nit: quick Next right after Back is dropped. Kept; consistent with user select and Add org
  (documented above).
- Nit: task id 106 isn't the next free integer. Kept; 106 was reserved for this task.

## Why not an e2e flow

These steps only exist in org-configured flows, which need `e2e/seed-org.sh`. Neither the
"Android end-to-end" job nor the before/after video workflow seeds an org, so a flow wouldn't run
on CI. The Compose UI test runs in `testDebugUnitTest` on every PR.
