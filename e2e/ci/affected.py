#!/usr/bin/env python3
"""Picks the end-to-end flows a change affects, for annotated videos on a PR.

What runs:
  - flows the change adds or edits (e2e/flows/*.yaml), whole;
  - for each other changed thing a flow can show, ONE flow, cut a few steps after it has shown it.

How a change is traced to a flow, through the test tag ids that flows and screens share:
  1. Flow index: each flow's top-level steps, with the ids each uses (following runFlow into
     sub-flows) and its length in steps (sub-flows counted in full).
  2. Code index: which app files use which tag (TestTags members and the older *_TAG constants),
     counting only tags some flow uses. A file that uses tags is a "screen file".
  3. A changed file is traced to screen files: a screen file is its own; any other Kotlin file
     follows the files that use its classes and functions, up to MAX_HOPS, to the nearest screen
     files; a resource follows the files that use it (R.string.x, R.drawable.y); a sub-flow goes
     to the flows that run it; TestTags.kt to the files using the tags the change touched.
  4. Each changed file then needs one flow that shows any of its screen files. A flow shows a
     screen file from the first step that uses one of its ids, through the steps after it that
     stay on it (they use its ids, or no ids at all), and is cut after the last of them. The
     cheapest such flow (fewest steps run) wins; a flow already picked is reused when it shows
     the screen too, so one video can cover several changes.
  5. At most --max flows run, changed flows first. The rest are listed as skipped.

Changes no flow can show (build files, tests, data code with no screen in reach) select nothing:
the full "Android end-to-end" check covers them.

Usage:
  affected.py --base origin/master [--head HEAD] [--max 6] [--json out.json] [--md out.md]
  affected.py --files a.kt b.yaml ...   (treat these files as modified; no git, for trying it out)
Prints the selectors for run-flows.sh (`e2e/flows/x.yaml` whole, `e2e/flows/x.yaml@7` cut after
7 steps), space separated, on stdout; empty when nothing is selected.
"""
import argparse
from fnmatch import fnmatch
import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

import flowfile
from flowfile import steps as flow_steps

ROOT = Path(__file__).resolve().parents[2]
FLOWS_DIR = "e2e/flows"
SRC = "app/src/main"
OVERRIDES = "e2e/ci/affected-overrides.json"
TEST_TAGS = "app/src/main/java/org/greenstand/android/TreeTracker/view/TestTags.kt"
MAX_HOPS = 3
# A flow shows a screen from the first step on it through at most this many more steps on it.
MAX_EXTRA_STEPS = 3
# Org-config and dev-tools flows are seeded before they run (run-flows.sh), which costs about as
# much as this many steps; it counts when comparing flows.
SETUP_COST = 5
HUB_MIN_COMPONENTS = 10
MAX_REASONS = 4
MAX_LISTED = 50
SEEDED_TAGS = {"org-config", "dev-tools"}
# A tag used by more files than this (shared buttons, dialogs) doesn't say which screen a flow is
# on, when the screen file has tags of its own.
SPECIFIC_MAX_FILES = 2

ID_RE = re.compile(r'\bid:\s*"?((?:[A-Za-z0-9_.-]|\$\{\w+\})+)')
FILE_RE = re.compile(r'(?:\bfile:|runFlow:)\s*"?([A-Za-z0-9_./-]+\.yaml)')
CONST_RE = re.compile(r'\bconst\s+val\s+(\w+)\s*=\s*"([^"$]+)"')
TAG_FUN_RE = re.compile(r'\bfun\s+(\w+)\([^)]*\)\s*=\s*"([^"$]*)\$')
PACKAGE_RE = re.compile(r"^package\s+([\w.]+)", re.M)
IMPORT_RE = re.compile(r"^import\s+([\w.]+)(\.\*)?", re.M)
MODIFIERS = (r"(?:(?:public|internal|protected|data|sealed|enum|abstract|open|inline|value|"
             r"annotation|suspend|override|const|lateinit|operator|infix|tailrec|external|"
             r"expect|actual|fun\s+interface)\s+)*")
