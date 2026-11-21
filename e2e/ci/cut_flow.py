#!/usr/bin/env python3
"""Prints a Test Buddy flow cut after its first N top-level steps.

Used by run-flows.sh for a selector like `capture-first-tree.yaml@7`, which affected.py writes to
show one screen without running the rest of the flow. The copy goes next to the original, so
runFlow paths still resolve. Comments after the last kept step are dropped.

Usage: cut_flow.py FLOW.yaml N > COPY.yaml
"""
import sys

from flowfile import is_step, split


def cut(lines, n):
    header, blocks = split(lines)
    out, kept = header, 0
    for b in blocks:
        if kept == n:
            break
        kept += is_step(b)
        out.extend(b)
    return out


def main():
    if len(sys.argv) != 3 or not sys.argv[2].isdigit() or int(sys.argv[2]) < 1:
        print(__doc__, file=sys.stderr)
        sys.exit(2)
    lines = open(sys.argv[1], encoding="utf-8").read().splitlines()
    print("\n".join(cut(lines, int(sys.argv[2]))))


if __name__ == "__main__":
    main()
