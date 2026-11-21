"""Splits a Test Buddy flow file into its top-level steps, without a YAML parser.

Shared by step_shots.py, cut_flow.py and affected.py, so they agree on what a step is: a line
that starts with "- " at column 0, plus the indented or blank lines under it. Comments at column 0
are blocks of their own, kept where they are.
"""
import re

STEP = re.compile(r"^- ")


def split(lines):
    """Returns (header, blocks): the lines up to and including the header's "---" (none for a
    file without one), and the rest as blocks, each a list of lines."""
    start = next((i + 1 for i, l in enumerate(lines) if l.rstrip() == "---"), 0)
    blocks, cur = [], None
    for line in lines[start:]:
        if STEP.match(line):
            cur = [line]
            blocks.append(cur)
        elif cur is not None and (line.startswith((" ", "\t")) or not line.strip()):
            cur.append(line)
        else:
            cur = None
            blocks.append([line])
    return lines[:start], blocks


def is_step(block):
    return bool(STEP.match(block[0]))


def steps(lines):
    """The top-level steps of a flow, each a list of lines."""
    return [b for b in split(lines)[1] if is_step(b)]
