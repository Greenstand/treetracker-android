# Task 17: Persist organization name across tree-capture sessions (Issue #1259)

## Problem
After completing onboarding once, every subsequent "TRACK" tap forces the user
through `AddOrgScreen` with an **empty** Organization field. The previously
typed value is offered only as a tap-to-fill chip, so the user has to either
retype it or tap the chip on every single tree-capture session.

Reported by `@valerie808` on Samsung Galaxy Tab A8 (Android 14, build 202).

## Root cause
`AddOrgViewModel.init` resolves `orgName` from `orgRepo.currentOrg().name`
filtered by `takeIf { it != "Greenstand" }`. The default Greenstand org is
the only thing that survives across sessions until the user links a real org
via Firebase Remote Config, so the filter wipes the prefill every time.
The user's typed value is saved to `PREV_ORG_KEY` for the autofill chip but
never used as a prefill.

## Fix
- `AddOrgViewModel.init` now falls back to `PREV_ORG_KEY` when the current org
  is the default Greenstand placeholder, prefilling the field and pushing the
  resolved value to `CaptureSetupScopeManager` immediately. The user can hit
  forward without any further interaction.
- `AddOrgScreen` hides the autofill chip when its value already matches the
  prefilled `orgName` (avoids a redundant button next to a populated field).

## Files modified
- `orgpicker/AddOrgViewModel.kt` — prefill from `PREV_ORG_KEY`
- `orgpicker/AddOrgScreen.kt` — hide redundant autofill chip
- `test/.../orgpicker/AddOrgViewModelTest.kt` — updated default-org test, added
  prefill-from-prefs and current-org-priority tests
