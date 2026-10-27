# Task 40 - Plan the KMP and CMP migration

## Goal

Write a plan for moving the app to Kotlin Multiplatform and Compose Multiplatform, and for
splitting it into modules along the way.

## Changes

- `docs/engineering/kmp-cmp-migration.md`: the plan. Current state, target module layout,
  library swaps, five phases, the image upload decision, risks, testing and open decisions.
- `CLAUDE.md` (AGENTS.md): links the plan from the Engineering section.
- `.github/workflows/pull_request.yml`: runs for PRs into any branch (see below).

## Notes

- No code changes. The plan is a proposal to review before Phase 0 starts.
- Findings that shaped it: the app is one ~25k-line Kotlin module, already all Compose with
  Navigation 3, Room 2.8 and Koin. Retrofit has a single endpoint. The hard parts are S3
  uploads via the AWS Android SDK, CameraX, location/sensors, WorkManager and Firebase.
- `models/` currently imports screen packages (via `TreeTrackerViewModelFactory`,
  `SessionTracker`, `ConvergenceConfiguration`); that cycle has to be broken first.

## Decisions

- **Structure first, then ship iOS.** Phases 0 to 3 (build setup, modules, library swaps, KMP on
  Android only) come first. Phase 4 then ships an iOS app to TestFlight and the App Store. Earlier
  phases should choose KMP-friendly options, and the backend team should be asked about pre-signed
  upload URLs early.

## CI for stacked PRs

`pull_request.yml` only ran for PRs into `master` and `release*`. The restructuring lands as a
stack of PRs, each based on the one below it, so most of them would have had no CI. It now runs
for PRs into any branch.
