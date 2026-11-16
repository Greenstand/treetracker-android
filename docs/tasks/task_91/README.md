# Task 91 - E2E flows: first run, signup and capture

Issues: [#1393](https://github.com/Greenstand/treetracker-android/issues/1393) (A, first run and
signup) and [#1394](https://github.com/Greenstand/treetracker-android/issues/1394) (B, capture),
from `docs/testing/e2e-flow-test-plan.md`.

## What was added

Test Buddy (`tb`) flows in `e2e/flows/`. They select only by test-tag ids, use no sleeps, and
each starts from a fresh install (`launchApp: {clearState: true}`).

| Plan | Flow | Asserts |
|---|---|---|
| A1 | `signup-phone.yaml` | Phone signup with selfie reaches the Dashboard with 0 trees |
| A2 | `signup-email.yaml` | Same with an email address |
| A3 | `signup-validation.yaml` | Invalid phone or email shows the error snackbar and stays on the step; names shorter than 2 characters show errors and keep Next disabled; fixing them continues |
| A4 | `signup-selfie-retake.yaml` | Decline the selfie, retake, approve; a second signup that backs out of the camera creates no user |
| A5 | `signup-existing-user.yaml` | Same phone again shows the existing-user dialog; cancel keeps the step; picking the user goes on to capture setup |
| A6 | `signup-back.yaml` | Back from the credential step and the name step |
| B1 | `capture-first-tree.yaml` | First capture with both tutorials; Dashboard shows 1 to sync |
| B2 | `capture-loop.yaml` | Three captures without leaving; Dashboard shows 3 |
| B3 | `capture-reject.yaml` | Declining the photo saves nothing; Dashboard shows 0 |
| B4 | `capture-note.yaml` | A note added on review shows on the tree's detail screen in Settings |
| B5 | `capture-exit.yaml` | System back and the back arrow go to the Dashboard; the profile icon goes to user select |
| B6 | `capture-new-user.yaml` | Adding a user from user select goes through signup and lands on Add org, then capture |

Shared sub-flows: `subflows/signup.yaml` (env `CREDENTIAL`, `IDENTIFIER`, `FIRST_NAME`,
`LAST_NAME`) and `subflows/start-capture.yaml` (env `WALLET`).

App change: `CustomSnackbar` now carries the `Snackbar` test tag, as the root snackbar already
did, so A3 can check the signup error without matching translated text.

`e2e/run.sh` needs no change: the new flows have tags other than `dev-tools`, so they run in its
first `tb test` call, before `setup-device.sh` seeds the dev-tools data.

## Verification

- API 35 emulators (`TbTour35`, emulated front and back cameras, airplane mode, `geo fix 37.3 -3.5`).
- All 12 flows passed twice in full runs on two emulators, about 4 minutes each run.
- `bash e2e/run.sh -s emulator-5590`: first-screen and the 12 new flows pass. The existing
  `debug-overlays.yaml` (I2) fails locally on this 1080x2400 emulator: after Upload, the root
  "Sync Preparing" snackbar squeezes the debug "D" button, so the tap lands on Track. It fails the
  same way on its own on a freshly seeded device and passes on CI, so it is not caused by this change.
- `spotlessCheck` and `ktlintCheck` pass.
- CI's emulator is 320x640, where the keyboard covers Next. The flows hide the keyboard before
  tapping Next or a dialog button; all 12 also pass locally with `wm size 320x640` and `wm density 160`.

## Findings

Added to section 4 of the plan:

- Next on the signup credential step is never disabled. Invalid input shows a snackbar instead (#1413).
- Names drop digits and symbols while typing.
- System back on the signup name step skips the credential step and goes to the language picker (#1412).
- After the capture screen's profile icon, system back on user select closes the app. Confirmed
  with a probe flow; not asserted in the suite (#1411).
