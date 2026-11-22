#!/usr/bin/env python3
"""Says whether each validating flow proves the PR's change, from its two runs: on the base
branch's app (DIR/before) and on the PR's app (DIR/after), both run-flows.sh output folders.

    before  after   verdict
    fails   passes  validated         the flow fails without the change and passes with it
    passes  passes  not-demonstrated  it passes without the change too
    fails   fails   broken            it fails on the PR's app
    passes  fails   broken            the PR makes a passing flow fail
    a side did not run (no base app, setup failed)   inconclusive

Writes DIR/verdict.json and DIR/verdict.md. A reason the base side could not run at all goes in
DIR/before-missing.txt. Exit status: 1 when a flow is broken, else 0.

Usage: verdict.py DIR
"""
import json
import os
import sys
from pathlib import Path

TEXT = {
    "validated": "✅ **validated**: fails without this change, passes with it",
    "not-demonstrated": "⚠️ **does not demonstrate the change**: passes without it too",
    "broken": "❌ **broken**: fails on this PR's app",
    "inconclusive": "❔ **inconclusive**",
}


def load(side_dir):
    try:
        flows = json.loads((side_dir / "summary.json").read_text())["flows"]
    except (OSError, ValueError, KeyError):
        return {}
    return {f["name"]: f for f in flows if isinstance(f, dict) and "name" in f}


def side(flow, folder):
    """What the comment needs of one run, with paths relative to DIR."""
    if flow is None:
        return None
    rel = lambda p: f"{folder}/{p}" if p else None  # noqa: E731
    error = (flow.get("error") or "").strip()
    return {
        "status": flow.get("status"),
        "seconds": flow.get("seconds"),
        "error": error.splitlines()[0][:300] if error else None,
        "ran": not error.startswith("Setup failed"),
        "video": rel(flow.get("video")),
        "recording": flow.get("recording"),
        "recordingError": flow.get("recordingError"),
        "screenshots": [rel(s) for s in flow.get("screenshots") or []][-2:],
    }


def judge(before, after, before_missing):
    """Returns (verdict, why): why only for inconclusive and for a PR that breaks a flow."""
    if after is None or not after["ran"]:
        return "inconclusive", "the flow did not run on this PR's app" + (
            f" ({after['error']})" if after and after["error"] else "")
    if before is None or not before["ran"]:
        why = before_missing or (before["error"] if before else None) or "no result"
        return "inconclusive", f"the flow did not run on the base app ({why})"
    b, a = before["status"] == "passed", after["status"] == "passed"
    if not b and a:
        return "validated", None
    if b and a:
        return "not-demonstrated", None
    return "broken", ("it passes on the base app: this PR makes it fail" if b else
                      "it fails on the base app too")


def main():
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    out = Path(sys.argv[1])
    before, after = load(out / "before"), load(out / "after")
    missing = out / "before-missing.txt"
    before_missing = missing.read_text().strip().splitlines()[0][:200] if missing.exists() and missing.read_text().strip() else None
    names = list(after) + [n for n in before if n not in after]
    flows = []
    for name in names:
        b, a = side(before.get(name), "before"), side(after.get(name), "after")
        verdict, why = judge(b, a, before_missing)
        flows.append({"name": name, "verdict": verdict, "why": why, "before": b, "after": a})
    counts = {v: sum(f["verdict"] == v for f in flows) for v in TEXT}
    sha = os.environ.get("FLOW_BEFORE_SHA") or None
    (out / "verdict.json").write_text(json.dumps(
        {"beforeSha": sha, "beforeMissing": before_missing, "counts": counts, "flows": flows}, indent=2) + "\n")

    cell = lambda s: "did not run" if s is None or not s["ran"] else (  # noqa: E731
        ("✅ passed" if s["status"] == "passed" else "❌ failed") +
        (" (recording lost)" if s["recording"] == "lost" else ""))
    md = ["### Before and after", "",
          "| Flow | Before (base app) | After (this PR's app) | Verdict |", "|---|---|---|---|"]
    for f in flows:
        md.append(f"| `{f['name']}` | {cell(f['before'])} | {cell(f['after'])} | "
                  f"{TEXT[f['verdict']]}{': ' + f['why'] if f['why'] else ''} |")
    for f in flows:
        for label, s in (("before", f["before"]), ("after", f["after"])):
            if s and s["error"]:
                md += ["", f"**`{f['name']}`** {label}: {s['error']}"]
    (out / "verdict.md").write_text("\n".join(md) + "\n")
    print("\n".join(md))
    sys.exit(1 if counts["broken"] else 0)


if __name__ == "__main__":
    main()
