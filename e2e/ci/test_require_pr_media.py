#!/usr/bin/env python3
"""Tests of .github/workflows/require-pr-media.yml ("Check for screenshot & video"): its script,
taken out of the workflow file, run with node against a stand-in for the GitHub API. Skipped
without node.

    python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

HARNESS = r"""
const fs = require('fs');
const input = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const out = { failed: null, comments: [] };
const github = {
  paginate: async () => input.comments,
  rest: {
    issues: {
      listComments: 'listComments',
      updateComment: async a => { out.comments.push(['update', a.body]); },
      createComment: async a => { out.comments.push(['create', a.body]); },
    },
    pulls: { get: async a => ({ data: { number: a.pull_number, body: input.liveBody, labels: input.labels.map(name => ({ name })), user: { login: 'someone' } } }) },
  },
};
// The event carries the description as it was when the check first ran.
const context = { repo: { owner: 'o', repo: 'r' }, payload: { pull_request: { number: 7, body: input.eventBody, labels: [], user: { login: 'someone' } } } };
const core = { setFailed: m => { out.failed = m; }, info: () => {} };
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
new AsyncFunction('github', 'context', 'core', fs.readFileSync(process.argv[3], 'utf8'))(github, context, core)
  .then(() => console.log(JSON.stringify(out)))
  .catch(e => { console.error(e); process.exit(1); });
"""

# What e2e-affected.yml writes between the markers for a validated flow.
BLOCK = """<!-- e2e-videos:start -->
### Before and after

| Flow | Before: base app `0303c06` | After: this PR's app | Verdict |
|---|---|---|---|
| `my-flow` | ❌ failed<br><img src="https://raw.githubusercontent.com/o/r/abc/validate/before/videos/my-flow.gif" width="200" alt=""><br>[mp4](https://github.com/o/r/blob/abc/validate/before/videos/my-flow.mp4) | ✅ passed<br><img src="https://raw.githubusercontent.com/o/r/abc/validate/after/videos/my-flow.gif" width="200" alt=""><br>[mp4](https://github.com/o/r/blob/abc/validate/after/videos/my-flow.mp4) | ✅ **Validated** |
<!-- e2e-videos:end -->"""
EMPTY = "Fixes the crash.\n\nvalidates: my-flow\n\n<!-- e2e-videos:start -->\n<!-- e2e-videos:end -->\n"
NO_FLOW = "Fixes a race.\n\nNo validating flow: the bug needs logout inside one database query, which no tap can time.\n\n- [x] This change has **no user-visible / UI effect**\n"


def workflow_script():
    lines = (ROOT / ".github/workflows/require-pr-media.yml").read_text().splitlines()
    start = next(i for i, l in enumerate(lines) if l.strip() == "script: |")
    indent = len(lines[start]) - len(lines[start].lstrip()) + 2
    return "\n".join(l[indent:] for l in lines[start + 1:])


@unittest.skipUnless(shutil.which("node"), "needs node")
class RequirePrMediaTest(unittest.TestCase):
    def check(self, live_body, labels=(), event_body=None, comments=()):
        tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, tmp, True)
        (tmp / "script.js").write_text(workflow_script())
        (tmp / "harness.js").write_text(HARNESS)
        (tmp / "input.json").write_text(json.dumps({
            "liveBody": live_body, "eventBody": live_body if event_body is None else event_body,
            "labels": list(labels), "comments": list(comments)}))
        r = subprocess.run(["node", str(tmp / "harness.js"), str(tmp / "input.json"), str(tmp / "script.js")],
                           capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        return json.loads(r.stdout)

    def test_an_empty_block_has_no_video(self):
        out = self.check(EMPTY)
        self.assertIn("screen recording", out["failed"])

    def test_the_block_ci_writes_is_a_video_and_a_screenshot(self):
        out = self.check(EMPTY.replace("<!-- e2e-videos:start -->\n<!-- e2e-videos:end -->", BLOCK))
        self.assertIsNone(out["failed"])

    def test_a_rerun_reads_the_description_as_it_is_now(self):
        # The event still has the description from before CI wrote the videos into it.
        out = self.check(EMPTY.replace("<!-- e2e-videos:start -->\n<!-- e2e-videos:end -->", BLOCK), event_body=EMPTY)
        self.assertIsNone(out["failed"])
        out = self.check(EMPTY, event_body=EMPTY.replace("<!-- e2e-videos:start -->\n<!-- e2e-videos:end -->", BLOCK))
        self.assertIsNotNone(out["failed"])

    def test_no_validating_flow_waits_for_a_maintainer(self):
        out = self.check(NO_FLOW)
        self.assertIn("maintainer", out["failed"])
        self.assertIn("no-validating-flow", out["comments"][0][1])
        self.assertIsNone(self.check(NO_FLOW, labels=["no-validating-flow"])["failed"])

    def test_the_label_alone_or_a_bare_statement_is_not_enough(self):
        self.assertIsNotNone(self.check("Fixes a race.\n- [x] no user-visible effect\n", labels=["no-validating-flow"])["failed"])
        bare = NO_FLOW.replace("No validating flow: the bug needs logout inside one database query, which no tap can time.", "No validating flow: n/a")
        self.assertIsNotNone(self.check(bare, labels=["no-validating-flow"])["failed"])

    def test_the_pr_template_alone_fails(self):
        template = (ROOT / ".github/pull_request_template.md").read_text()
        out = self.check(template)
        # Its hint names .mp4 inside an HTML comment: that is not a video.
        self.assertIn("screen recording", out["failed"])
        self.assertIn("screenshot", out["failed"])

    def test_a_ui_change_without_a_screenshot_still_fails_with_the_label(self):
        body = NO_FLOW.replace("- [x] This change has **no user-visible / UI effect**\n", "")
        out = self.check(body, labels=["no-validating-flow"])
        self.assertIn("screenshot", out["failed"])


if __name__ == "__main__":
    unittest.main()