CLASS_DECL_RE = re.compile(r"^\s*" + MODIFIERS + r"(?:class|interface|object)\s+(\w+)", re.M)
TOP_DECL_RE = re.compile(r"^" + MODIFIERS + r"(?:fun|val|var|typealias)\s+(?:<[^>]*>\s*)?"
                         r"(?:[\w.<>?, *]+\.)?(\w+)", re.M)
PRIVATE_RE = re.compile(r"^\s*private\b")
WORD_RE = re.compile(r"[A-Za-z_]\w*")
RES_VALUE_RE = re.compile(
    r'<(string|color|dimen|plurals|string-array|integer-array|array|integer|bool|style)\b[^>]*'
    r'\bname="([\w.]+)"')


def rel(p):
    return Path(p).as_posix()


# ---------------------------------------------------------------------------------------------
# Flows

@dataclass
class Step:
    index: int          # 1-based top-level step number
    text: str           # first line, for the summary
    ids: set            # ids this step uses, sub-flows included
    subflows: set       # sub-flow paths this step runs, nested ones included
    cost: int           # steps it runs, sub-flows counted in full


@dataclass
class Flow:
    path: str
    name: str
    tags: set
    steps: list

    def cost_to(self, n):
        return sum(s.cost for s in self.steps[:n])

    @property
    def total(self):
        return self.cost_to(len(self.steps))


def without_absent(block):
    """The lines of a step without comments and without assertNotVisible checks, whose ids are
    on no screen."""
    out, skip = [], None
    for line in block:
        indent = len(line) - len(line.lstrip())
        if skip is not None:
            if line.strip() and indent > skip:
                continue
            skip = None
        if line.lstrip().startswith("#"):
            continue
        if re.match(r"\s*-?\s*assertNotVisible\b", line):
            skip = indent
            continue
        out.append(line)
    return out


def flow_tags(text):
    return flowfile.tags(text)


def without_comments(block):
    return [l for l in block if l.strip() and not l.lstrip().startswith("#")]


class FlowIndex:
    def __init__(self, root):
        self.root = Path(root).resolve()
        self._sub = {}
        self.flows = {}
        for p in sorted((self.root / FLOWS_DIR).glob("*.yaml")):
            if p.name.startswith(".ci-") or p.name == "inline.yaml":
                continue
            text = p.read_text(encoding="utf-8")
            tags = flow_tags(text)
            path = rel(p.relative_to(self.root))
            self.flows[path] = Flow(path, p.stem, tags, self._steps(p, text, set()))

    def _subflow(self, base, name, seen):
        p = (base.parent / name).resolve()
        if p in seen or not p.exists():
            return set(), set(), 1
        if p not in self._sub:
            steps = self._steps(p, p.read_text(encoding="utf-8"), seen | {p})
            ids = set().union(*(s.ids for s in steps)) if steps else set()
            subs = {rel(p.relative_to(self.root))}.union(*(s.subflows for s in steps))
            self._sub[p] = (ids, subs, sum(s.cost for s in steps))
        return self._sub[p]

    def _steps(self, path, text, seen):
        out = []
        for i, block in enumerate(flow_steps(text.splitlines()), 1):
            body = "\n".join(without_absent(block))
            ids = set(ID_RE.findall(body))
            subs, cost = set(), 1
            for name in FILE_RE.findall(body):
                s_ids, s_subs, s_cost = self._subflow(path, name, seen)
                ids |= s_ids
                subs |= s_subs
                cost += s_cost
            text = " ".join(l.strip() for l in without_comments(block))[2:]
            out.append(Step(i, text if len(text) <= 70 else text[:69] + "…", ids, subs, cost))
        return out

    def all_ids(self):
        ids = set()
        for f in self.flows.values():
            for s in f.steps:
                ids |= s.ids
        return ids

    def candidates(self):
        return [f for f in self.flows.values() if "known-bug" not in f.tags]


# ---------------------------------------------------------------------------------------------
# Code

@dataclass
class KtFile:
    path: str
    package: str
    imports: set
    star_imports: set
    words: set
    text: str
    declared: set = field(default_factory=set)


