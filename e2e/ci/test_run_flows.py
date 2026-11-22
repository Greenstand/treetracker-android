"""Tests for run-flows.sh's selectors (name@N cuts), ANNOTATE and lost recordings, with stand-in
adb and tb.

Run: python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import json
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

# A tb that records: it loses the recording (as tb does when its adb pull fails) on its first
# FAKE_LOSE runs, and saves one after that.
FAKE_TB_RECORDING = """#!/bin/sh
flow=$2
shift 2
out=
while [ $# -gt 0 ]; do
  [ "$1" = --debug-output ] && out=$2
  shift
done
echo "$(basename "$flow")" >> "$FAKE_LOG"
runs=$(wc -l < "$FAKE_LOG")
if [ "$runs" -le "${FAKE_LOSE:-0}" ]; then
  echo "    log: --record: the recording could not be saved: adb pull /sdcard/tb-recording-1.mp4 x: exit status 1: "
else
  mkdir -p "$out/Flow" && echo video > "$out/Flow/recording.mp4"
fi
echo "1 passed"
exit ${FAKE_EXIT:-0}
"""


class RunFlowsTest(unittest.TestCase):
    def run_flows(self, *selectors, tb=FAKE_TB, read=None, **env):
        with tempfile.TemporaryDirectory() as d:
            bin_dir = Path(d, "bin")
            bin_dir.mkdir()
            for name, text in [("adb", FAKE_ADB), ("tb", tb)]:
                Path(bin_dir, name).write_text(text)
                Path(bin_dir, name).chmod(0o755)
            log = Path(d, "log")
            full_env = dict(os.environ, PATH=f"{bin_dir}:{os.environ['PATH']}", ADB="adb", TB="tb",
                            OUT=str(Path(d, "out")), SCREENSHOTS="none", FAKE_LOG=str(log),
                            PYTHONDONTWRITEBYTECODE="1", **env)
            r = subprocess.run(["bash", str(SCRIPT), "-s", "emulator-5554", *selectors],
                               cwd=ROOT, env=full_env, capture_output=True, text=True)
            lines = log.read_text().splitlines() if log.exists() else []
            if read:
                return r, lines, read(Path(d, "out"))
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
        # (This stand-in tb saves no video, so the run ends with status 3: recording lost.)
        r, lines = self.run_flows("signup-back@2", ANNOTATE="1", RECORD_TRIES="1")
        self.assertEqual(r.returncode, 3, r.stderr)
        self.assertTrue(lines[0].endswith("--flatten-debug-output --record --annotate"), lines[0])
        r, lines = self.run_flows("signup-back@2", ANNOTATE="2s", RECORD_TRIES="1")
        self.assertTrue(lines[0].endswith("--record --annotate=2s"), lines[0])

    def summary(self, out):
        files = sorted(p.relative_to(out).as_posix() for p in out.rglob("*") if p.is_file())
        return json.loads((out / "summary.json").read_text()), (out / "summary.md").read_text(), files

    def test_a_lost_recording_runs_the_flow_again(self):
        r, lines, (summary, md, files) = self.run_flows(
            "signup-back@2", tb=FAKE_TB_RECORDING, read=self.summary, ANNOTATE="1", FAKE_LOSE="1")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertEqual(len(lines), 2)
        flow = summary["flows"][0]
        self.assertEqual((flow["recording"], flow["tries"], flow["video"], flow["recordingError"]),
                         ("saved", 2, "videos/signup-back.mp4", None))
        self.assertEqual(summary["recordingsLost"], 0)
        self.assertIn("🎬 saved (try 2)", md)
        # The try that lost its video keeps its log.
        self.assertIn("flows/signup-back/console-try1.txt", files)
        self.assertIn("could not be saved", r.stdout)

    def test_a_recording_lost_every_time_is_said_and_fails_the_run(self):
        r, lines, (summary, md, files) = self.run_flows(
            "signup-back@2", tb=FAKE_TB_RECORDING, read=self.summary, ANNOTATE="1", FAKE_LOSE="9")
        self.assertEqual(r.returncode, 3, r.stdout + r.stderr)
        self.assertEqual(len(lines), 3)   # RECORD_TRIES defaults to 3
        flow = summary["flows"][0]
        self.assertEqual((flow["status"], flow["recording"], flow["tries"], flow["video"]),
                         ("passed", "lost", 3, None))
        self.assertIn("adb pull", flow["recordingError"])
        self.assertEqual(summary["recordingsLost"], 1)
        self.assertIn("1 recording lost", md.splitlines()[0])
        self.assertIn("⚠️ **lost**", md)
        self.assertIn("the recording was lost after 3 tries", md)
        self.assertIn("::warning::signup-back: the recording was lost", r.stdout)

    def test_record_tries_1_does_not_run_again_and_a_failed_flow_wins(self):
        r, lines, (summary, md, files) = self.run_flows(
            "signup-back@2", tb=FAKE_TB_RECORDING, read=self.summary, ANNOTATE="1", FAKE_LOSE="9",
            RECORD_TRIES="1", FAKE_EXIT="1")
        self.assertEqual(r.returncode, 1)
        self.assertEqual(len(lines), 1)
        self.assertEqual(summary["flows"][0]["recording"], "lost")

    def test_a_saved_recording_runs_once_and_no_video_asked_has_no_video_column(self):
        r, lines, (summary, md, files) = self.run_flows(
            "signup-back@2", tb=FAKE_TB_RECORDING, read=self.summary, ANNOTATE="1")
        self.assertEqual((r.returncode, len(lines)), (0, 1))
        self.assertEqual((summary["flows"][0]["recording"], summary["flows"][0]["tries"]), ("saved", 1))
        self.assertNotIn("flows/signup-back/attempts.txt", files)
        r, lines, (summary, md, files) = self.run_flows("signup-back@2", read=self.summary)
        self.assertEqual(summary["flows"][0]["recording"], None)
        self.assertNotIn("Video", md)

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
