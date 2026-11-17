# Task 98 - Process death on image review (#1419)

Reopening the app after Android killed it on image review crashed with
`IllegalStateException: CaptureFlowScope not open`.

## Cause

Nav3 saves the back stack, so after process death it restores image review. The capture flow
scope (`CaptureFlowScopeManager`), the tree being captured (`TreeCapturer.currentTree`) and the
current session id (`SessionTracker`) only lived in memory. Creating `TreeImageReviewViewModel`
resolves `TreeCapturer`, whose Koin factory calls `CaptureFlowScopeManager.getData()`, which
threw because nothing had opened the scope.

Reopening the scope alone is not enough: the photo, location and step data of the tree would be
gone, the flow would start at step 0, and the next capture would fail with "Session must be
started" because the session id and GPS updates were lost too.

## Fix

- `PendingTreeStore` (new): keeps the tree whose photo was taken in preferences
  (`SYSTEM_SETTINGS/pending-capture-tree`, JSON). `TreeCapturer` saves it on `setImage`,
  `addAttribute` and `setNote`, clears it once `saveTree` succeeds, and drops it when the user
  leaves the capture flow (`CaptureFlowNavigationController.endSession`).
- `CaptureFlowScopeManager.restoreOnImageReview()`: when the scope is closed, opens it, restores
  the kept tree into `TreeCapturer`, and calls `CaptureFlowNavigationController.resumeAt(image review)`.
  It does nothing while the scope is open, so the normal flow is unchanged.
- `CaptureFlowNavigationController.resumeAt(route)`: points the flow at that step, resumes the
  interrupted session (`SessionTracker.resumeInterruptedSession()`, from the persisted
  `session-active` id) and restarts the step counter, orientation sensor and GPS updates.
- `di/AppModule.kt`: the `TreeImageReviewViewModel` definition calls `restoreOnImageReview()`
  before resolving `TreeCapturer`, and tells the view model when it restored the flow.
- `TreeImageReviewViewModel`: does not show the first-tree review tutorial again after a
  restore. The user saw it before the app was killed, and the dialog would cover Approve.

Approving the restored tree saves it once and pops back to capture, where the next tree goes
into the same session.

## Tests

- Unit: `PendingTreeStoreTest` (new), `TreeCapturerTest`, `SessionTrackerTest`,
  `CaptureFlowScopeManagerTest`, `CaptureFlowNavigationControllerTest`,
  `TreeImageReviewViewModelTest`, and `KoinGraphTest` (resolving `TreeImageReviewViewModel` on
  the real graph with no scope open, the crash path).
- Gradle could not run in the agent container (Maven Central rate limit, no Android SDK), so
  the unit tests ran on CI only.
- E2E: `e2e/flows/sad-process-death-review.yaml` no longer tagged `known-bug`, so the
  "Android end-to-end" check runs it. Before/after videos come from the "E2E before/after videos"
  workflow on the PR.

## Out of scope

Process death on the capture screen itself (#1420) and on Add org (#1418) are fixed in their own
tasks.