class CodeIndex:
    def __init__(self, root, flow_ids, overrides=None):
        self.root = Path(root)
        self.files = {}
        for p in sorted((self.root / SRC).rglob("*.kt")):
            text = p.read_text(encoding="utf-8")
            m = PACKAGE_RE.search(text)
            imports, stars = set(), set()
            for name, star in IMPORT_RE.findall(text):
                (stars if star else imports).add(name)
            f = KtFile(rel(p.relative_to(self.root)), m.group(1) if m else "", imports, stars,
                       set(WORD_RE.findall(text)), text)
            f.declared = self._declared(text)
            self.files[f.path] = f
        self._tag_keys(flow_ids)
        # file -> tag keys it uses; key -> files using it
        self.file_tags, self.tag_files = {}, {}
        for f in self.files.values():
            if f.path == TEST_TAGS:
                continue
            keys = {key for (pkg, owner, name), key in self.tag_symbols.items()
                    if self.uses(f, pkg, name, owner)}
            if keys:
                self.file_tags[f.path] = keys
                for k in keys:
                    self.tag_files.setdefault(k, set()).add(f.path)
        self._users = {}
        # Tags a file shows through the tagged components it uses (CaptureButton() brings
        # CaptureButton), and how many files show each tag, own or through a component.
        # A file that uses many tagged files (Host.kt) is a navigation hub, not a screen: it
        # isn't shown by itself, and the search for screens doesn't go through it.
        self.comp_tags, comp_count, self.hubs = {}, {}, set()
        for f in self.files.values():
            if f.path == TEST_TAGS:
                continue
            comp, used = set(), 0
            for other in self.file_tags:
                o = self.files[other]
                if other != f.path and any(self.uses(f, o.package, n) for n in o.declared):
                    comp |= self.file_tags[other]
                    used += 1
            if used >= HUB_MIN_COMPONENTS:
                self.hubs.add(f.path)
                continue
            comp -= self.file_tags.get(f.path, set())
            if comp:
                self.comp_tags[f.path] = comp
                for k in comp:
                    comp_count[k] = comp_count.get(k, 0) + 1
        self.comp_count = comp_count
        self.shown_by = {k: len(v) + comp_count.get(k, 0) for k, v in self.tag_files.items()}
        self.overrides = overrides or {}
        self.screens, self.specific = {}, set()
        for path in self.files:
            if path in self.hubs or path == TEST_TAGS:
                continue
            screen = self._screen(path)
            if screen:
                self.screens[path] = screen[:2]
                if screen[2]:
                    self.specific.add(path)

    @staticmethod
    def _declared(text):
        names = set()
        for m in CLASS_DECL_RE.finditer(text):
            line = text[text.rfind("\n", 0, m.start()) + 1:m.end()]
            if not PRIVATE_RE.match(line):
                names.add(m.group(1))
        for m in TOP_DECL_RE.finditer(text):
            names.add(m.group(1))
        return names

    def _tag_keys(self, flow_ids):
        """Tag symbols some flow uses: (package, owner object or None, name) -> key. A key is
        the id itself, or "Prefix_*" for a TestTags function that builds ids."""
        exact = {i for i in flow_ids if "${" not in i}
        prefixes = {i.split("${")[0] for i in flow_ids if "${" in i}
        self.tag_symbols = {}
        for f in self.files.values():
            owner = "TestTags" if f.path == TEST_TAGS else None
            for name, value in CONST_RE.findall(f.text):
                if value in exact:
                    self.tag_symbols[(f.package, owner, name)] = value
            if f.path == TEST_TAGS:
                for name, prefix in TAG_FUN_RE.findall(f.text):
                    if prefix in prefixes or any(i.startswith(prefix) for i in exact):
                        self.tag_symbols[(f.package, owner, name)] = prefix + "*"

    def uses(self, f, pkg, name, owner=None):
        """Whether file f can see and names the symbol `name` declared in package pkg (as a
        member of object `owner`, when given)."""
        if owner:
            if re.search(rf"\b{owner}\.{name}\b", f.text):
                return (f.package == pkg or f"{pkg}.{owner}" in f.imports
                        or pkg in f.star_imports)
            return f"{pkg}.{owner}.{name}" in f.imports and name in f.words
        if name not in f.words:
            return False
        return f.package == pkg or f"{pkg}.{name}" in f.imports or pkg in f.star_imports

    def users(self, path):
        """Files that use something the file at path declares."""
        if path not in self._users:
            src = self.files[path]
            out = set()
            for f in self.files.values():
                if f.path != path and any(self.uses(f, src.package, n) for n in src.declared):
                    out.add(f.path)
            self._users[path] = out
        return self._users[path]

    def screens_from(self, start, direct=False):
        """Nearest screen files from the given files (themselves if they are), with the hops
        taken, following users() up to MAX_HOPS. Returns ({screen file: route}, hops). direct:
        the files are the change itself, so any ids of theirs count."""
        found = {p: [p] for p in start if p in self.screens}
        if found:
            # A file reached through others only counts with ids of its own (not Root.kt's
            # Snackbar, which every screen shows).
            found = {p: r for p, r in found.items() if direct or p in self.specific}
        if found:
            return found, 0
        frontier = {p: [p] for p in start if p in self.files}
        seen = set(frontier)
        for hop in range(1, MAX_HOPS + 1):
            nxt = {}
            for p, route in frontier.items():
                for u in sorted(self.users(p)):
                    if u not in seen and u not in self.hubs:
                        seen.add(u)
                        nxt[u] = route + [u]
            found = {p: r for p, r in nxt.items() if p in self.specific}
            if found:
                return found, hop
            frontier = nxt
        return {}, None

    def files_using_resource(self, kind, name):
        pat = re.compile(r"\bR\." + re.escape(kind) + r"\." + re.escape(name) + r"\b")
        return {f.path for f in self.files.values() if pat.search(f.text)}

    def _screen(self, path):
        """How a flow shows this file: (ids, after), or None when no flow can tell it is on it.
        From the overrides; else its own tags that at most SPECIFIC_MAX_FILES files show, and
        tags it shows through a component no other file uses; else all its own tags."""
        for suffix, o in self.overrides.items():
            if path.endswith("/" + suffix) or path == suffix:
                return set(o.get("ids", [])), list(o.get("after", [])), True
        own, comp = self.file_tags.get(path, set()), self.comp_tags.get(path, set())
        specific = ({k for k in own if self.shown_by[k] <= SPECIFIC_MAX_FILES}
                    | {k for k in comp if self.comp_count[k] == 1})
        if specific:
            return specific, [], True
        return (own, [], False) if own else None


