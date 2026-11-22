#!/usr/bin/env python3
"""Writes summary.json and summary.md for a run-flows.sh output folder.

summary.json lists each flow with its status, time, error, screenshots (in step order), video and
debug files, all as paths relative to the folder, plus the run's details from the environment
(GITHUB_* and FLOW_RUN_*), so an agent can read one file and open what it needs.

A flow whose video was asked for has "recording": "saved", "partial" (tb kept what the device had
when stopping it failed) or "lost" (no video, with tb's reason in "recordingError"), and "tries",
the times the flow ran to get it. "recordingsLost" counts the lost ones.

Usage: summarize.py OUT_DIR
"""
import json
import os
import re
import sys
from pathlib import Path


def video_parts(out, name):
    """videos/<name>-part2.mp4, ... in order (not another flow's, like <name>-partial.mp4)."""
    pattern = re.compile(re.escape(name) + r"-part(\d+)\.mp4$")
    found = [(int(m.group(1)), p) for p in (out / "videos").glob("*.mp4") if (m := pattern.match(p.name))]
    return [p.relative_to(out).as_posix() for _, p in sorted(found)]


def main():
    out = Path(sys.argv[1])
    flows = []
    tsv = out / "results.tsv"
    for line in tsv.read_text().splitlines() if tsv.exists() else []:
        # More columns from run-flows.sh: the top-level steps it ran when it cut the flow, then,
        # when a video was asked for, what became of it, the tries it took and tb's reason.
        name, file, status, seconds, *rest = line.split("\t")
        rest += [""] * (4 - len(rest))
        cut = int(rest[0]) if rest[0].isdigit() else None
        recording = rest[1] if rest[1] in ("saved", "partial", "lost") else None
        d = out / "flows" / name
        shots = sorted(p.relative_to(out).as_posix() for p in (out / "screens" / name).glob("*.png"))
        # Failure screenshots last, after the steps.
        shots.sort(key=lambda p: Path(p).name.startswith("failure"))
        error = (d / "error.txt").read_text().strip() if (d / "error.txt").exists() else None
        video = out / "videos" / f"{name}.mp4"
        captions = out / "videos" / f"{name}.srt"
        flows.append({
            "name": name,
            "file": file,
            "status": status,
            "seconds": int(seconds),
            "steps": cut,
            "error": error,
            "screenshots": shots,
            "video": video.relative_to(out).as_posix() if video.exists() else None,
            "captions": captions.relative_to(out).as_posix() if captions.exists() else None,
            # A recording whose parts could not be joined (join_recording.py): part 2 on.
            "videoParts": video_parts(out, name),
            "recording": recording,
            "recordingError": (rest[3] or None) if recording in ("partial", "lost") else None,
            "tries": int(rest[2]) if rest[2].isdigit() else 1,
            "files": sorted(p.relative_to(out).as_posix() for p in d.iterdir() if p.is_file()),
        })
    env = os.environ.get
    server, repo, run_id = env("GITHUB_SERVER_URL", ""), env("GITHUB_REPOSITORY", ""), env("GITHUB_RUN_ID", "")
    summary = {
        "name": env("FLOW_RUN_NAME") or out.name,
        "ref": env("FLOW_RUN_REF"),
        "sha": env("FLOW_RUN_SHA"),
        "pr": int(env("FLOW_RUN_PR")) if (env("FLOW_RUN_PR") or "").isdigit() else None,
        "apiLevel": env("FLOW_RUN_API_LEVEL"),
        "runId": run_id or None,
        "runUrl": f"{server}/{repo}/actions/runs/{run_id}" if run_id else None,
        "passed": sum(f["status"] == "passed" for f in flows),
        "failed": sum(f["status"] != "passed" for f in flows),
        "recordingsLost": sum(f["recording"] == "lost" for f in flows),
        "flows": flows,
    }
    (out / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")

    icon = {"passed": "✅", "failed": "❌"}
    md = [f"### Flow run: {summary['passed']} passed, {summary['failed']} failed", ""]
    if summary["sha"]:
        md += [f"App at `{summary['sha'][:10]}`, API {summary['apiLevel'] or '?'}.", ""]
    lost = summary["recordingsLost"]
    if lost:
        md[0] += f", {lost} recording{'s' if lost > 1 else ''} lost"
    videos = any(f["recording"] for f in flows)
    video_cell = {"saved": "🎬 saved", "partial": "⚠️ incomplete", "lost": "⚠️ **lost**"}
    md += ["| Flow | Result | Time | Screenshots |" + (" Video |" if videos else ""),
           "|---|---|---|---|" + ("---|" if videos else "")]
    for f in flows:
        label = f"`{f['name']}`" + (f" (steps 1–{f['steps']})" if f["steps"] else "")
        row = f"| {label} | {icon.get(f['status'], '')} {f['status']} | {f['seconds']}s | {len(f['screenshots'])} |"
        if videos:
            tries = f" (try {f['tries']})" if f["tries"] > 1 else ""
            row += f" {video_cell.get(f['recording'], '—')}{tries} |"
        md.append(row)
    for f in flows:
        if f["error"]:
            first = f["error"].splitlines()[0][:300]
            md += ["", f"**`{f['name']}`** failed: {first}"]
    for f in flows:
        if f["recording"] in ("lost", "partial"):
            what = "has no video: the recording was lost" if f["recording"] == "lost" else "may have an incomplete video"
            tries = f" after {f['tries']} tries" if f["tries"] > 1 else ""
            md += ["", f"**`{f['name']}`** {what}{tries} ({f['recordingError'] or 'no reason given'})."]
    (out / "summary.md").write_text("\n".join(md) + "\n")
    print("\n".join(md))


if __name__ == "__main__":
    main()
