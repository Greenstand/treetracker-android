#!/usr/bin/env python3
"""Removes the admin login from tb's output.

tb prints and saves the text a flow types: `Input text <value>` on the console, and in a failed
flow's log.txt, screen.txt and hierarchy.json. This replaces every value named in SCRUB_VARS
(environment variable names, comma-separated) with ***, in the forms tb writes it: as is,
JSON-escaped (Python's and Go's, which writes < > & as \u003c \u003e \u0026) and HTML-escaped.

After scrubbing a directory it checks each file once more, for the values and for every run of 6
or more letters and digits in them. A file that still has one (an encoding not listed here) is
deleted rather than kept.

  python3 scrub.py --stdin        filter stdin to stdout, line by line
  python3 scrub.py DIR            scrub every file under DIR in place, then check: a file that
                                  still holds a value is deleted and the exit code is 1

The values come from the environment only, so they never appear on a command line.
"""
import html
import json
import os
import re
import sys

MASK = b"***"


def values():
    names = [n.strip() for n in os.environ.get("SCRUB_VARS", "").split(",") if n.strip()]
    # Shorter than 3 characters is too short to replace safely; run.sh refuses such values.
    return [v for v in (os.environ.get(n, "") for n in names) if len(v) >= 3]


def go_json(v):
    s = json.dumps(v, ensure_ascii=False)[1:-1]
    for c, e in (("<", "\\u003c"), (">", "\\u003e"), ("&", "\\u0026"),
                 ("\u2028", "\\u2028"), ("\u2029", "\\u2029")):
        s = s.replace(c, e)
    return s


def secrets():
    forms = set()
    for v in values():
        for f in (v, json.dumps(v)[1:-1], json.dumps(v, ensure_ascii=False)[1:-1], go_json(v),
                  html.escape(v), html.escape(v, quote=False)):
            forms.add(f.encode("utf-8"))
    # Longest first, so a value that contains another is replaced whole.
    return sorted(forms, key=len, reverse=True)


def canaries():
    """Runs of 6+ letters and digits from each value: they survive any escaping."""
    out = set()
    for v in values():
        out.update(m.encode("utf-8") for m in re.findall(r"[A-Za-z0-9]{6,}", v))
    return out


def scrub(data, forms):
    for f in forms:
        data = data.replace(f, MASK)
    return data


def scrub_stdin(forms):
    out = sys.stdout.buffer
    for line in sys.stdin.buffer:
        out.write(scrub(line, forms))
        out.flush()
    return 0


def scrub_dir(root, forms):
    check = set(forms) | canaries()
    leaked = 0
    for d, _, files in os.walk(root):
        for name in files:
            path = os.path.join(d, name)
            if os.path.islink(path):
                continue
            with open(path, "rb") as fh:
                data = fh.read()
            clean = scrub(data, forms)
            if clean != data:
                with open(path, "wb") as fh:
                    fh.write(clean)
            if any(f in clean for f in check):  # an encoding not handled above: fail closed
                os.remove(path)
                print(f"scrub: removed {path}: part of a secret was still in it", file=sys.stderr)
                leaked += 1
    return 1 if leaked else 0


def main():
    forms = secrets()
    if len(sys.argv) == 2 and sys.argv[1] == "--stdin":
        return scrub_stdin(forms)
    if len(sys.argv) == 2 and os.path.isdir(sys.argv[1]):
        return scrub_dir(sys.argv[1], forms)
    print("usage: SCRUB_VARS=A,B scrub.py --stdin | DIR", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