def id_matches(flow_id, keys):
    if flow_id in keys:
        return True
    head = flow_id.split("${")[0]
    return any(k.endswith("*") and head.startswith(k[:-1]) for k in keys)


# ---------------------------------------------------------------------------------------------
# Changes and targets

@dataclass
class Change:
    path: str
    status: str = "M"            # git status letter: A, M, D, R...
    lines: list = field(default_factory=list)   # added and removed lines, without the +/-


@dataclass
class Target:
    """Something a changed file needs a flow to show."""
    change: str
    screens: dict = field(default_factory=dict)   # screen file -> (ids, after)
    routes: dict = field(default_factory=dict)    # screen file -> files from the change to it
    via: str = None              # what the change touched, when not the whole file
    subflow: str = None
    order: int = 0               # lower is picked first under the cap

    def why(self, screen=None):
        """How the change reaches the screen shown (any, when not given), for the summary."""
        if self.subflow:
            return f"`{short(self.subflow)}` changed; this flow runs it"
        if screen is None:
            screen = min(self.routes, key=lambda s: (len(self.routes[s]), s))
        route = [x for x in self.routes[screen] if x != self.change]
        head = f"`{short(self.change)}`" + (f" ({self.via})" if self.via else "")
        if not route:
            return f"{head}: on screen by ids {fmt_ids(self.screens[screen][0])}"
        out = " → ".join([head] + [f"`{short(x)}`" for x in route])
        others = len(self.routes) - 1
        if others:
            out += f" (one of {others + 1} screen files it reaches)"
        return out


