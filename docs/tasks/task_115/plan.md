# Plan for parts B to D

Goal: a PR check whose output shows whether the PR's fix or feature works: a flow that **fails on
the base branch's app and passes on the PR's app**, recorded both ways, with an honest result when
that is not possible. Separate PRs, in this order. B is built, with D's GIF embed (see the
README's "B. Before and after" for what was checked); C is on hold for a maintainers' decision.

## B. Before/after, in pure code

**Which flows.** At most 3 per PR, from two sources:

1. Flow files the PR adds or changes (`e2e/flows/*.yaml`, not sub-flows): what `e2e-affected.yml`
   already runs whole, and what the #1424 prototype used. A fix PR that drops `known-bug` from its
   flow is picked up this way with no extra step.
2. A line in the PR description, `validates: <flow> [<flow> ...]`, for a flow that already exists
   and is not edited. Parsed the way `flow-runner-pr.yml` parses its `run-flows:` line
   (`^[[:space:]>*-]*validates:`), each word checked against `^[A-Za-z0-9_./-]+$` and resolved to
   a file under `e2e/flows/` in the PR's checkout; anything else is refused. The description is
   read through the API as data, never interpolated into a shell line.

**What runs.** One job, one emulator (extends `flow-runner-job.yml` with a `before_ref` input):

- "Before" app: the code at the merge base of the PR and its base branch, built in a second
  worktree. "After" app: the PR's head. Flow files, sub-flows, seeding scripts (`seed-org.sh`,
  `setup-device.sh`) and `e2e/ci` are the PR's for both runs, as in #1424.
- For each flow: uninstall, install the before APK, run with `--record --annotate`; uninstall,
  install the after APK, run again. `run-flows.sh` is called once per side with its own `OUT`
  (`before/`, `after/`), so retries for a lost recording, org seeding and the summary are reused.
- Cost: one more app build (Gradle cache warm) and each flow twice. No second emulator boot.

**Verdict** (`e2e/ci/verdict.py`, pure code, unit tested), per flow:

| Before (base app) | After (PR app) | Verdict |
|---|---|---|
| fails | passes | ✅ **validated**: the flow fails without the change and passes with it |
| passes | passes | ⚠️ **does not demonstrate the change**: it passes without it too |
| fails | fails | ❌ **broken**: fails on the PR's app (and on base) |
| passes | fails | ❌ **broken**: the PR makes a passing flow fail |
| either did not run (build failed, setup failed) | | ❔ **inconclusive**, with the reason |

The comment shows, per flow, the verdict, both results with the step each failure stopped at and
tb's error line (so a "before" that failed for the wrong reason is visible: a missing id on a new
screen is a fair failure for a feature, a seeding error is not), and both videos side by side. A
recording lost on either side is said in that cell (part A). The check fails only for **broken**;
"does not demonstrate" is reported loudly but does not fail (edited flows often pass both ways for
good reasons: a renamed id, a refactored sub-flow). Not a required check.

**PRs with no validating flow** keep today's behaviour, with the comment saying plainly "this PR
names no validating flow: the clips below only show the screens near the change, not that the
change works".

**Fork PRs.** Unchanged rules: nothing runs on its own; a maintainer runs it by hand with the full
sha they read; read-only token; no caches saved; no results branch; flows picked by the base
branch's selector. The `validates:` line of a fork PR is only read on that manual run.

**Known limits to state in the docs.** One run each way, so a flaky flow can give a false verdict
(option: run "before" twice and require both to fail); a flow that fails on base for an unrelated
reason reads as validated unless the reviewer reads the failing step; the base app is the merge
base, so a base branch that is itself broken shows as "fails before".

## C. Generating a validating flow in CI when the PR has none

Design note only, no prototype yet, because the premise does not hold today:

- **GitHub Models is gone.** GitHub retired it on 30 July 2026 (playground, catalogue and
  inference API; GitHub's changelog and the docs landing page both say so). There is no free
  `permissions: models: read` endpoint for the built-in token any more. GitHub's own pointers are
  Azure AI Foundry and Copilot, both of which need a paid account and a stored credential.
- That breaks the "no secrets" rule of the brief. The options left:
  1. **A model API key as a repository secret** (any provider), used only in a job that checks out
     nothing from the PR, runs no PR code, has `contents: read` and gets the diff, the linked
     issue and the id catalogue as data. Its only output is one flow file, checked by pure code
     (a schema plus an allow-list of the commands today's flows use: `launchApp`, `tapOn`,
     `doubleTapOn`, `inputText`, `assertVisible`, `assertNotVisible`, `extendedWaitUntil`, `back`,
     `hideKeyboard`, `runFlow` to an existing sub-flow; ids must exist in `TestTags`; nothing
     that runs a script or a shell command; size capped). A second job, without the secret, runs it through
     part B's oracle; up to 3 tries, feeding back only the oracle's result. Never for fork PRs.
     Needs a maintainer to decide on a provider and a budget.
  2. **Generate at authoring time, not in CI** (recommended). Most PRs here are written by an
     agent session that already has the diff, the issue and the flow catalogue, and can write
     the flow, run part B's oracle on its own PR (`cloud-flows.sh`) and iterate before asking for
     review. CI stays pure code with no model and no new trust surface. For a PR that still has
     no validating flow, the comment says so (see B) and falls back to the smoke clip.
- Whichever is chosen, **the oracle stays pure code**: a generated flow counts only if it fails on
  base and passes on head.

**What this cannot cover, with or without a model** (all five trial PRs were of these kinds):

- A fix with no on-screen symptom (a leak, a stale ViewModel, state that is only wrong in the
  database or a log).
- Millisecond races (the double-tap guards): whether a driven double tap lands both taps inside
  the window the bug needs is down to emulator timing, so "before" can pass too.
- States a CI emulator cannot reach offline (a server error on delete, a sync conflict), or that
  need process death at an exact moment.

For those the honest output is "no validating flow" and a unit or instrumented test in the PR.

## D. Inline playback

Tried in a real comment (on the throwaway PR #1458, with a recording from a results branch):

| Way | Result |
|---|---|
| GIF on a results branch, `<img src="https://raw.githubusercontent.com/...gif">` | **Plays inline**, in GitHub's animated-image player (with a pause button). Built-in token, nothing new. |
| `<video src="...mp4">` pointing at the results branch | Stripped from the comment: nothing is shown. |
| The MP4's URL on a line of its own | A plain link. GitHub only turns its own `user-attachments` URLs into a player. |

Sizes: a 32 s annotated clip (320x690) is 0.23 MB as MP4 and 0.56 MB as a GIF at full speed,
8 fps, 270 px wide, 64 colours (the #1424 settings without the double speed); a 20 s one is
0.40 MB.

Not tried, because it needs a maintainer's secret: `gh pr comment --attach video.mp4`, which
gives GitHub's real video player. It needs gh 2.99 or newer on the runner and a **user** token
with push access: GitHub's changelog names the `gh auth login` OAuth token or a classic PAT, and
#1424 found the built-in token is refused. A classic PAT is broad (the whole `repo` scope, for
every repository its owner can push to), and it would sit in a job that handles files a PR's
code produced. Videos are capped at 10 MB on the Free plan.

**Recommendation:** GIF by default, next to the MP4 link (it works for every PR from this
repository with the built-in token, and it is what a reviewer sees without clicking). Add the
attachment path later only if maintainers want a real player enough to keep an
`E2E_ATTACH_TOKEN`; the job that uses it must run no PR code and only upload the files.
For a fork's PR (run by hand, no results branch) the comment keeps linking the artifact.
