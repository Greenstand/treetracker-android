# fix(setup): drop the second tap of a double tap on session note, wallet select and add wallet (#1442)

Fixes #1442.

## What was wrong

Every capture setup screen's Next is the bottom bar's right arrow. The second tap of a double tap
on one screen's Next lands on the next screen's Next. It comes from a new screen that is already
on top, so it passes both the origin throttle in `Navigator` and the same-screen guard in
`CaptureSetupNavigationController.navForward`.

#1423 guarded user select's add-user and Add org's Next with `navigation/DoubleTapGuard.kt`. The
steps an org can add (session note, wallet select, add wallet) still called `navForward` directly.
For an org whose setup is `[user-select, session-note]`, a double tap on user select's Next
skipped the note, started the session and opened capture.

## Change

- `SessionNoteScreen`, `WalletSelectScreen`, `AddWalletScreen`: `DoubleTapGuard.modifier` on the
  screen root; Next is dropped when `isSecondTapOfDoubleTap(navigator)` is true. Same pattern as
  `AddOrgScreen`. Other actions are unchanged.
- Each of the three screens takes the guard as a parameter, defaulting to
  `rememberDoubleTapGuard()`, so UI tests can pass a guard with a fixed clock. `Host.kt` is
  unchanged.

## Tests

- `SessionNoteScreenUiTest`, `WalletSelectScreenUiTest`, `AddWalletScreenUiTest`: Next right after
  a navigation is dropped; Next well after it calls `navForward` once.
- New flow `e2e/flows/org-session-note-double-tap.yaml` (seeded with `session-note` through
  `e2e/run-org.sh`): double tap Next on user select, the session note stays open, then a single
  Next opens capture.
- Docs: `docs/tasks/task_111/README.md`, H8 in `docs/testing/e2e-flow-test-plan.md`, `e2e/README.md`.

validates: org-session-note-double-tap

## Not verified locally

This was written without an Android SDK or emulator. The app was not compiled, and the unit
tests, linter and e2e flow were not run. Only the `e2e/ci` Python tests were run (58 pass). CI is
the first build of this branch.

## Review notes

- Wallet select and add wallet have no flow of their own for this: their Next is disabled on
  arrival, so a driven double tap can't reach it. The UI tests cover them.
- The keyboard's Go goes through the same check as Next, as on Add org. A tap on the text field
  within 300 ms of arriving makes the next Go be dropped once. This is `DoubleTapGuard`'s existing
  behaviour and is not changed here.

## 📸 Proof of change

### 🎥 Screen recording / video

Not attached: no device was available. The "before and after" job records
`org-session-note-double-tap` on the base app and on this branch's app; the recording should be
taken from there before merging.

### 🖼️ Screenshots

- [x] This change has **no user-visible / UI effect** on layout: no screen looks different, so
  before/after screenshots are not applicable.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
