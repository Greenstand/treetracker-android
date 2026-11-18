# Task 110 - H1 e2e flow passes when the app closes after location is denied (#1449)

Found in the review of PR #1428 (bug #1417).

## Problem

`e2e/flows/sad-location-denied.yaml` (H1) denied location at first launch and then only waited for
`SplashScreen` to be not visible. That is also true when the app closes or crashes after the denial,
so the flow could not tell #1417's fix from a closed app.

## How it was shown (PR #1446)

1. `102ad87` (temporary, reverted): debug builds call `activity.finish()` in the splash screen's
   permission result when location is denied. With the old flow, "Android end-to-end" passed:
   `✓ Wait until id: SplashScreen is not visible  249ms`, `✓ H1 Location denied at first launch passed`
   (run 44239647958).
2. `6f38679`: the flow waits for `LanguageOption_ENGLISH` instead. Against the same broken app it
   failed with `Assertion is false: id: LanguageOption_ENGLISH is visible` (run 44245658334); the
   other 26 flows passed.
3. The breakage is reverted; the strengthened flow passes against #1417's fix.

`SplashUiTest` caught the breakage (its two location-denied tests failed on steps 1 and 2); only
the e2e flow was blind to it.

## Change

- `e2e/flows/sad-location-denied.yaml`: wait for `LanguageOption_ENGLISH` (language select, where a
  fresh install lands), then assert the splash is gone.
- `docs/testing/e2e-flow-test-plan.md`: H1 row.

The container has no Android SDK or emulator, so the flow was checked on CI only.
