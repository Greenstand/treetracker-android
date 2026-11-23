# Task 117 - The media check passes without anyone re-saving the PR description

## Problem

Task 116's skill told agents to save the PR description again after CI had written the
before/after videos into it, because an edit by the workflow's token does not re-run "Check for
screenshot & video". On the first PR with a validated flow (#1444) that turned out to destroy the
videos: a PR description saved from a Claude Code cloud session comes back with its media links
rewritten (`<img` as `&lt;img`, URLs wrapped in backticks), through the GitHub tools and through
`gh api` alike. The check then can't go green, and the videos are gone until "Affected flows"
runs again. It was missed on #1462 because CI rewrote the block after each save there.

## Change

- `e2e-affected.yml`, `comment` job: after writing the block it looks up "Check for screenshot &
  video" on the PR's head commit and, when that is not green, re-runs it (`actions: write`,
  `checks: read` on that job only).
- `require-pr-media.yml`:
  - reads the PR as it is now, through the API, not the event's copy: a re-run gets the first
    run's event again;
  - ignores HTML comments, so the template's own hint ("Drag a ... (.mp4 / .mov / .webm) here") is
    no longer taken for a video;
  - a description with a line `No validating flow: <why>` passes without a video once a
    maintainer adds the `no-validating-flow` label. Without the label the check says it is
    waiting for that decision. (The label keeps the decision with a maintainer, as the skill
    asks; dropping the label condition is a one-line change if maintainers prefer the statement
    alone to pass.)
- `SKILL.md`, the PR template, `docs/testing/ci-flows.md`: the re-save step is gone. The
  description is written once, up front; nobody writes between the markers; a description that
  holds CI's media is not saved again from a cloud session; a later edit is followed by a push or
  a re-run of "Affected flows", which writes the block afresh.
- Tests, run by "skill tests": `e2e/ci/test_require_pr_media.py` (the check's script under node:
  empty block, CI's block, a re-run with a stale event, the `No validating flow` cases, the
  template alone) and a new case in `test_description_block.py` (the re-run is asked for only
  when the check is completed and not green; a refused re-run is a warning).

## What was checked, and what could not be

On a real PR, #1465 (closed; the fix of #1441 replayed onto the code before it, with this task's
workflows on its base branch):

- "Affected flows" gave the flow the verdict **validated** and wrote both videos into the
  description. The description was never saved by the session after it was opened, and the
  `<img src="https://raw.githubusercontent.com/...">` links in it are intact.
- The `comment` job then **re-ran "Check for screenshot & video" with the workflow's own token**
  (run 45107108927, attempt 2, started by `github-actions[bot]`).
- That re-run still failed, and this is the part that can't be shown before merge: GitHub runs
  `pull_request_target` workflows from the **default branch's** copy of the file, so #1465 ran
  master's `require-pr-media.yml`, which reads the event's (old) description. The new script,
  run locally under node against #1465's live description with the event's description as it was
  at open, passes; against the description as opened it fails. So the two halves are each shown
  on real data, and the first PR after this merges is the end-to-end check.

Also not checked: a PR that was opened before this merges keeps master's old check for its
existing runs; it picks up the new one on its next push or description edit.

## Left for a maintainer

- Create the `no-validating-flow` label (or decide the statement alone should pass).
- Delete `release-proof-117` and `claude/proof-117-replay-1441` (the proof PR's branches; the
  session's push to delete them was refused), and the `skill-eval/*` branches from task 116.
- "test and assemble" fails now and then on dependency downloads (429 from Maven Central):
  `pull_request.yml` has no Gradle cache.
