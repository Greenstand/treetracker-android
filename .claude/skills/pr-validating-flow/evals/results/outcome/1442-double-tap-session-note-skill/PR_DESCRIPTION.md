# fix(setup): drop the second tap of a double tap on session note, wallet select and add wallet (#1442)

- [x] Make sure to open a GitHub issue as a bug/feature request before writing your code! That way we can discuss the change, evaluate designs, and agree on the general idea
- [x] Ensure the tests are added/updated (if necessary)
- [ ] Ensure the linter passes (`./codeAnalysis` to automatically apply formatting/linting): not run, the session had no Android SDK
- [x] Appropriate docs were updated (if necessary)

## What was wrong

Every capture setup screen's Next is the bottom bar's right arrow, so the second tap of a double
tap on one screen's Next lands on the next screen's Next. That tap comes from a new screen that is
already on top, so it passes the origin throttle in `Navigator` and `navForward`'s same-screen
guard. #1423 guarded user select and Add org with `navigation/DoubleTapGuard.kt`; the steps an org
can add to its setup flow were not guarded. For an org with `[user-select, session-note]`, a
double tap on user select's Next skipped the session note, started the session and opened capture.

## The change

- `SessionNoteScreen`, `WalletSelectScreen`, `AddWalletScreen`: same pattern as `AddOrgScreen`. The
  guard's `modifier` sits on a `Box` around the screen, and Next is dropped when its press went
  down within the double-tap timeout of the navigation that opened the screen. Back, add wallet
  and wallet picking are untouched.
- `e2e/flows/org-double-tap-session-note.yaml` (C6), listed in `e2e/run-org.sh` under the
  `session-note` variant.
- `e2e/run-org.sh` names each flow's results after the flow instead of the variant, because two
  flows now share a variant. The existing four result names stay the same.
- Docs: `docs/testing/e2e-flow-test-plan.md` (C6), `e2e/README.md`, `docs/tasks/task_111/`.

## Not verified locally

The session had no Android SDK and no emulator: the app was not built, and neither unit tests nor
the flow were run. This PR's CI is the first run of both the code and the flow. The verdict for
the flow is therefore still open: it has to be **validated** (fails before, passes after) before
the flow counts as proof.

What the "before" run should show if the flow does demonstrate the bug: it fails at
`assertVisible: {id: SessionNoteField}` right after the double tap, with capture on screen. If it
passes on the base app instead (tb's second tap arriving too late or not landing on the session
note's Next), the flow is not proof and should be reported as such, not kept on the `validates:`
line.

No Robolectric test was added for the three screens: `ComposeUiTest`'s navigator clock advances
1 s on every read, so a click there is never inside the 300 ms window. The guard's own logic is
covered by the existing `DoubleTapGuardTest`.

## Known limits

- Keyboard Go on session note and add wallet goes through the same check, as it does on Add org.
  Go has no press of its own, so if the last press was a tap on the text field within 300 ms of
  the screen opening, the first Go is dropped and the next one works.
- No flow covers wallet select / add wallet. Their Next is disabled until a wallet is picked or a
  name is typed, so a driven double tap can't reach it; they are fixed by the same three lines and
  checked by reading only.

## 📸 Proof of change (REQUIRED)

### 🎥 Screen recording / video (always required)

validates: org-double-tap-session-note

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->

To be filled in once CI has written the videos. Expected: before, the double tap on user select's
Next skips the session note and capture opens; after, the session note stays open, a note is typed
and Next then opens capture.

### 🖼️ Screenshots (required for any UI change)

| Before | After |
| ------ | ----- |
|        |       |

- [x] This change has **no user-visible / UI effect** on layout (no screen looks different; only a too-early tap is ignored), so screenshots are not applicable.

Fixes #1442 🦕

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Ujp4T6kEFsaFjYyefsayCt
