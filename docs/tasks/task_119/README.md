# Task 119 - Unit test flake: "Dispatchers.Main is used concurrently with setting it"

## Problem

Unit test runs fail now and then with `IllegalStateException: Dispatchers.Main is used
concurrently with setting it`, reported in `KoinGraphTest` or a `DashboardViewModel` test.

`DashboardViewModel.updateNonTreeData` runs on `Dispatchers.IO` (a real thread pool in tests) and
then switched to `Dispatchers.Main` to update the state. A test that finishes while that work is
still running resets `Dispatchers.Main` (`MainCoroutineRule.finished`) at the moment the IO thread
dispatches onto it. kotlinx-coroutines-test detects the concurrent use and throws on the IO
thread; the uncaught exception is then reported by whichever test starts next, which is why the
failure moves between test classes.

## Change

- `BaseViewModel.updateState` updates the `MutableStateFlow` atomically (`update {}`), so it is
  safe from any thread. Before, it read the value and wrote it back in two steps, which is only
  safe on one thread.
- `DashboardViewModel.updateNonTreeData` updates the state from the IO coroutine directly. No
  switch to `Dispatchers.Main`, so nothing touches Main after the test has moved on.

## Test

`DashboardViewModelMainDispatcherTest` sets a `StandardTestDispatcher` as Main and never advances
it, then waits for the org and unread-message flags to reach the state. With the switch to Main
the update never runs and the test fails; without it the test passes.

## What was checked

See the PR description for the CI runs (test alone: fails; with the change: passes).
