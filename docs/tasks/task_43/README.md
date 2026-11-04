# Task 43 - Android emulator end-to-end check in CI

## Goal

Run end-to-end tests on an Android emulator on every pull request, with Test Buddy (`tb`), as
their own GitHub check that fails when a test fails and passes otherwise.

## Changes

- `.github/workflows/android-e2e.yml`: new "Android end-to-end / emulator tests" check, modeled
  on the Android end-to-end job in Test Buddy's own CI. It builds `tb` (and its Android agent)
  from a pinned Test Buddy commit, builds the debug app with dummy keys, boots an API 35
  `google_apis` x86_64 emulator (KVM, with a cached boot snapshot), installs the app and runs
  `tb test e2e/flows/ -p android`. A failed step makes `tb` exit non-zero, which fails the check.
  The JUnit report and tb's debug files (screenshot, screen listing, log) upload as the
  `android-e2e` artifact on every run.
- `e2e/flows/first-screen.yaml`: the first flow. A fresh install, with its permissions granted
  by `launchApp`, opens on the language picker.
- `e2e/README.md` and `docs/testing/testing.md`: how to run the flows.

To pick up a newer `tb`, bump `TESTBUDDY_REF` in the workflow.
