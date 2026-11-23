#!/usr/bin/env python3
"""Tests of the `comment` job's script in .github/workflows/e2e-affected.yml: the before/after
table it writes into the PR description (between the e2e-videos markers) and the PR comment.
The script is taken out of the workflow file and run with node against a stand-in for the GitHub
API, so what is tested is what CI runs. Skipped without node.

    python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import json
import shutil
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
START, END = "<!-- e2e-videos:start -->", "<!-- e2e-videos:end -->"
SHA = "a" * 40

HARNESS = r"""
const fs = require('fs');
const input = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
Object.assign(process.env, input.env);
process.chdir(input.cwd);
const calls = [];
const github = {
  paginate: async () => input.comments,
  rest: {
    issues: {
      listComments: 'listComments',
      updateComment: async a => { calls.push(['updateComment', a]); },
      createComment: async a => { calls.push(['createComment', a]); },
    },
    checks: { listForRef: async a => { calls.push(['listChecks', a]); return { data: { check_runs: input.checks } }; } },
    actions: { reRunWorkflow: async a => { calls.push(['rerun', a]); if (input.rerunFails) throw new Error('nope'); } },
    pulls: {
      get: async () => ({ data: { body: input.body } }),
      update: async a => { calls.push(['updatePull', a]); },
    },
  },
};
const context = { repo: { owner: 'o', repo: 'r' }, serverUrl: 'https://github.com' };
const core = { warning: m => calls.push(['warning', m]) };
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
new AsyncFunction('github', 'context', 'core', 'require', fs.readFileSync(process.argv[3], 'utf8'))(github, context, core, require)
  .then(() => console.log(JSON.stringify(calls)))
  .catch(e => { console.error(e); process.exit(1); });
