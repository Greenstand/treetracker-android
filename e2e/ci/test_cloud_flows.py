#!/usr/bin/env python3
"""Tests of cloud-flows.sh's `run`: what it asks GitHub to start, with a stand-in for gh and a
local repository as origin. No network.

    python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "cloud-flows.sh"
# Records each call; answers the dispatch with a run id, and the first status poll with an error.
GH = r"""#!/usr/bin/env bash
echo "$*" >> "$GH_LOG"
case "$*" in
  *dispatches*) echo 4242 ;;
  *actions/runs/4242*)
    n=$(cat "$GH_LOG.polls" 2>/dev/null || echo 0); echo $((n + 1)) > "$GH_LOG.polls"
    if [[ $n == 0 ]]; then echo "gh: Not Found (HTTP 404)" >&2; exit 1; fi
    echo "completed success" ;;
  *pulls*) ;;
esac
"""


class CloudFlowsRunTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        t = Path(self.tmp.name)
        self.repo, origin, results = t / "repo", t / "origin.git", t / "results"
        self.git(t, "init", "-q", "--bare", "-b", "master", origin)
        self.git(t, "init", "-q", "-b", "master", self.repo)
        (self.repo / "a").write_text("1")
        self.base = self.commit("base")
        self.git(self.repo, "remote", "add", "origin", origin)
        self.git(self.repo, "push", "-q", "origin", "master")
        self.git(self.repo, "fetch", "-q", "origin")
        self.git(self.repo, "checkout", "-q", "-b", "fix")
        (self.repo / "a").write_text("2")
        self.commit("fix")
        self.git(self.repo, "push", "-q", "origin", "fix")
        # What the run would push: a results branch with a verdict.
        self.git(t, "init", "-q", "-b", "flow-results/run-4242", results)
        (results / "validate").mkdir()
        (results / "summary.json").write_text('{"runId": "4242"}')
        (results / "summary.md").write_text("### Flow run: 0 passed, 0 failed\n")
        (results / "validate/verdict.md").write_text("| `my-flow` | validated |\n")
        self.git(results, "add", "-A")
        self.git(results, "commit", "-q", "-m", "results")
        self.git(results, "push", "-q", origin, "flow-results/run-4242")
        bin_dir = t / "bin"
        bin_dir.mkdir()
        (bin_dir / "gh").write_text(GH)
        (bin_dir / "gh").chmod(0o755)
        self.log = t / "gh.log"
        self.env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}", GH_LOG=str(self.log), FLOW_POLL_SECONDS="1")

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, cwd, *args):
        return subprocess.run(["git", "-c", "user.name=t", "-c", "user.email=t@t", *map(str, args)], cwd=cwd,
                              check=True, capture_output=True, text=True).stdout.strip()

    def commit(self, msg):
        self.git(self.repo, "add", "-A")
        self.git(self.repo, "commit", "-q", "-m", msg)
        return self.git(self.repo, "rev-parse", "HEAD")

    def run_script(self, *args):
        return subprocess.run(["bash", str(SCRIPT), *args], cwd=self.repo, env=self.env, capture_output=True, text=True)

    def test_validate_starts_a_before_and_after_run_and_prints_the_verdict(self):
        r = self.run_script("run", "--validate", "my-flow", "--out", "out")
        self.assertEqual(r.returncode, 0, r.stderr)
        dispatch = next(l for l in self.log.read_text().splitlines() if "dispatches" in l)
        self.assertIn("ref=fix", dispatch)
        self.assertIn("inputs[validate_flows]=my-flow", dispatch)
        # Before: the merge base with origin/master, as a full sha.
        self.assertIn(f"inputs[before_ref]={self.base}", dispatch)
        # The first poll answered 404 (a run that was only just started): it waits, not gives up.
        self.assertIn("completed success", r.stdout)
        self.assertIn("| `my-flow` | validated |", r.stdout)
        self.assertTrue((self.repo / "out/validate/verdict.md").is_file())

    def test_before_can_be_given(self):
        r = self.run_script("run", "--validate", "my-flow", "--before", "HEAD", "--out", "out", "--no-wait")
        self.assertEqual(r.returncode, 0, r.stderr)
        head = self.git(self.repo, "rev-parse", "HEAD")
        self.assertIn(f"inputs[before_ref]={head}", self.log.read_text())

    def test_a_plain_run_sends_no_validate_inputs(self):
        r = self.run_script("run", "settings-logout", "--no-wait")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertNotIn("validate_flows", self.log.read_text())

    def test_nothing_to_run_is_refused(self):
        r = self.run_script("run")
        self.assertEqual(r.returncode, 2)
        self.assertFalse(self.log.exists())


if __name__ == "__main__":
    unittest.main()
