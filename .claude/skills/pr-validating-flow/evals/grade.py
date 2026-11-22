#!/usr/bin/env python3
"""Grades one outcome run of the pr-validating-flow eval. Pure code: every check here reads the
agent's git diff, its PR_DESCRIPTION.md, its transcript, or CI's verdict.json. The two checks a
model judges are in run_outcome.py (judge) and are marked "judge": true in the results.

    grade.py --repo WORKDIR --base SHA --kind flow|fallback [--verdict verdict.json]
             [--transcript FILE ...] [--json OUT]

A check is {"id", "pass": true|false|null, "scored": bool, "detail"}; null means it could not be
decided (no CI verdict yet). `scored` is false for checks that only say whether the skill fired.
"""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

FLOWS = "e2e/flows"
VALIDATES = re.compile(r"^[\s>*-]*validates:(.*)$", re.I)
NAME = re.compile(r"^(?:e2e/flows/)?([A-Za-z0-9_-]+)(?:\.yaml)?$")
ASSERTS = re.compile(r"^\s*-?\s*(assert\w*|extendedWaitUntil|visible|notVisible)\b")
# Things that have no business in app code for a test's sake. A heuristic: it lists what it found.
APP_SMELLS = re.compile(r"Thread\.sleep\(|\bdelay\(|SystemClock\.sleep\(|isRunningTest|isE2e|forTest\b|TestHook", re.I)
TEST_DIRS = ("app/src/test/", "app/src/androidTest/")


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, text=True).stdout


def diff_names(repo, base, flt):
    out = git(repo, "diff", "--name-only", f"--diff-filter={flt}", "-z", base, "--")
    return [p for p in out.split("\0") if p]


def named_flows(description):
    """Flow names on the description's first `validates:` line (as e2e/ci/validating.py reads it)."""
    for line in description.splitlines():
        m = VALIDATES.match(line.rstrip("\r"))
        if m:
            words = [w for w in re.split(r"[\s,]+", m.group(1).strip()) if w]
            return [NAME.match(w).group(1) for w in words if NAME.match(w)]
    return []


def tags(text):
    m = re.search(r"^tags:\s*\[(.*?)\]", text, re.M)
    return [t.strip() for t in m.group(1).split(",")] if m else []


def removed_assertions(repo, base, path):
    """Assertion lines an existing flow lost (a changed assertion shows as removed + added)."""
    out = git(repo, "diff", "-U0", base, "--", path)
    return [l[1:].strip() for l in out.splitlines()
            if l.startswith("-") and not l.startswith("---") and ASSERTS.match(l[1:])]


def app_smells(repo, base):
    out = git(repo, "diff", "-U0", base, "--", "app/src/main")
    return [l[1:].strip()[:120] for l in out.splitlines()
            if l.startswith("+") and not l.startswith("+++") and APP_SMELLS.search(l)]


def transcript_facts(paths):
    """What the agent's stream-json transcripts show: skills loaded, files read, commands run."""
    skills, text = set(), []
    for p in paths:
        for line in Path(p).read_text(errors="replace").splitlines():
            try:
                ev = json.loads(line)
            except ValueError:
                continue
            for block in (ev.get("message") or {}).get("content") or []:
                if isinstance(block, dict) and block.get("type") == "tool_use":
                    inp = block.get("input") or {}
                    if block.get("name") == "Skill":
                        skills.add(str(inp.get("skill")))
                    text.append(json.dumps(inp))
    blob = "\n".join(text)
    return {
        "skills": sorted(skills),
        "loaded_this_skill": "pr-validating-flow" in skills or "pr-validating-flow/SKILL.md" in blob,
        "used_tb_skill": bool({"tb", "test-buddy:tb"} & skills) or "plugin/skills/tb" in blob,
    }


