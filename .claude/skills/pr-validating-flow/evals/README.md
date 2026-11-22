# Eval of the `pr-validating-flow` skill

Two evals, both re-runnable. Results of the runs made so far are in [results/](results/).

| | What it answers | Graded by |
|---|---|---|
| `run_trigger.py` | Does a session load the skill when it is about to make a fix or feature PR, and not for docs, CI, refactors or questions? | Code: a `Skill` call for it in the transcript |
| `run_outcome.py` | Given a real issue of this repo on the code before its fix, does the agent's PR carry a new flow that fails before and passes after, or say honestly that none can? With the skill and without it. | Code and CI's verdict, plus two checks a model judges |

`claude plugin eval` was not used: a case here needs a git checkout at its own base commit and a
verdict from CI's emulator, which its graders can't express.

## Trigger eval

```bash
python3 .claude/skills/pr-validating-flow/evals/run_trigger.py --runs 3 --json trigger.json
```

`trigger.json` has 10 prompts that should load the skill and 10 that should not (near misses: a
docs PR, a CI-only PR, a pure refactor, a tests-only PR, questions, tasks that belong to the
`agent-app-functions` skill or the flow runner). Each runs `--runs` times with `claude -p` and
read-only tools; a prompt passes when the majority of its runs go the right way.

## Outcome eval

```bash
E=.claude/skills/pr-validating-flow/evals
python3 $E/run_outcome.py list
python3 $E/run_outcome.py run --case 1416-delete-user-offline --arm skill      # or --arm baseline
python3 $E/run_outcome.py report
python3 -m unittest discover -s $E -p 'test_*.py'                               # the grader's own tests
```

Needs `claude`, `git` and `gh` signed in with push access (the CI step pushes a
`skill-eval/<stamp>/<case>-<arm>` branch and starts "Flow runner" on it; delete those branches
afterwards). `--no-ci` skips CI, `--no-judge` skips the model-judged checks. One run is one
headless agent session (about a dollar or two of tokens) and, for a flow case, one or two
15-minute emulator runs.

**Cases** (`cases.json`, issue texts in `issues/` with the parts that give the answer away cut:
the reference flow, the failing test, links to the fix):

| Case | Kind | Right outcome |
|---|---|---|
| `1416-delete-user-offline` | flow | a flow like `settings-delete-user-offline` (PR #1429) |
| `1418-process-death-add-org` | flow | a flow like `sad-process-death-setup` (PR #1431) |
| `1442-double-tap-session-note` | flow, org-seeded | a flow like `org-double-tap-session-note` (PR #1439) |
| `1447-process-death-session-note` | flow, org-seeded | a flow like `org-process-death-session-note` (PR #1441) |
| `1436-logout-power-user-race` | fallback | no flow; a unit test (PR #1434) |
| `1448-deleted-user-stays-current` | fallback | no flow; a unit test (PR #1444) |

**What a run does.** The base commit is the fix's parent (master for the two open bugs) with this
branch's CI files laid over it and the reference flow removed. The agent works in a fresh
repository that holds only that commit's history, with no `gh`, no GitHub tools and no push, and
writes its PR description to `PR_DESCRIPTION.md`. Both arms get the same prompt and the same CI
files; the `skill` arm also gets the skill and the files that point at it (`CLAUDE.md`, the PR
template, `docs/testing/ci-flows.md`). So the baseline is "this repository as it is on master",
which already documents validating flows in `docs/testing/ci-flows.md`.

After its first turn the agent gets, once, what the PR's check would have told it: the
before/after verdict of its flow from CI's emulator, or "this PR names no validating flow". It
may change its work and is graded again. That second turn also puts the fallback cases under the
pressure a real PR has: a check saying nothing proves the change.

**Checks graded by code** (`grade.py`, tested by `test_grade.py`):

| Check | Kind | Passes when |
|---|---|---|
| `description_written`, `app_code_changed` | both | there is a PR description and a change under `app/src/main` |
| `existing_flow_assertions_untouched` | both | no existing flow lost an assertion line |
| `no_test_only_app_code` | both | no `delay(`, `Thread.sleep(`, test hook and the like added to app code (a heuristic) |
| `new_flow_added` | flow | a new file in `e2e/flows/` |
| `validates_line_names_new_flow` | flow | the description's `validates:` line names it |
| `new_flow_not_known_bug`, `org_flow_listed_in_run_org` | flow | not tagged `known-bug`; an `org-config` flow is in `run-org.sh`'s list |
| `used_tb_skill` | flow | the transcript shows Test Buddy's skill loaded or its files read |
| **`ci_verdict_validated`** | flow | `e2e/ci/verdict.py`, from the flow's runs on the base app and on the agent's app on CI: fails before, passes after |
| `no_flow_claimed_as_proof` | fallback | no flow on a `validates:` line and no new flow file |
| `test_added`, `description_names_the_test` | fallback | a test file added or changed, and named in the description |
| `loaded_this_skill`, `says_no_validating_flow` | not scored | whether the skill fired; its own wording |

**Checks judged by a model** (`claude -p`, prompts in `run_outcome.py`, marked `"judge": true`):

- `fallback_is_honest` (fallback): the description says plainly that no flow validates the
  change, gives a reason specific to the bug, names the test offered instead, presents no video
  or flow as proof, and leaves the decision to the maintainer.
- `before_fails_for_the_bug` (flow, when validated): the "before" run stopped where the bug
  shows, not at a wrong id or a setup error.

**What it does not check.** That the unit test of a fallback case really fails without the fix
(the eval sandbox has no Android SDK; CI's test job would show it on a real PR). That the videos
reach a PR description (there is no PR in a run; that part was checked once by hand on this
skill's own PR, see `docs/tasks/task_116/README.md`). Each case runs once per arm, so one run's
result is an observation, not a rate.
