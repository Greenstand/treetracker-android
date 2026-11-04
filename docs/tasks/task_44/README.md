# Task 44 - Make every screen reachable for Test Buddy flows

## Goal

Get each screen ready for end-to-end flows with Test Buddy (`tb`), without writing the flows
yet: every control a flow needs shows up in `tb look` with a stable, unique id, and taps land.

## Changes

- `view/TestTags.kt`: one place for e2e test tags (`TestTags`), plus `Modifier.exposeTestTags()`,
  which turns on `testTagsAsResourceId` so tags show up as resource ids for UiAutomator and `tb`.
  It is set on every composition root: `Root`, `ImageCaptureActivity`, `CustomDialog` and the
  tutorial dialog (dialogs are their own windows, so they need it too).
- `TreeTrackerButton` now exposes button semantics (role, click action, disabled, selected).
  Before, it handled taps in `pointerInput` only, so accessibility services and `tb` saw no
  button at all, and the language, user, org and dashboard buttons could not be found as tappable.
- Dialog buttons have their own tags (`DialogPositiveButton`, `DialogNegativeButton`,
  `DialogUserButton`) so they don't collide with the screen's Approve/Decline buttons under them.
  `tb` only shows an id when it is unique on screen.
- Tutorial dialogs: the done button is `TutorialDoneButton`; the decorative button pictures inside
  the tutorial are hidden from accessibility, so they no longer duplicate the real buttons' ids.
- Repeated items get per-item tags: `LanguageOption_<LANGUAGE>`, `UserButton_<wallet>`,
  `OrgOption_<name>`, `TreeButton_<index>`, `MapTreeCard_<index>`, `MessageItem_<index>`,
  `SurveyAnswer_<index>`, `DevParam_<name>`.
- Fields, counters and actions on every screen get tags (see `TestTags` for the full list).
  The dashboard settings icon also gets a content description ("Settings").
- Tests that found nodes by the old shared tags or by "the only clickable node" now use the new tags.
- `e2e/tour/` and `.github/workflows/tb-screen-tour.yml`: a temporary screen tour that walks the
  app with `tb` on an emulator and saves `tb look`, a screenshot and the hierarchy for each screen,
  before and after this change. Remove the workflow once the real flows land.

## Still needs more than a tag

- Camera: the emulator needs `-camera-back emulated -camera-front emulated`.
- GPS: a fix is needed before capture (`adb emu geo fix`).
- Chat has no send button; a flow sends with the keyboard's Go action (`pressKey: enter`).
- The unread badge sits inside the merged Messages and user buttons, so it isn't its own node.
- Seeding trees or users with AppFunctions needs API 37; the CI emulator is API 35.
- Splash stays put when location is denied (see the flow test plan).
