# Task 114 - Plan: annotated videos of the flows a PR affects

Status: plan only, nothing implemented yet.

## Goal

1. A new CI job on every PR that runs, with Test Buddy v0.6.0 and annotated video
   (`tb test --record --annotate`: each tap held about a second, a ring where it landed, a caption band
   naming the step), only:
   - flows the PR adds or changes (`e2e/flows/**`), and
   - flows that reach code the PR changed, even when no flow changed (a padding or color change on
     a screen still passes, but a reviewer wants to see it).
2. Isolation: a screen shared by many flows (38 of the 42 flows reach the Dashboard) must not run
   38 flows. Run one flow per changed screen, and only as far as it needs to show that screen.
3. The existing "Android end-to-end" check (`android-e2e.yml`, every flow, no video) stays as it is,
   apart from moving to v0.6.0 with no `--annotate`.

## What already exists (reuse, don't rebuild)

- `flow-runner.yml`: reusable workflow (`workflow_call`) that builds the app and tb (cached by
  commit), boots the cached API 35 snapshot, runs flow selectors through `e2e/ci/run-flows.sh`,
  uploads an artifact, pushes results to `flow-results/pr-<N>` and writes `summary.md`.
- `flow-runner-pr.yml`: keeps one summary comment per PR up to date.
- `run-flows.sh`: runs each flow alone, seeds org-config and dev-tools flows, records video today
  with its own `adb screenrecord` loop (`RECORD=1`).
- `step_shots.py`: already rewrites a flow's top-level steps safely (column-0 `- ` lines), so a
  "cut the flow after step N" tool is the same kind of change.
- Every screen element has a stable id in `view/TestTags.kt`, and the flows find elements by those
  ids, so the ids are a shared vocabulary between code and flows.

## Test Buddy v0.6.0 facts the plan depends on

- Tag `v0.6.0` = commit `8220d90ca0ff220f9614da00baab9ee88ec0dceb` (both workflows pin
  `ab598c2…` = v0.5.2 today).
- `--annotate` is a flag, not a command: `tb test <flow> --record --annotate[=TIME]`
  (or `tb replay ... --record --annotate`). It needs `ffmpeg` on PATH (or `TB_FFMPEG`); without it
  tb keeps the raw recording and logs why. It also writes `recording.srt` next to the video.
- `--timeline` writes an HTML step-by-step page per flow; cheap to add to the same job.

## Shared parts (needed by all three options)

1. **Bump tb to v0.6.0** in `android-e2e.yml` and `flow-runner.yml` (`TESTBUDDY_REF`). No flag
   changes in `android-e2e.yml`. Check the flows still pass there before the new job lands (the
   0.5.x → 0.6.0 change may move timings).
2. **`annotate` input on `flow-runner.yml`** → `ANNOTATE=1` in `run-flows.sh`, which then passes
   `--record --annotate` to `tb test` and skips its own `screenrecord` loop (tb's recording is what
   gets the captions). Copy tb's video into `videos/<flow>.mp4` so `summary.md` / the results branch
   keep working. ffmpeg is already installed there for `--video`.
3. **New workflow `e2e-affected.yml`** on `pull_request` (master, release*):
   - job `select` (ubuntu, no emulator, ~1 min): `git diff --name-only origin/<base>...HEAD`,
     runs the selector (`e2e/ci/affected.py`, below), writes `flows` and a "why" table to the job
     summary. Outputs an empty list when nothing UI-facing changed.
   - job `annotated` (`if: needs.select.outputs.flows != ''`): `uses: ./.github/workflows/flow-runner.yml`
     with `flows`, `pr`, `annotate: true`, `screenshots: none`.
   - Non-blocking at first (not a required check): a flaky video run should not hold up merges;
     `android-e2e.yml` remains the gate.
   - Same PR comment as `flow-runner-pr.yml`, with a "Selected because" column
     (`settings-logout`: changed flow; `capture-first-tree`: `dashboard/DashboardScreen.kt` → `TrackButton`).
4. **Isolation by cutting flows.** For a flow chosen only because of a changed screen (not because
   the flow itself changed), write a copy that stops a few steps after the last step that touches
   that screen's ids (end on an `assertVisible` + `takeScreenshot`), like `step_shots.py` does. The
   Dashboard example then runs `signup` → Dashboard → stop, not a full capture.
5. **Caps.** At most N flows (say 6) per PR; past that, pick by priority and list the rest as
   "skipped, over the cap" in the summary. Changed flows always run.
6. Docs: `docs/testing/ci-flows.md` section and this task folder.

The three options differ only in how the selector turns changed files into flows.

## Option 1 - Hand-written impact map

`e2e/impact-map.yaml`, kept by hand:

```yaml
screens:
  dashboard:
    paths: [app/src/main/java/**/dashboard/**]
    flow: capture-first-tree          # the one flow to run
    stop_after: TrackButton           # cut after the last step on this id
  settings:
    paths: [app/src/main/java/**/settings/**, app/src/main/java/**/profile/**]
    flow: settings-profile-privacy
  ...
global:                               # theme, shared components, strings
  paths: [app/src/main/java/**/theme/**, app/src/main/res/values*/**]
  flow: capture-first-tree
```

Selector: glob-match changed files → screens → their one flow. Unmatched app files → nothing (or
`first-screen`).