@dataclass
class Pick:
    flow: Flow
    cut: int = None              # top-level steps to run; None = whole flow
    reasons: list = field(default_factory=list)

    def steps_run(self):
        return len(self.flow.steps) if self.cut is None else self.cut


def short(path):
    return printable(path.rsplit("/", 1)[-1])


def cost(flow, n):
    return flow.cost_to(n) + (SETUP_COST if flow.tags & SEEDED_TAGS else 0)


def show(flow, target, code):
    """Where flow shows target: (cut, cost, step, screen), the cheapest over the target's
    screens, or None when it never does."""
    best = None
    if target.subflow:
        for s in flow.steps:
            if target.subflow in s.subflows:
                best = (s.index, cost(flow, s.index), s, None)
                break
        return best
    for screen, (keys, after) in target.screens.items():
        first = last = None
        armed = not after
        for s in flow.steps:
            if not armed:
                if any(a in s.subflows for a in after if a.endswith(".yaml")):
                    armed = True
                    continue
                armed = any(id_matches(i, set(after)) for i in s.ids)
            on = armed and any(id_matches(i, keys) for i in s.ids)
            if first is None:
                if on:
                    first = last = s.index
            elif s.index > first + MAX_EXTRA_STEPS:
                break
            elif on:
                last = s.index
            elif s.ids:
                break
        if first is not None:
            cand = (last, cost(flow, last), flow.steps[last - 1], screen)
            if best is None or cand[1] < best[1]:
                best = cand
    return best


def targets_for(changes, flows, code):
    targets, unmapped, whole = [], [], []
    for c in changes:
        p = c.path
        if c.status.startswith("D"):
            unmapped.append((p, "deleted"))
            continue
        if c.status == "R100":
            unmapped.append((p, "renamed, not changed"))
            continue
        is_flow = p.startswith(FLOWS_DIR + "/") and p.endswith(".yaml")
        if is_flow and "/" in p[len(FLOWS_DIR) + 1:]:
            # In a folder under e2e/flows: a sub-flow other flows run.
            targets.append(Target(p, subflow=p, order=0))
        elif is_flow:
            if p in flows.flows:
                whole.append(p)
            else:
                unmapped.append((p, "not a flow run-flows.sh runs"))
        elif p == TEST_TAGS:
            names = {m for line in c.lines for m in re.findall(r"\b(?:val|fun)\s+(\w+)", line)}
            keys = {k for (pkg, owner, n), k in code.tag_symbols.items() if n in names}
            if not keys:
                unmapped.append((p, "no tag a flow uses changed"))
                continue
            files = {f for k in keys for f in code.tag_files.get(k, ())}
            add_code_target(targets, unmapped, p, files, code, f"tags {', '.join(sorted(keys))}")
        elif p.startswith(SRC + "/") and p.endswith(".kt"):
            if p in code.files:
                add_code_target(targets, unmapped, p, {p}, code, None)
            else:
                unmapped.append((p, "not found"))
        elif p.startswith(SRC + "/res/"):
            res = resources(c)
            if not res:
                unmapped.append((p, "no resource names found in the diff"))
                continue
            files = set()
            for kind, name in res:
                files |= code.files_using_resource(kind, name)
            label = ", ".join(f"R.{k}.{n}" for k, n in sorted(res)[:4]) + (" ..." if len(res) > 4 else "")
            add_code_target(targets, unmapped, p, files, code, label or None)
        else:
            unmapped.append((p, "not app code or a flow"))
    return targets, unmapped, whole


def resources(c):
    parts = c.path.split("/")
    if len(parts) < 6:
        return set()
    kind = parts[4].split("-")[0]
    if kind == "values":
        out = set()
        for line in c.lines:
            for tag, name in RES_VALUE_RE.findall(line):
                k = {"string-array": "array", "integer-array": "array"}.get(tag, tag)
                out.add((k, name.replace(".", "_")))
        return out
    return {(kind, parts[-1].split(".")[0])}