def grade(repo, base, kind, verdict=None, transcripts=()):
    repo = Path(repo)
    desc_file = repo / "PR_DESCRIPTION.md"
    desc = desc_file.read_text(errors="replace") if desc_file.is_file() else ""
    added = [p for p in diff_names(repo, base, "A") if re.fullmatch(FLOWS + r"/[A-Za-z0-9_-]+\.yaml", p)]
    added_names = [Path(p).stem for p in added]
    modified_flows = [p for p in diff_names(repo, base, "MD") if p.startswith(FLOWS + "/")]
    named = named_flows(desc)
    claimed = [n for n in named if (repo / FLOWS / f"{n}.yaml").is_file()]
    tests = [p for p in diff_names(repo, base, "AM") if p.startswith(TEST_DIRS)]
    main = [p for p in diff_names(repo, base, "AMD") if p.startswith("app/src/main/")]
    facts = transcript_facts(transcripts)
    checks = []

    def add(cid, ok, detail="", scored=True):
        checks.append({"id": cid, "pass": ok, "scored": scored, "detail": detail})

    add("description_written", bool(desc.strip()), "PR_DESCRIPTION.md" if desc.strip() else "no PR_DESCRIPTION.md")
    add("app_code_changed", bool(main), ", ".join(main[:5]))
    lost = {p: removed_assertions(repo, base, p) for p in modified_flows}
    lost = {p: v for p, v in lost.items() if v}
    add("existing_flow_assertions_untouched", not lost, json.dumps(lost)[:300] if lost else "")
    smells = app_smells(repo, base)
    add("no_test_only_app_code", not smells, "; ".join(smells[:3]))

    if kind == "flow":
        add("new_flow_added", bool(added), ", ".join(added) or "no new file in e2e/flows/")
        ok = bool(set(claimed) & set(added_names))
        add("validates_line_names_new_flow", ok, f"validates: {' '.join(named) or '(no line)'}")
        kb = [p for p in added if "known-bug" in tags((repo / p).read_text())]
        add("new_flow_not_known_bug", (not kb) if added else None, ", ".join(kb))
        org = [p for p in added if "org-config" in tags((repo / p).read_text())]
        listed = (repo / "e2e/run-org.sh").read_text() if (repo / "e2e/run-org.sh").is_file() else ""
        missing = [p for p in org if Path(p).name not in listed]
        add("org_flow_listed_in_run_org", (not missing) if org else None, ", ".join(missing))
        add("used_tb_skill", facts["used_tb_skill"] if transcripts else None,
            "the transcript shows Test Buddy's skill loaded or its files read" if facts["used_tb_skill"] else
            "no Skill tb call and no read of plugin/skills/tb in the transcript")
        # The oracle: CI ran each flow on the base app and on the agent's app (e2e/ci/verdict.py).
        if verdict is None:
            add("ci_verdict_validated", None if added else False,
                "CI did not run" if added else "no flow to run")
        else:
            by = {f["name"]: f for f in verdict.get("flows", [])}
            target = [n for n in (claimed or added_names) if n in by]
            v = {n: by[n]["verdict"] for n in target}
            add("ci_verdict_validated", bool(v) and all(x == "validated" for x in v.values()), json.dumps(v))
    else:
        # A flow that CI would run as proof: named on validates:, or simply added by the PR.
        add("no_flow_claimed_as_proof", not claimed and not added,
            f"validates: {' '.join(named)}" if claimed else (", ".join(added) if added else ""))
        add("test_added", bool(tests), ", ".join(tests[:4]) or "no test file added or changed")
        names = [Path(p).stem for p in tests]
        add("description_names_the_test", any(n in desc for n in names) if tests else False,
            ", ".join(n for n in names if n in desc))
        add("says_no_validating_flow", bool(re.search(r"no validating flow", desc, re.I)),
            "the skill's own wording; the judge checks the meaning", scored=False)
        if verdict is not None:
            add("ci_verdict_of_claimed_flow", None, json.dumps({f["name"]: f["verdict"] for f in verdict.get("flows", [])}), scored=False)
    add("loaded_this_skill", facts["loaded_this_skill"] if transcripts else None, ", ".join(facts["skills"]), scored=False)
    return {"kind": kind, "added_flows": added_names, "claimed_flows": claimed, "tests": tests, "checks": checks}


def score(result):
    scored = [c for c in result["checks"] if c["scored"] and c["pass"] is not None]
    return sum(c["pass"] for c in scored), len(scored)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--base", required=True)
    ap.add_argument("--kind", required=True, choices=["flow", "fallback"])
    ap.add_argument("--verdict")
    ap.add_argument("--transcript", action="append", default=[])
    ap.add_argument("--json")
    a = ap.parse_args()
    verdict = json.loads(Path(a.verdict).read_text()) if a.verdict else None
    result = grade(a.repo, a.base, a.kind, verdict, a.transcript)
    if a.json:
        Path(a.json).write_text(json.dumps(result, indent=2) + "\n")
    for c in result["checks"]:
        mark = {True: "PASS", False: "FAIL", None: " -- "}[c["pass"]]
        print(f"{mark} {c['id']}{'' if c['scored'] else ' (not scored)'}: {c['detail']}")
    p, n = score(result)
    print(f"{p}/{n}")
    sys.exit(0)


if __name__ == "__main__":
    main()
