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


def _uncomment(line):
    """The line without a trailing # comment (a # at the start or after a space)."""
    return re.split(r"(?:^|\s)#", line, maxsplit=1)[0].rstrip()


def tags(text):
    """The tags in a flow's header: `tags: [a, b]`, `tags: a`, or a block list under `tags:`
    (indented or not, with comments)."""
    header = text.split("\n---", 1)[0].splitlines()
    for i, line in enumerate(header):
        m = re.match(r"^tags:(.*)$", _uncomment(line))
        if not m:
            continue
        rest = m.group(1).strip()
        if rest:
            items = rest.strip("[]").split(",")
        else:
            items = []
            for nxt in header[i + 1:]:
                body = _uncomment(nxt)
                if not body.strip():
                    continue
                item = re.match(r"^\s*-\s*(.*)$", body)
                if not item:
                    break
                items.append(item.group(1))
        return {t.strip().strip("'\"") for t in items if t.strip().strip("'\"")}
    return set()


if __name__ == "__main__":
    # flowfile.py has-tag FLOW TAG: exit 0 when the flow has the tag (for run-flows.sh).
    import sys
    if len(sys.argv) == 4 and sys.argv[1] == "has-tag":
        with open(sys.argv[2], encoding="utf-8") as f:
            sys.exit(0 if sys.argv[3] in tags(f.read()) else 1)
    print("Usage: flowfile.py has-tag FLOW TAG", file=sys.stderr)
    sys.exit(2)