def add_code_target(targets, unmapped, change, files, code, via):
    if not files:
        unmapped.append((change, f"nothing in the app uses {via}" if via else "no screen in reach"))
        return
    found, hops = code.screens_from(files, direct=files == {change})
    if not found:
        unmapped.append((change, f"no screen within {MAX_HOPS} hops" + (f" (via {via})" if via else "")))
        return
    order = (1 if via else 0) + (hops or 0)
    targets.append(Target(change, {s: code.screens[s] for s in found}, found, via, order=order))


def fmt_ids(keys, n=3):
    ks = sorted(keys)
    return ", ".join(f"`{k}`" for k in ks[:n]) + (" ..." if len(ks) > n else "")


# ---------------------------------------------------------------------------------------------
# Selection

def load_overrides(root):
    p = Path(root) / OVERRIDES
    return json.loads(p.read_text()) if p.exists() else {}


def select(changes, root=ROOT, max_flows=6):
    over = load_overrides(root)
    flows = FlowIndex(root)
    code = CodeIndex(root, flows.all_ids(), over.get("screens"))
    ignored = [c for c in changes if any(fnmatch(c.path, g) for g in over.get("ignore", []))]
    changes = [c for c in changes if c not in ignored]
    targets, unmapped, whole = targets_for(changes, flows, code)
    unmapped += [(c.path, "ignored in " + OVERRIDES) for c in ignored]
    picks, skipped, no_flow = {}, [], []
    for p in whole:
        if len(picks) < max_flows:
            picks[p] = Pick(flows.flows[p], None, ["changed in this PR"])
        else:
            skipped.append((p, "changed in this PR", "over the limit of %d flows" % max_flows))
    candidates = flows.candidates()
    options = {}
    for t in targets:
        options[id(t)] = [(f, s) for f in candidates if (s := show(f, t, code))]
    targets.sort(key=lambda t: (t.order, len(options[id(t)]), t.change))
    for t in targets:
        opts = options[id(t)]
        if not opts:
            no_flow.append((t.change, t.why()))
            continue
        # Reuse a picked flow when it shows this at no or little extra cost; else the cheapest.
        # At the limit, only flows already picked can take it (run further if need be).
        if len(picks) >= max_flows and any(f.path in picks for f, _ in opts):
            opts = [(f, o) for f, o in opts if f.path in picks]
        best = None
        for f, (cut, c, step, screen) in opts:
            pick = picks.get(f.path)
            if pick:
                extra = 0 if pick.cut is None or cut <= pick.cut else c - cost(f, pick.steps_run())
            else:
                extra = c
            key = (extra, 0 if pick else 1, c, f.name)
            if best is None or key < best[0]:
                best = (key, f, cut, step, screen)
        _, f, cut, step, screen = best
        pick = picks.get(f.path)
        if pick is None:
            if len(picks) >= max_flows:
                skipped.append((f.path, t.why(screen), "over the limit of %d flows" % max_flows))
                continue
            pick = picks[f.path] = Pick(f, cut)
        elif pick.cut is not None and cut > pick.cut:
            pick.cut = cut
        pick.reasons.append(f"{t.why(screen)}; step {step.index}: `{step.text}`")
    for pick in picks.values():
        if pick.cut is not None and pick.cut >= len(pick.flow.steps):
            pick.cut = None
    return {
        "picks": list(picks.values()),
        "skipped": skipped,
        "no_flow": no_flow,
        "unmapped": unmapped,
    }


def selectors(result):
    return [p.flow.path + ("" if p.cut is None else f"@{p.cut}") for p in result["picks"]]


