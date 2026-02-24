# Task Workflow

Use this workflow for all agent work.

## 1. Create a Task Folder

- Parent directory: `docs/tasks/`
- Folder name format: `task_{id}`
- `id` rule: use the next integer after the highest existing `task_{id}` folder
- Start at `task_0` when no task folders exist

## 2. Log the Work

Each task folder should include:

- `log.md`: running notes of what was done and why it was done
- `summary.md`: concise summary for future reference

`log.md` should capture:

- context and objective
- implementation steps and rationale
- important decisions and tradeoffs
- validation or checks performed

## 3. Finalize After PR Activity

When a PR is created or merged, update `summary.md` with:

- PR status (opened or merged)
- high-level change summary
- key decisions and outcomes
- follow-up notes for future work