"""


def workflow_script():
    """The github-script of the `comment` job, as text."""
    lines = (ROOT / ".github/workflows/e2e-affected.yml").read_text().splitlines()
    job = next(i for i, l in enumerate(lines) if l.startswith("  comment:"))
    start = next(i for i in range(job, len(lines)) if lines[i].strip() == "script: |")
    indent = len(lines[start]) - len(lines[start].lstrip()) + 2
    body = []
    for l in lines[start + 1:]:
        if l.strip() and len(l) - len(l.lstrip()) < indent:
            break
        body.append(l[indent:])
    return "\n".join(body)


@unittest.skipUnless(shutil.which("node"), "needs node")
class DescriptionBlockTest(unittest.TestCase):
    def run_script(self, body, validate="e2e/flows/my-flow.yaml", verdict="validated", comments=(), flows="",
                   checks=(), rerun_fails=False):
        tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, tmp, True)
        if verdict:
            side = lambda status: {"status": status, "ran": True, "error": None if status == "passed" else "Assertion is false: id: Done is visible",  # noqa: E731
                                   "recording": "saved"}
            before = side("passed" if verdict == "not-demonstrated" else "failed")
            before["video"] = "before/videos/my-flow.mp4"
            after = side("passed")
            after["video"] = "after/videos/my-flow.mp4"
            (tmp / "results/validate/before/videos").mkdir(parents=True)
            (tmp / "results/validate/after/videos").mkdir(parents=True)
            (tmp / "results/validate/before/videos/my-flow.gif").write_text("")
            (tmp / "results/validate/after/videos/my-flow.gif").write_text("")
            (tmp / "results/validate/verdict.json").write_text(json.dumps(
                {"flows": [{"name": "my-flow", "verdict": verdict, "why": None, "before": before, "after": after}]}))
        (tmp / "script.js").write_text(workflow_script())
        (tmp / "harness.js").write_text(HARNESS)
        (tmp / "input.json").write_text(json.dumps({
            "cwd": str(tmp), "body": body, "comments": list(comments), "checks": list(checks), "rerunFails": rerun_fails,
            "env": {"PR": "7", "SHA": SHA, "TABLE": "### Before and after: 1 validating flow", "FLOWS": flows,
                    "VALIDATE": validate, "BEFORE": "b" * 40, "ARTIFACT": "art", "BRANCH": "flow-results/affected-pr-7",
                    "RESULTS_SHA": "c" * 40, "RUN_URL": "https://github.com/o/r/actions/runs/1"}}))
        r = subprocess.run(["node", str(tmp / "harness.js"), str(tmp / "input.json"), str(tmp / "script.js")],
                           capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        calls = json.loads(r.stdout)
        updates = [c[1]["body"] for c in calls if c[0] == "updatePull"]
        return (updates[0] if updates else None), calls

    def test_fills_the_marked_block_and_keeps_the_rest(self):
        body = f"Intro.\n\nvalidates: my-flow\n\n{START}\nstale\n{END}\n\nWhat the videos show.\n"
        new, calls = self.run_script(body)
        self.assertTrue(new.startswith("Intro.\n\nvalidates: my-flow\n\n" + START))
        self.assertTrue(new.endswith(END + "\n\nWhat the videos show.\n"))
        self.assertNotIn("stale", new)
        block = new[new.index(START):new.index(END)]
        self.assertIn("**Validated**", block)
        # Both videos: a GIF that plays inline and a link to the MP4, at the results commit.
        for side in ("before", "after"):
            self.assertIn(f'<img src="https://raw.githubusercontent.com/o/r/{"c" * 40}/validate/{side}/videos/my-flow.gif"', block)
            self.assertIn(f"https://github.com/o/r/blob/{'c' * 40}/validate/{side}/videos/my-flow.mp4", block)
        self.assertIn("Assertion is false", block)
        self.assertIn("--record --annotate", block)
        self.assertEqual(new.count(START), 1)
        self.assertEqual([c[0] for c in calls], ["createComment", "updatePull", "listChecks"])

    def test_a_second_run_changes_nothing_else(self):
        first, _ = self.run_script(f"Intro.\n\n{START}\n{END}\n\nOutro.\n")
        second, _ = self.run_script(first)
        self.assertIsNone(second, "the same result written twice is not an edit")
        third, _ = self.run_script(first, verdict="not-demonstrated")
        self.assertIn("Does not demonstrate the change", third)
        self.assertNotIn("**Validated**", third)
        self.assertTrue(third.endswith(END + "\n\nOutro.\n"))
        self.assertEqual(third.count(START), 1)

    def test_without_markers_the_block_is_added_at_the_end(self):
        new, _ = self.run_script("Intro.\n\nvalidates: my-flow\n\n\n")
        self.assertTrue(new.startswith("Intro.\n\nvalidates: my-flow\n\n" + START))
        self.assertTrue(new.rstrip().endswith(END))

    def test_no_validating_flow(self):
        new, _ = self.run_script("Just a description.", validate="", verdict=None, flows="e2e/flows/x.yaml")
        self.assertIsNone(new, "no markers and nothing to show: the description is left alone")
        new, _ = self.run_script(f"Intro.\n{START}\nold videos\n{END}\n", validate="", verdict=None, flows="e2e/flows/x.yaml")
        self.assertNotIn("old videos", new)
        self.assertIn("No validating flow ran", new)

    def test_the_media_check_runs_again_when_it_is_not_green(self):
        failed = {"status": "completed", "conclusion": "failure",
                  "details_url": "https://github.com/o/r/actions/runs/987/job/654"}
        _, calls = self.run_script(f"{START}\n{END}\n", checks=[failed])
        self.assertEqual([c[0] for c in calls][-3:], ["updatePull", "listChecks", "rerun"])
        self.assertEqual(calls[-2][1]["ref"], SHA)
        self.assertEqual(calls[-2][1]["check_name"], "Check for screenshot & video")
        self.assertEqual(calls[-1][1]["run_id"], 987)
        # Already green, or still running: left alone.
        for check in ({**failed, "conclusion": "success"}, {**failed, "status": "in_progress", "conclusion": None}):
            _, calls = self.run_script(f"{START}\n{END}\n", checks=[check])
            self.assertNotIn("rerun", [c[0] for c in calls])
        # A re-run that is refused is a warning, not a failed job.
        _, calls = self.run_script(f"{START}\n{END}\n", checks=[failed], rerun_fails=True)
        self.assertEqual(calls[-1][0], "warning")

    def test_the_block_is_not_read_as_a_validates_line(self):
        import validating
        new, _ = self.run_script(f"{START}\n{END}\n")
        self.assertEqual(validating.named(new), [])

    def test_a_description_that_would_get_too_long_is_left_alone(self):
        new, calls = self.run_script("x" * 64900 + f"\n{START}\n{END}\n")
        self.assertIsNone(new)
        self.assertIn("warning", [c[0] for c in calls])


if __name__ == "__main__":
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    unittest.main()
