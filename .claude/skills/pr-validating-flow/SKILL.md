---
name: pr-validating-flow
description: Proof for a bug-fix or feature pull request in treetracker-android. Makes the PR carry a new Test Buddy (tb) end-to-end flow under e2e/flows/ that fails on the app before the change and passes after it, recorded with tb's --annotate option, with the before and after videos in the PR description, and says what to do when no flow can show the change. Use this whenever you fix a bug, implement a feature or change app behaviour in this repo and will open, update or finish a pull request for it ("fix issue #1416", "implement this and open a PR", "the PR needs a video", "add a regression flow", "why does the PR say 'does not demonstrate the change'"), even when nobody mentions flows, videos or proof. Load it before you write the fix, since the flow has to fail first. Not for PRs that only refactor, change docs, or change CI or build files.
---

# A validating flow for every fix and feature PR

A reviewer of a fix or feature PR here wants to see two things without checking out the branch:
the app misbehaving before the change and behaving after it. A **validating flow** shows both. It
is one new YAML flow that drives the real UI, **fails on the app at the PR's merge base and passes
on the PR's app**, and then stays in `e2e/flows/` as the regression test for that behaviour. CI
runs it on both apps, records both runs with `tb test --record --annotate` (a ring on every tap, a
caption naming every step), judges the pair in pure code and puts both videos in the PR
description.

What the PR must end up with:

