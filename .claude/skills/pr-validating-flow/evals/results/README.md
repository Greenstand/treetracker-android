# Eval results, 22 November 2026

One run of each eval, with the skill at the commits named in each `result.json`
(`skill_commit`). Model: `claude-fable-5-2` for the agents and the judge. Each outcome case ran
once per arm, so read a row as one observation, not a rate.

## Trigger eval: 20/20

`trigger.json` in this folder. All 10 "should load" prompts loaded the skill in 3 of 3 runs. Of
the 10 "should not" prompts, 9 never loaded it; the CI-only "bump the Test Buddy pin" prompt
loaded it in 1 of 3 runs (a pass by majority).

## Outcome eval: 57/58 with the skill, 48/57 without

| Case | Kind | With skill | Baseline | What the baseline missed | CI verdict of the flows (both arms) |
|---|---|---|---|---|---|
| 1416-delete-user-offline | flow | 10/10 | 6/9 | no new flow: it edited two existing flows and named those; did not use Test Buddy's skill | validated |
| 1418-process-death-add-org | flow | 9/10 | 8/10 | no `validates:` line; did not use Test Buddy's skill | validated |
| 1442-double-tap-session-note | flow, org | 11/11 | 10/11 | did not use Test Buddy's skill | validated |
| 1447-process-death-session-note | flow, org | 11/11 | 10/11 | did not use Test Buddy's skill | validated (3 flows each) |
| 1436-logout-power-user-race | fallback | 8/8 | 7/8 | `fallback_is_honest` (judge): declared the case exempt instead of leaving the decision to the maintainer | no flow claimed |
| 1448-deleted-user-stays-current | fallback | 8/8 | 7/8 | same | no flow claimed |

Per run: `outcome/<case>-<arm>/result.json` (every check, with its detail), the PR description
the agent wrote, and CI's `verdict.md` for the flow cases.

What the numbers say, and don't:

- **The baseline is strong.** Master's `docs/testing/ci-flows.md` already describes validating
  flows and what they can't show, and an agent that reads it gets most of the way. Every flow in
  both arms was validated by CI on its first run (fails on the base app, passes on the agent's),
  and no run, in either arm, pushed a fake flow onto the two no-symptom bugs, also not after being
  told "this PR names no validating flow".
- **What the skill changed:** always a *new* flow and a `validates:` line (2 baseline runs
  missed one or the other); Test Buddy's skill read before writing the flow (3 of 4 with-skill
  runs, 0 of 4 baseline); the `e2e-videos` markers in the description (6 of 6 against 0 of 6; not
  scored, since CI adds the block without them); and in the fallback cases, listing what was
  tried and leaving the merge decision to the maintainer (2 of 2 against 0 of 2, judged by a
  model).
- **The one with-skill miss** (1418: wrote the flow without reading Test Buddy's skill) led to a
  change in `SKILL.md`: that step now says "before you write any YAML, every time", with the
  reason, and is on the checklist. Re-run of that case with the changed skill
  (`outcome/1418-process-death-add-org-skill-rerun/`): it read the skill; 10/10, validated. Its
  first CI attempt failed on a GitHub API 404 while `cloud-flows.sh` waited for the run
  (`cloud-flows.sh` now retries that), so the verdict is from the second attempt, with the flow
  unchanged.
- **Model-judged checks:** `fallback_is_honest` (4 runs) decided the only difference between the
  arms on the fallback cases; `before_fails_for_the_bug` passed on all 8 flow runs, with the
  judge noting for the process-death flows that the error text alone can't tell the pre-kill
  assertion from the post-kill wait on the same id.

Not run, or not checked:

- Whether each fallback case's unit test fails without the fix (no Android SDK in the eval
  sandbox).
- The feedback round on a failing verdict: no flow got one, so "the agent repairs a flow that
  does not demonstrate the change" was never exercised.
- A feature (as opposed to a bug fix) case.

Cost: about $35 of model tokens for the 13 agent runs and the judges, and 10 emulator runs on CI
of about 10 minutes each (one of them the harness's own check with the real fix of #1447, which
came back validated).

## Branches to delete

The CI step pushed these; nothing depends on them:

```
skill-eval/reference/1447
skill-eval/1122a/1416-delete-user-offline-skill      skill-eval/1122a/1416-delete-user-offline-baseline
skill-eval/1122a/1418-process-death-add-org-skill    skill-eval/1122a/1418-process-death-add-org-baseline
skill-eval/1122a/1442-double-tap-session-note-skill  skill-eval/1122a/1442-double-tap-session-note-baseline
skill-eval/1122a/1447-process-death-session-note-skill  skill-eval/1122a/1447-process-death-session-note-baseline
skill-eval/1122b/1418-process-death-add-org-skill
```

Their `flow-results/run-<id>` branches are removed by the flow runner after 14 days.
