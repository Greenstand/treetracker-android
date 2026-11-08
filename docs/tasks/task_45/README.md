# Task 45 - End-to-end flow test plan

## Goal

Audit the app's navigation and existing tests, and list the end-to-end flows (happy and sad
paths) worth testing, so broken contributor PRs and KMP migration regressions get caught.

## Changes

- `docs/testing/e2e-flow-test-plan.md`: navigation map (29 routes, org-configured flows,
  dialogs), what is covered today, about 50 proposed flows in 9 groups with priorities and a
  suggested per-PR smoke set, four likely bugs found while reading the code, and notes on
  building the suite (test tags, seeding, device control, backend).
- `docs/README.md`, `AGENTS.md` and `docs/testing/testing.md`: link the plan.

No code changes. The flows themselves are not written yet.
