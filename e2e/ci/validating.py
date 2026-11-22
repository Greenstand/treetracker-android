#!/usr/bin/env python3
"""Picks the flows that are meant to prove a PR's change: e2e-affected.yml runs each on the base
branch's app and on the PR's app (docs/testing/ci-flows.md, "Before and after").

Two sources, in this order, at most --max flows:

1. A line in the PR description, `validates: <flow> [<flow> ...]` (a flow's name, with or without
   .yaml or the e2e/flows/ folder; spaces or commas between them). Only the first such line counts.
2. Flows the PR adds or edits in e2e/flows (not sub-flows), unless they are still tagged
   known-bug: such a flow is expected to fail on the PR's app too.

The description is text anyone can write: a word is only ever used as a flow's name, it must be
made of letters, digits, - and _, and the flow must exist in the checkout. Anything else is
refused and listed.

Prints the flows' paths on one line; --json and --md write the reasons.

Usage: validating.py --base REF [--head REF] [--body-file FILE] [--max 3] [--root DIR]
                     [--json FILE] [--md FILE]
"""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import flowfile  # noqa: E402

LINE = re.compile(r"^[\s>*-]*validates:(.*)$", re.I)
NAME = re.compile(r"^(?:e2e/flows/)?([A-Za-z0-9_-]+)(?:\.yaml)?$")
FLOWS = "e2e/flows"


def named(body):
    """The words of the description's first `validates:` line, or [] without one."""
    for line in body.splitlines():
        m = LINE.match(line.rstrip("\r"))
        if m:
            return [w for w in re.split(r"[\s,]+", m.group(1).strip()) if w][:20]
    return []


def changed(root, base, head):
    """Flows (not sub-flows) the PR adds or edits, as paths."""
    out = subprocess.run(["git", "-C", str(root), "diff", "--name-only", "--diff-filter=AM", "-z",
                          f"{base}...{head}", "--", FLOWS],
                         check=True, capture_output=True, text=True).stdout
    return sorted(p for p in out.split("\0") if re.fullmatch(FLOWS + r"/[A-Za-z0-9_-]+\.yaml", p))


def pick(root, body, changed_flows, limit):
    """Returns (picks, skipped): picks are (path, why); skipped are (word or path, why)."""
    root = Path(root)
    picks, skipped, seen = [], [], set()

    def add(path, why):
        if path in seen:
            return
        seen.add(path)
        if len(picks) >= limit:
            skipped.append((path, "over the limit of %d flows" % limit))
        else:
            picks.append((path, why))

    for word in named(body):
        m = NAME.match(word)
        if not m:
            skipped.append((re.sub(r"[^ -~]", "?", word)[:60], "not a flow's name"))
            continue
        path = f"{FLOWS}/{m.group(1)}.yaml"
        if not (root / path).is_file():
            skipped.append((path, "no such flow"))
            continue
        add(path, "named on the description's `validates:` line")
    for path in changed_flows:
        if path in seen:
            continue
        if not (root / path).is_file():
            continue
        if "known-bug" in flowfile.tags((root / path).read_text(encoding="utf-8")):
            skipped.append((path, "still tagged known-bug (name it on a `validates:` line to run it anyway)"))
            continue
        add(path, "added or changed in this PR")
    return picks, skipped


def markdown(picks, skipped):
    name = lambda p: Path(p).stem if p.startswith(FLOWS + "/") else p  # noqa: E731
    if not picks:
        md = ["### Before and after: no validating flow", "",
              "This PR names no flow that shows its change (a `validates: <flow>` line in the "
              "description, or a flow it adds or edits). Nothing below shows that the change works."]
    else:
        md = [f"### Before and after: {len(picks)} validating flow{'s' if len(picks) > 1 else ''}", "",
              "| Flow | Why |", "|---|---|"]
        md += [f"| `{name(p)}` | {why} |" for p, why in picks]
    if skipped:
        md += ["", "Not run: " + "; ".join(f"`{name(p)}` ({why})" for p, why in skipped) + "."]
    return "\n".join(md) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base", required=True)
    ap.add_argument("--head", default="HEAD")
    ap.add_argument("--body-file")
    ap.add_argument("--max", type=int, default=3)
    ap.add_argument("--root", default=str(Path(__file__).resolve().parents[2]))
    ap.add_argument("--json")
    ap.add_argument("--md")
    a = ap.parse_args()
    body = Path(a.body_file).read_text(encoding="utf-8", errors="replace") if a.body_file else ""
    picks, skipped = pick(a.root, body, changed(a.root, a.base, a.head), a.max)
    if a.json:
        Path(a.json).write_text(json.dumps({
            "flows": [{"file": p, "why": why} for p, why in picks],
            "skipped": [{"what": p, "why": why} for p, why in skipped],
        }, indent=2) + "\n")
    md = markdown(picks, skipped)
    if a.md:
        Path(a.md).write_text(md)
    print(md, file=sys.stderr)
    print(" ".join(p for p, _ in picks))


if __name__ == "__main__":
    main()
