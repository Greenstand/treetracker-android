#!/usr/bin/env python3
"""Trigger eval of the pr-validating-flow skill: does a session in this repository load the skill
for prompts where it should, and leave it alone where it should not? Graded by code: a run counts
as triggered when its transcript has a Skill call for pr-validating-flow (or a Read of its
SKILL.md) within the first MAX_TOOLS tool calls.

    run_trigger.py [--runs 3] [--jobs 6] [--set trigger.json] [--json OUT]

Each query runs `claude -p` in a clean copy of this checkout with read-only tools, so nothing is
edited, pushed or opened. An agent may read code before it loads the skill; it gets MAX_TOOLS tool
calls and TIMEOUT seconds to do so.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
WORK = Path(os.environ.get("EVAL_WORK", Path.home() / ".cache/pr-validating-flow-eval")).resolve()
SKILL = "pr-validating-flow"
MAX_TOOLS = 12
TIMEOUT = 300


def run_one(query, repo):
    cmd = ["claude", "-p", query, "--output-format", "stream-json", "--verbose", "--strict-mcp-config",
           "--tools", "Read,Glob,Grep,Skill"]
    if os.environ.get("EVAL_MODEL"):
        cmd += ["--model", os.environ["EVAL_MODEL"]]
    env = {k: v for k, v in os.environ.items() if k != "CLAUDECODE"}
    p = subprocess.Popen(cmd, cwd=repo, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, env=env)
    tools, triggered = 0, False
    try:
        import threading
        timer = threading.Timer(TIMEOUT, p.kill)
        timer.start()
        for line in p.stdout:
            try:
                ev = json.loads(line)
            except ValueError:
                continue
            for block in (ev.get("message") or {}).get("content") or []:
                if not (isinstance(block, dict) and block.get("type") == "tool_use"):
                    continue
                tools += 1
                inp = json.dumps(block.get("input") or {})
                if (block.get("name") == "Skill" and SKILL in inp) or f"{SKILL}/SKILL.md" in inp:
                    triggered = True
            if triggered or tools >= MAX_TOOLS or ev.get("type") == "result":
                break
    finally:
        timer.cancel()
        p.kill()
        p.wait()
    return triggered


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--runs", type=int, default=3)
    ap.add_argument("--jobs", type=int, default=6)
    ap.add_argument("--set", default=str(HERE / "trigger.json"))
    ap.add_argument("--json")
    a = ap.parse_args()
    queries = json.loads(Path(a.set).read_text())
    repo = WORK / "trigger-repo"
    subprocess.run(["git", "-C", str(ROOT), "worktree", "remove", "--force", str(repo)], capture_output=True)
    shutil.rmtree(repo, ignore_errors=True)
    WORK.mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "-C", str(ROOT), "worktree", "add", "-q", "--detach", str(repo), "HEAD"], check=True)
    # The skill under test is the working copy's, committed or not; its eval files stay out.
    shutil.copytree(HERE.parent, repo / ".claude/skills" / SKILL, dirs_exist_ok=True, ignore=shutil.ignore_patterns("evals"))
    shutil.rmtree(repo / ".claude/skills" / SKILL / "evals", ignore_errors=True)
    jobs = [(i, q) for i, q in enumerate(queries) for _ in range(a.runs)]
    with ThreadPoolExecutor(a.jobs) as ex:
        hits = list(ex.map(lambda j: (j[0], run_one(j[1]["query"], repo)), jobs))
    subprocess.run(["git", "-C", str(ROOT), "worktree", "remove", "--force", str(repo)], capture_output=True)
    rows, ok = [], 0
    for i, q in enumerate(queries):
        n = sum(h for j, h in hits if j == i)
        passed = (n * 2 > a.runs) == q["should_trigger"]
        ok += passed
        rows.append({"query": q["query"], "should_trigger": q["should_trigger"], "triggered": n, "runs": a.runs, "pass": passed})
        print(f"{'PASS' if passed else 'FAIL'}  {'should    ' if q['should_trigger'] else 'should not'}  {n}/{a.runs}  {q['query'][:90]}")
    pos = [r for r in rows if r["should_trigger"]]
    neg = [r for r in rows if not r["should_trigger"]]
    summary = {"passed": ok, "total": len(rows),
               "should_trigger_rate": sum(r["triggered"] for r in pos) / max(1, len(pos) * a.runs),
               "should_not_trigger_rate": sum(r["triggered"] for r in neg) / max(1, len(neg) * a.runs)}
    print(json.dumps(summary))
    if a.json:
        Path(a.json).write_text(json.dumps({"summary": summary, "queries": rows}, indent=2) + "\n")
    sys.exit(0 if ok == len(rows) else 1)


if __name__ == "__main__":
    main()
