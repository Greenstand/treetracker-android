#!/usr/bin/env python3
"""Prints a copy of a Test Buddy flow with a takeScreenshot after its top-level steps.

The copy is written next to the original by run-flows.sh, so runFlow paths still resolve. Each
screenshot is named after the step it follows (step-03-tapOn-SettingsButton), and the last one is
always taken (step-NN-end-...). Steps that only check the screen (assertVisible and the like) get
none, since the screen is the one before them. Comments, anchors and nested commands are kept as
they are: only lines that start a top-level step ("- " at column 0) are looked at.

Usage: step_shots.py [--mode steps|end] FLOW.yaml > COPY.yaml
"""
import re
import sys

from flowfile import is_step, split

# Steps that leave the screen as it was.
NO_SHOT = {
    "assertVisible", "assertNotVisible", "assertTrue", "assertWithAI", "assertNoDefectsWithAI",
    "assertScreenshot", "takeScreenshot", "evalScript", "copyTextFrom", "startRecording",
    "stopRecording",
}
KEY = re.compile(r"^- ([A-Za-z]+)\s*(?::\s*(.*))?$")


def label(block):
    m = KEY.match(block[0].rstrip())
    if not m:
        return "step", True
    cmd = m.group(1)
    # The step's id, text or file, from its own line or the lines under it.
    text = " ".join(l.strip() for l in block)
    word = re.search(r'(?:\bid|\btext|\bfile|\bvisible):\s*"?([A-Za-z0-9_./-]+)', text) or \
        re.match(r'- [A-Za-z]+:\s*"?([A-Za-z0-9_./-]+)', block[0])
    name = cmd
    if word:
        name += "-" + word.group(1).rsplit("/", 1)[-1].removesuffix(".yaml")
    return re.sub(r"[^A-Za-z0-9_-]+", "_", name)[:48], cmd not in NO_SHOT


def main():
    args = sys.argv[1:]
    mode = "steps"
    if len(args) >= 2 and args[0] == "--mode":
        mode, args = args[1], args[2:]
    if len(args) != 1 or mode not in ("steps", "end"):
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    lines = open(args[0], encoding="utf-8").read().splitlines()
    # The commands are the document after the header's "---"; a file without one is all commands.
    out, blocks = split(lines)
    out = list(out)
    steps = [b for b in blocks if is_step(b)]
    n = 0
    for b in blocks:
        out.extend(b)
        if not is_step(b):
            continue
        n += 1
        name, wanted = label(b)
        if n == len(steps):
            # Always end on the screen the flow left.
            out.append(f'- takeScreenshot: "step-{n:02d}-end-{name}"')
        elif wanted and mode == "steps":
            out.append(f'- takeScreenshot: "step-{n:02d}-{name}"')
    print("\n".join(out))


if __name__ == "__main__":
    main()
