"""Tests for run-flows.sh's selectors (name@N cuts) and ANNOTATE, with stand-in adb and tb.

Run: python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import os
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

CI = Path(__file__).resolve().parent
ROOT = CI.parents[1]
SCRIPT = CI / "run-flows.sh"

FAKE_ADB = "#!/bin/sh\nexit 0\n"
# Logs the flow it was given, the step count of that copy and its other arguments.
FAKE_TB = """#!/bin/sh
flow=$2
n=$(grep -c '^- ' "$flow")
shift 2
echo "$(basename "$flow") steps=$n $*" >> "$FAKE_LOG"
exit 0
"""


class RunFlowsTest(unittest.TestCase):
    def run_flows(self, *selectors, **env):
        with tempfile.TemporaryDirectory() as d:
            bin_dir = Path(d, "bin")
            bin_dir.mkdir()
            for name, text in [("adb", FAKE_ADB), ("tb", FAKE_TB)]:
                Path(bin_dir, name).write_text(text)
                Path(bin_dir, name).chmod(0o755)
            log = Path(d, "log")
            full_env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}", ADB="adb", TB="tb",
                            OUT=str(Path(d, "out")), SCREENSHOTS="none", FAKE_LOG=str(log),
                            PYTHONDONTWRITEBYTECODE="1", **env)
            r = subprocess.run(["bash", str(SCRIPT), "-s", "emulator-5554", *selectors],
                               cwd=ROOT, env=full_env, capture_output=True, text=True)
            lines = log.read_text().splitlines() if log.exists() else []
            return r, lines

    def steps(self, name):
        return len(re.findall(r"^- ", (ROOT / "e2e/flows" / f"{name}.yaml").read_text(), re.M))

    def test_cut_whole_and_two_cuts(self):
        r, lines = self.run_flows("signup-back@4", "settings-logout@3", "signup-back",
                                  "settings-logout@5", "capture-exit@2")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual([l.split(" --")[0].split(" -p")[0] for l in lines], [
            f"signup-back.yaml steps={self.steps('signup-back')}",   # whole wins over a cut
            ".ci-cut-settings-logout.yaml steps=5",                  # the longer of two cuts
            ".ci-cut-capture-exit.yaml steps=2",
        ])
        self.assertEqual(list(ROOT.glob("e2e/flows/**/.ci-*.yaml")), [])

    def test_bad_cuts_are_refused(self):
        for sel in ["signup-back@0", "signup-back@08", "tag:settings@3", "settings-*@3", "signup-back@x"]:
            r, lines = self.run_flows(sel)
            self.assertEqual(r.returncode, 2, sel)
            self.assertEqual(lines, [], sel)

    def test_annotate_records_with_tb(self):
        r, lines = self.run_flows("signup-back@2", ANNOTATE="1")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertTrue(lines[0].endswith("--flatten-debug-output --record --annotate"), lines[0])
        r, lines = self.run_flows("signup-back@2", ANNOTATE="2s")
        self.assertTrue(lines[0].endswith("--record --annotate=2s"), lines[0])

    def test_without_new_options_tb_gets_the_old_arguments(self):
        r, lines = self.run_flows("tag:settings")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertTrue(lines)
        for l in lines:
            self.assertNotIn(".ci-cut-", l)
            self.assertTrue(l.endswith("--flatten-debug-output"), l)

    def test_no_bash_4_only_features(self):
        # macOS ships bash 3.2: no associative arrays, mapfile, case changes or |&.
        text = SCRIPT.read_text()
        for pattern in [r"declare -A", r"\bmapfile\b", r"\breadarray\b", r"\$\{\w+(,,|\^\^)", r"\|&"]:
            self.assertIsNone(re.search(pattern, text), pattern)


if __name__ == "__main__":
    unittest.main()