1. One new flow in `e2e/flows/` that asserts the fixed (or new) behaviour through the UI.
2. A line `validates: <flow-name>` in the PR description.
3. The verdict **validated** for that flow, and both videos in the description (CI writes them).
4. Or, when no flow can show the change, a plain statement of that with other proof
   ([No flow can show it](#when-no-flow-can-show-the-change)).

Out of scope: PRs that only refactor, change docs, or change CI or build files. They change no
behaviour a flow could fail on, so don't invent one. Tick the template's "no user-visible / UI
effect" box and say what was checked instead.

## 1. Use Test Buddy's own skill to write the flow

Flows are written and debugged with Test Buddy (`tb`), and its skill is the reference for flow
syntax, selectors, waits, `tb look`, `tb check --lint` and failure diagnosis. Load it and follow
it; this file repeats none of it.

**Do this before you write any YAML, every time**, also when a neighbouring flow looks close
enough to copy. A neighbouring flow shows what worked on its screens, not why: which waits a
step already does, how `killApp` and `launchApp` behave, when a selector is ambiguous. A copied
flow that is subtly wrong costs a 15-minute CI run to find out.

- If your session lists a skill named `tb` (or `test-buddy:tb`, from the Test Buddy plugin), load
  that.
- Otherwise (Claude Code cloud sessions don't have the plugin) read the same files from source,
  at the commit CI builds tb from, so the docs match the tb that will run your flow:

  ```bash
  REF=$(grep -m1 -o 'TESTBUDDY_REF: [0-9a-f]*' .github/workflows/android-e2e.yml | cut -d' ' -f2)
  git clone -q https://github.com/Elforama/testbuddy /tmp/testbuddy && git -C /tmp/testbuddy checkout -q "$REF"
  ls /tmp/testbuddy/plugin/skills/tb    # SKILL.md first, then flows.md and gotchas.md; debugging.md when a run fails
  ```

What is specific to this repo, on top of that skill:

- **Read `e2e/README.md`** and two or three flows near yours before writing. Ids come from
  `TestTags` (`app/src/main/java/org/greenstand/android/TreeTracker/view/TestTags.kt`); a control without a tag gets one in your PR.
- **Reuse the sub-flows** in `e2e/flows/subflows/` (`signup.yaml`, `start-capture.yaml`,
  `org-capture-setup.yaml`, ...) instead of copying their steps.
- **The emulator is offline and the app has dummy keys**, in CI and in `e2e/run.sh`. A flow can't
  depend on a server answer, a sync that succeeds, or Remote Config.
- **Org-configured behaviour** (session note, wallet select, tree height, ...): the org is seeded,
  not fetched. Name the flow `org-<something>.yaml`, tag it `org-config`, start it with a plain
  `launchApp`, and add a `<variant> <file>` line to the list in `e2e/run-org.sh`; that line is
  what makes every runner seed the org with `e2e/seed-org.sh` before the flow. A new kind of org
  is a new variant in `seed-org.sh`.
- **State that is not the thing under test** (a signed-in user, extra users, saved trees): on an
  emulator of your own, the `agent-app-functions` skill (`.claude/skills/agent-app-functions/`)
  sets it up in a second while you explore. A committed flow can't call it; it either signs up
  through `subflows/signup.yaml` or is tagged `dev-tools`, which makes the runners seed one admin
  and two trees with `e2e/setup-device.sh` first.
- **Tags**: every flow has a `tags:` line (`settings`, `capture`, `signup`, `sad-path`,
  `org-config`, ...). Never tag a validating flow `known-bug`: that tag means "expected to fail",
  and CI then skips it.
- The header comment says what behaviour the flow guards and the issue number, because the flow
  outlives the PR.

## 2. Make it fail first, for the right reason

Write the flow before the fix, or at least judge it against the unfixed app. Its last steps
assert what the user sees only when the change works: the dialog that used to be a crash, the
screen that used to be skipped, the value that used to be lost. Check three things:

- **It fails without the change at the step that shows the bug.** A flow that fails earlier
  (a wrong id, a seeding error) fails "before" too, and reads as proof when it is not. For a
  feature, failing at the first control the feature adds is the fair reason.
- **It passes with the change, every time.** One run on each app decides the verdict, so a flow
  that passes two times in three will mislead somebody.
- **It asserts through the UI**, at the end of a path a user can take. `launchApp`, `tapOn`,
  `inputText`, `back`, `pressKey: Home` + `killApp` (process death) and the like.

Where the flow runs depends on whether your session has an emulator. Check, don't assume:

```bash
adb devices 2>/dev/null | grep -w device || echo "no emulator here: use CI"
```


- **With an emulator** (your own machine): `./gradlew :app:installDebug`, then follow the tb skill
  (`tb test e2e/flows/<name>.yaml`). For both apps and the verdict in one go, see "Locally" under
  "Before and after" in `docs/testing/ci-flows.md` (`e2e/ci/run-job.sh` with `BEFORE_APK` and
  `AFTER_APK`).
- **Without one** (a cloud session has no emulator): CI is your emulator. Push the branch, then

  ```bash
  bash e2e/ci/cloud-flows.sh run --validate <flow-name>      # before = merge base with origin/master
  cat build/flow-results/validate/verdict.md
  ```

  or open the PR as a draft and let its own check run (next section). Results come back as files:
  `docs/testing/ci-flows.md` says where the screenshots, `console.txt`, `screen.txt` and
  `error.txt` of each side are. Read the failing step of the "before" run, not only its status.

## 3. The PR description, the verdict and the videos

Use `.github/pull_request_template.md`. In it:

```markdown
validates: <flow-name>

<!-- e2e-videos:start -->
<!-- e2e-videos:end -->
```

The `validates:` line is how CI knows which flow is the proof (a flow the PR adds is also picked
up on its own, but the line makes the claim explicit and survives a rename). Put the two marker
lines under "Screen recording / video". Don't record or upload videos yourself.

On every push, the "Affected flows (annotated video)" check (`.github/workflows/e2e-affected.yml`)
builds the app at the merge base and the PR's app, runs the flow on each with
`tb test --record --annotate`, and writes a table with both videos (inline GIFs, links to the
MP4s) and the verdict between the markers and in a PR comment. It takes about 15 minutes. Read
the result (the PR comment, or `bash e2e/ci/cloud-flows.sh fetch --affected <PR>` then
`build/flow-results/validate/verdict.md`) and act on it:

| Verdict | Meaning | What you do |
|---|---|---|
| ✅ validated | fails before, passes after | Check the "before" error is the bug itself. Then done. |
| ⚠️ does not demonstrate the change | passes before too | The flow is **not proof**. It doesn't reach the bug, or asserts something that was already true. Fix the flow, or go to the next section. Never leave it presented as validation. |
| ❌ broken | fails on the PR's app | The fix or the flow is wrong. Read `error.txt` and `screen.txt` of the "after" run. |
| ❔ inconclusive | a side did not run | Read the reason (base app did not build, seeding failed, recording lost) and fix or re-run. |

The description is written once, when you open the PR, and then left to CI:

- **Write all of it up front**, including one or two sentences under the markers saying what
  each video will show ("Before: the app closes when Bob is deleted offline. After: 'Account
  Deleted' shows and Settings comes back"). You know this before CI runs: it is what the flow's
  last steps assert.
- **Never write between the markers**, and **don't save the description again once CI has put
  videos in it**. From a Claude Code cloud session, saving a PR description rewrites the media
  links in it (`<img` becomes `&lt;img`, URLs get wrapped in backticks; seen on #1444), with the
  GitHub tools and with `gh api` alike, even when you send back exactly what you read. The videos
  are then gone until CI runs again.
- You don't need to save it: after CI fills the block it re-runs "Check for screenshot & video"
  itself, and that check reads the description as it is then.
- If the description has to change after that (a wrong sentence, a new `validates:` line), make
  the edit and then push a commit, or re-run the "Affected flows (annotated video)" check: every
  run writes the block again from scratch, which also repairs a block that got garbled. The
  `validates:` line is only read when that check runs.

## The proof has to be real

A validating flow is evidence, and the maintainers merge on it. So:

- **Don't weaken an assertion** to get a pass, and don't end the flow before the step that shows
  the behaviour.
- **Don't edit an existing flow's assertions** so that it passes with your change. If an existing
  flow now fails, either your change broke something or the intended behaviour changed: say which
  in the PR.
- **Don't add anything to the app for the flow's sake** except test tags: no sleeps or delays, no
  debug-only screens, buttons or text, no test hooks that make the bug easier to hit.
- **Don't present a flow that passes on both apps as validation**, and don't make "before" fail
  artificially (asserting an id only your PR adds, when the PR is a fix and not a feature).
- A flow added only to keep around (it passes before and after) is fine if you say so: leave it
  off the `validates:` line and call it a regression flow, not proof.

## When no flow can show the change

Some changes have nothing on screen for a flow to assert. From this repo's history: a logout that
races a database read by milliseconds, a capture session left open after a permission dialog,
state that is wrong in preferences but that no screen reads, a double-tap guard with a 300 ms
window. Forcing a flow onto these gives a video that proves nothing.

Before deciding that, try, and write down what you tried:

1. **Look for a consequence a user can see.** Follow the wrong state forward: does a later screen
   read it (after logout, relaunch, the next capture, user select)? Many "internal" bugs surface
   two screens later.
2. **Check it is reachable on the offline emulator** with seeded state: no server answer, no
   successful sync, no second device.
3. **Check a driven UI can hit it reliably.** `killApp` makes process death exact; a race that
   needs two events inside a few milliseconds is not something tb's taps can promise.
4. If one candidate flow exists, run it on both apps. "Passes before too" is the answer.

If nothing holds up, say so in the PR and give other proof:

- Under "Screen recording / video", write a line that starts `No validating flow: <one sentence
  why>` (the check looks for exactly that start), then the list of what you tried. No
  `validates:` line.
- Add a unit, Robolectric or instrumented test that **fails without the change and passes with
  it**, name it in the description, and show the failing run (the test's failure message from a
  commit or local run without the fix) next to the passing one.
- Don't attach a recording of some nearby flow to satisfy the template: it shows that nothing
  broke, not that the fix works, and reads as proof when it is not.
- Whether the PR can merge without a validating flow is the maintainer's call, so ask for it in
  the description instead of deciding it yourself. "Check for screenshot & video" stays red on
  such a PR, saying it waits for that decision, until a maintainer adds the
  `no-validating-flow` label. Don't add the label yourself.

## Checklist before asking for review

- [ ] Test Buddy's skill was loaded (or its files read at the pinned commit) before the flow was
      written.
- [ ] New flow in `e2e/flows/`, tagged, not `known-bug`, with a header comment; `run-org.sh` line
      if it is an org flow.
- [ ] `validates:` line and the `e2e-videos` markers in the description.
- [ ] Verdict is **validated**, and the "before" run failed at the step that shows the bug.
- [ ] Both videos are in the description (CI put them there; you did not edit the description
      after that), with a sentence on what each shows.
- [ ] No assertion weakened, no existing flow's assertions changed, nothing added to the app for
      the test but test tags.
- [ ] Or: a `No validating flow: ...` line, what was tried, a test that fails without the change,
      and the decision left to a maintainer (the `no-validating-flow` label is theirs to add).
