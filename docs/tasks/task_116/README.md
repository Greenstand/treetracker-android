# Task 116 - A skill for validating flows on fix and feature PRs, with an eval

## Problem

Tasks 114 and 115 gave CI a before/after check: a flow named on a `validates:` line (or added by
the PR) runs on the app at the merge base and on the PR's app, recorded with
`tb test --record --annotate`, and gets a verdict in pure code. Nothing told an agent writing a
fix to produce such a flow, to use Test Buddy's own skill for it, or what to do when no flow can
show the change. And the videos ended up in a PR comment, not in the description.

## Change

- **`.claude/skills/pr-validating-flow/SKILL.md`**, linked from `CLAUDE.md`/`AGENTS.md`, the docs
  index, the PR template and `docs/testing/ci-flows.md`. For a bug-fix or feature PR: one new
  flow in `e2e/flows/` that fails before and passes after, written with Test Buddy's skill (the
  `tb` skill of the Test Buddy plugin where it is installed; otherwise its files read from
  `Elforama/testbuddy` at the commit pinned in `android-e2e.yml`), a `validates:` line, reading
  the verdict and acting on it, what counts as faking proof, and what to do when no flow can show
  the change (say so, say what was tried, give a test that fails without the change, leave the
  decision to the maintainer). Refactor, docs and CI-only PRs are out of scope.
- **Videos in the PR description** (`e2e-affected.yml`, the existing `comment` job). The
  before/after table, with both annotated videos as inline GIFs, links to the MP4s and the
  verdict, is written between `<!-- e2e-videos:start -->` and `<!-- e2e-videos:end -->` in the
  description on every run; only that block is replaced, and it is added at the end when the
  markers are missing and validating flows ran. The PR template now has the markers and an empty
  `validates:` line.
- **Before/after without a PR**: `flow-runner.yml` (by hand) takes `validate_flows` and
  `before_ref`, and `cloud-flows.sh run --validate "<flows>" [--before <sha>]` starts it and
  prints the verdict. The eval uses it; an agent can use it before opening a PR.
- **The eval**, in `.claude/skills/pr-validating-flow/evals/` (its README says how to run it).

## What was checked

On this task's PR (#1462), with CI's built-in token:

- The description block: CI wrote the table with both GIFs and MP4 links for the flow named on
  the PR's `validates:` line. **GIF mode is the one that was verified.** Real attachments
  (`gh pr edit --attach` with a maintainer's `E2E_ATTACH_TOKEN`) are not built: no such secret was
  available to test with.
- CI's edit of the description does not re-run "Check for screenshot & video" (an edit by the
  workflow's token starts no workflow). The check stayed red until the description was saved
  again by a user token, then passed. The skill tells the agent to make its last description
  edit after CI has filled the block.
- `cloud-flows.sh run --validate` on a branch with the real fix of #1447 and its flow:
  **validated** (fails on the base app at `SessionNoteField is visible`, passes on the fixed
  app), both videos recorded, about 10 minutes.

The eval's results are in `.claude/skills/pr-validating-flow/evals/results/README.md`.

## Open questions for maintainers

- An `E2E_ATTACH_TOKEN` for GitHub's real video player in the description, or stay with GIFs.
- "Check for screenshot & video" could count the `e2e-videos` block as soon as CI writes it (run
  it from the `comment` job, or re-check on `workflow_run`), so nobody has to save the
  description again. Left out here to keep the CI change small.
- The eval pushed branches under `skill-eval/` (listed in the results README): delete them.