- Pros: simple, quick to build (a day), fully predictable, a reviewer can read why a flow ran.
- Cons: goes stale as screens move or are added (KMP/CMP move will reshuffle packages); someone has
  to pick each screen's flow; shared composables (`view/`, `theme/`) need hand rules.
- Guard: a CI lint that fails when a package under `app/src/main/java/.../TreeTracker/` that has a
  `@Composable` has no entry.

## Option 2 - Derived from test tags and the code graph (recommended)

No hand-kept map; built on each run from the code and the flows.

1. **Flow index** (from `e2e/flows/**`, following `runFlow` into subflows): for each flow, the
   ordered list of ids it uses, with step numbers. This says "flow X is on the screen with
   `TrackButton` from step 7 to step 12".
2. **Code index** (from `app/src/main`): which files use which `TestTags.*` (and the older
   `*_TAG` constants). A file that uses tags is a *screen file*; its tags are its *screen ids*.
3. **Changed file → screen ids:**
   - screen file: its own ids;
   - other Kotlin file (ViewModel, shared composable, helper): walk the reverse reference graph
     (who imports / names its class or functions, by simple grep on package + name) up to the
     nearest screen files, max 2–3 hops. `DashboardViewModel.kt` → `DashboardScreen.kt`;
     `view/CustomDialog.kt` → every screen that shows a dialog;
   - `res/` files: `R.string.x`, `R.drawable.x`, `R.color.x` usages → screen files;
   - beyond the hop limit, or a data / DI / sync file with no screen in reach: no flow
     (said so in the summary), or a configurable fallback flow.
4. **Screen ids → flows, one per screen:** among flows that use any of the screen's ids, pick the
   one that reaches it with the fewest steps (ties: fewest total steps, then name). Then cut it a
   few steps after its last step on that screen (shared part 4). A set cover across all changed
   screens prefers one flow that shows several of them.
5. Flows the PR added or changed run whole, and count as covering the screens they reach, so a
   PR that changed `settings-logout.yaml` and `SettingsScreen.kt` runs one flow, not two.
6. `e2e/impact-overrides.yaml` (small, optional): pin a screen to a flow, add rules for things the
   graph can't see (deep links, `OrgRepo` flow config → org-config flows), or ignore paths.

- Pros: stays correct as screens and flows change; picks the cheapest path automatically; the
  "why" chain (file → screen file → id → flow, step) is printable; same index can later power a
  `cloud-flows.sh affected` command for sessions.
- Cons: more code (~2–3 days for `affected.py` + tests); grep-based reference graph can over- or
  under-reach for shared helpers (hop limit and overrides handle it); screens without test tags are
  invisible (every current screen has them).

## Option 3 - Recorded runtime coverage

Learn what each flow touches by running it.

1. A nightly job on master runs all flows with an instrumented build: either JaCoCo on the debug
   build (per-flow `.ec` dump through a debug broadcast) or, lighter, a debug-only log line per
   composed screen/route (`Nav3` back stack changes, `TestTags` roots) that `run-flows.sh` maps to
   tb's step times.
2. Store `coverage-map.json` (flow → step ranges → classes/files and routes) on a `e2e-coverage`
   branch.
3. PR selector: changed files → map lookup → flows and step ranges; pick the shortest per screen,
   cut as in shared part 4.

- Pros: most accurate, including logic changes that no test tag reaches (ViewModel branches,
  repositories) and real file-level coverage.
- Cons: biggest build (JaCoCo on an emulator, per-flow dumps, a nightly job, a storage branch);
  map is a day stale and blind to new code until the next nightly; a renamed file loses its
  history; JaCoCo slows the app, which the timing-sensitive flows (double tap, process death) may
  not like.

## Recommendation

**Option 2**, with the overrides file from option 1 as the escape hatch.

It matches how the flows are already written (by `TestTags` id), needs no extra runtime
instrumentation, keeps itself current as screens move during the KMP/CMP work, and handles the
Dashboard case directly: a `DashboardScreen.kt` change maps to Dashboard ids, the cheapest flow that
reaches them is picked, and it is cut right after the Dashboard, so one short annotated video comes
back instead of 38. Option 3 can be added later as a second input to the same selector if
non-UI changes need videos too.

## Steps (option 2)

1. Bump `TESTBUDDY_REF` to v0.6.0 in `android-e2e.yml` and `flow-runner.yml`; run the full suite
   once to confirm it still passes.
2. `annotate` input in `flow-runner.yml` + `ANNOTATE=1` in `run-flows.sh`; try it with
   `cloud-flows.sh run --video first-screen` style run on a branch.
3. `e2e/ci/affected.py`: flow index, code index, reverse graph, selection, cutting; `--explain`
   prints the why table. Unit tests in `e2e/ci/test_affected.py` over fixture diffs
   (Dashboard color change → one cut flow; flow-only change → that flow; DI change → none;
   `strings.xml` change → screens using the changed keys).
4. `e2e/impact-overrides.yaml` with the first known gaps (org-config, deep link).
5. `.github/workflows/e2e-affected.yml` (`select` + `annotated` jobs), non-blocking.
6. PR comment with videos and the "selected because" column; docs in `ci-flows.md`.
7. After a few weeks of PRs, review the selections and the cap; decide whether to make it required.

## Open questions

- Cap on flows per PR (6?) and on video length per flow.
- Should PRs from forks run it (no write token for the results branch; artifact only)?
- Should a non-UI change (data, sync) run a fallback flow or nothing?
- Make `annotated` a required check later, or keep it informational?
