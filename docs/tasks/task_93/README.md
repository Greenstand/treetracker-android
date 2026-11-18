# Task 93 - Before/after videos of e2e flows on CI

Cloud agent sessions have no emulator, so they can't record the before/after videos a bug fix PR
needs. This adds a CI workflow that records them.

## What was added

- `e2e/record.sh`: runs each given flow on its own with `adb screenrecord` running, and writes
  `<flow>.mp4`, the tb report and debug files, and `results.txt` (one `name passed|failed` line per
  flow). Runs offline like `run.sh`. A failing flow doesn't fail the script.
- `.github/workflows/e2e-video.yml`: on PRs that touch `e2e/flows/`, records the added or changed
  flows twice, on master's app (`git restore --source=<base>` of everything but `e2e/` and
  `.github/`) and on the PR's app. Uploads `e2e-video-before` and `e2e-video-after` and writes a
  results table to the run summary. A PR that only changes the recorder records every
  `known-bug` flow. Also runs by hand with a list of flows and a base ref.
- `e2e/README.md`: "Before/after videos" section.

## Use in bug fix PRs

Each fix for #1415 to #1423 drops `known-bug` from its flow, so the workflow records that flow
failing on master and passing on the branch. Link both artifacts in the PR description.

## Bug sessions

| Bug | Flow | Task |
|---|---|---|
| #1415 Back from Dashboard returns to Signup after logout | `settings-logout-back.yaml` | task_94 |
| #1416 Deleting a user offline crashes | `settings-delete-user-offline.yaml` | task_95 |
| #1417 Location denied leaves app on splash | `sad-location-denied.yaml` | task_96 |
| #1418 Process death on Add org crashes | `sad-process-death-setup.yaml` | task_97 |
| #1419 Process death on image review crashes | `sad-process-death-review.yaml` | task_98 |
| #1420 Bad GPS dialog after process death on capture | `sad-capture-after-process-death.yaml` | task_99 |
| #1421 Next after camera rationale cancel crashes | `sad-camera-rationale-next.yaml` | task_100 |
| #1422 Back from Add org skips Add org | `sad-back-add-org.yaml` | task_101 |
| #1423 Double-tap Next skips Add org | `sad-double-tap-setup.yaml` | task_102 |

## Inline videos

GitHub plays a video inline only when it's uploaded from the browser; there's no API for it, so
neither `gh` nor a workflow can do it. The `publish` job in `e2e-video.yml` instead converts each
recording to a GIF (double speed, 8 fps, 270px wide; about 1.3 MB for a minute), pushes GIFs and
MP4s to the `e2e-media` branch under `pr-<number>/<sha>/`, and writes a before/after table into the
PR description between `<!-- e2e-videos:start -->` and `<!-- e2e-videos:end -->`. The GIFs render
inline through raw.githubusercontent.com; the MP4 links are full speed. Fork PRs are skipped
because their token can't push or edit the PR.