def markdown(result, max_flows):
    picks = result["picks"]
    md = [f"### Flows this PR affects: {len(picks)} to run", ""]
    if picks:
        md += ["| Flow | Runs | Why |", "|---|---|---|"]
        for p in picks:
            n = len(p.flow.steps)
            if p.cut is None:
                runs = f"whole ({n} steps)"
            else:
                runs = f"steps 1–{p.cut} of {n}, to `{p.flow.steps[p.cut - 1].text}`"
            reasons = p.reasons[:MAX_REASONS]
            if len(p.reasons) > MAX_REASONS:
                reasons.append(f"and {len(p.reasons) - MAX_REASONS} more changed files")
            cells = [f"`{p.flow.name}`", runs, "<br>".join(reasons)]
            md.append("| " + " | ".join(c.replace("|", "\\|") for c in cells) + " |")
    else:
        md.append("No changed flows, and no changed screen any flow shows.")
    if result["skipped"]:
        md += ["", f"**Not run, over the limit of {max_flows} flows:**", ""]
        md += [f"- `{short(f)}`: {why}" for f, why, _ in result["skipped"]]  # short() escapes
    if result["no_flow"]:
        md += ["", "**Changed screens no flow reaches** (worth a new flow?):", ""]
        md += capped([f"- {why}" for _, why in result["no_flow"]])
    if result["unmapped"]:
        md += ["", "<details><summary>Changed files with no screen to show "
               f"({len(result['unmapped'])}): the full end-to-end check covers them</summary>", ""]
        md += capped([f"- `{printable(p)}`: {why}" for p, why in result["unmapped"]])
        md += ["", "</details>"]
    return "\n".join(md) + "\n"


def printable(text):
    """Control characters (a newline in a file name) shown escaped, so one path stays one line."""
    return re.sub(r"[\x00-\x1f\x7f]", lambda m: f"\\x{ord(m.group()):02x}", text)


def capped(lines, n=None):
    """At most n lines (MAX_LISTED), so the PR comment stays under GitHub's size limit."""
    n = n or MAX_LISTED
    return lines[:n] + ([f"- and {len(lines) - n} more"] if len(lines) > n else [])


def as_json(result):
    return {
        "selectors": selectors(result),
        "flows": [{"flow": p.flow.path, "cut": p.cut, "steps": len(p.flow.steps),
                   "reasons": p.reasons} for p in result["picks"]],
        "skipped": [{"flow": f, "why": w, "reason": r} for f, w, r in result["skipped"]],
        "noFlow": [{"change": c, "why": w} for c, w in result["no_flow"]],
        "unmapped": [{"change": c, "why": w} for c, w in result["unmapped"]],
    }


# ---------------------------------------------------------------------------------------------

def git(root, *args):
    return subprocess.run(["git", *args], cwd=root, check=True, capture_output=True,
                          text=True).stdout


def changes_from_git(root, base, head):
    out = []
    # -z: paths as they are, not quoted when they have unusual characters.
    fields = git(root, "diff", "-z", "--name-status", "-M", f"{base}...{head}").split("\0")
    i = 0
    while i < len(fields) and fields[i]:
        status = fields[i]
        n = 2 if status[:1] in "RC" else 1
        path = fields[i + n]
        i += n + 1
        c = Change(path, status)
        if path == TEST_TAGS or path.startswith(SRC + "/res/values"):
            diff = git(root, "diff", "-U0", f"{base}...{head}", "--", path)
            c.lines = [l[1:] for l in diff.splitlines()
                       if l[:1] in "+-" and not l.startswith(("+++", "---"))]
        out.append(c)
    return out


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--base")
    ap.add_argument("--head", default="HEAD")
    ap.add_argument("--files", nargs="+")
    ap.add_argument("--max", type=int, default=6)
    ap.add_argument("--json")
    ap.add_argument("--md")
    ap.add_argument("--root", default=str(ROOT),
                    help="the checkout to look at (default: the one this script is in)")
    a = ap.parse_args()
    if bool(a.base) == bool(a.files):
        ap.error("give --base or --files")
    root = Path(a.root).resolve()
    changes = [Change(p) for p in a.files] if a.files else changes_from_git(root, a.base, a.head)
    result = select(changes, root, a.max)
    md = markdown(result, a.max)
    if a.json:
        Path(a.json).write_text(json.dumps(as_json(result), indent=2) + "\n")
    if a.md:
        Path(a.md).write_text(md)
    print(md, file=sys.stderr)
    print(" ".join(selectors(result)))


if __name__ == "__main__":
    main()
