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
- `CaptureFlowScopeManager.restoreAt(route)`: when the scope is closed, opens it, restores the
  kept tree into `TreeCapturer`, and calls `CaptureFlowNavigationController.resumeAt(route, sessionId)`.
  It does nothing while the scope is open, so the normal flow is unchanged. If the kept tree
  can't be picked up (none kept, no interrupted session, the tree belongs to another session, or
  the step isn't in the org's flow), the kept tree is dropped and the screen sends the user to
  the dashboard, which ends the session, instead of reviewing an empty tree.
- `CaptureFlowNavigationController.resumeAt(route, sessionId)`: points the flow at that step,
  resumes the interrupted session (`SessionTracker.resumeInterruptedSession()`, from the
  persisted `session-active` id; it must match the tree's session) and restarts the step counter,
  orientation sensor and GPS updates.
- `di/AppModule.kt`: the `TreeImageReviewViewModel` and `TreeHeightSelectionViewModel`
  definitions call `restoreAt()` for their step before resolving `TreeCapturer`, and tell the
  view model when they restored the flow. Tree height is part of capture flows that list
  `tree-height-selection` (for example the org config example in `OrgRepo.kt`) and crashed the
  same way.
- `TreeImageReviewViewModel`: does not show the first-tree review tutorial again after a
  restore. The app was killed after the photo was taken, and the dialog would cover Approve.
- `TreeCapturer.saveTree()` clears `currentTree` once the tree is saved, so a later change can't
  store or save it again.

Back on the capture screen, `TreeCaptureViewModelFactory` opens a fresh scope as usual; the
session, sensors and GPS are singletons, so the resumed session carries on.

Approving the restored tree saves it once and pops back to capture, where the next tree goes
into the same session.

## Review

A review of the fix found the tree height crash, the restore going ahead with nothing to restore,
the session id not being checked against the kept tree, and `currentTree` staying set after a
save; all are fixed above. Not changed: the step count baseline is reset on resume (steps walked
before the kill are not counted, same as starting a session), and a note typed but not yet added
is lost on process death.

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
