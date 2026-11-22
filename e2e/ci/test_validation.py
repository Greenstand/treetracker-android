"""Tests for the before/after check: validating.py (which flows), verdict.py (the 2x2) and
run-job.sh (both runs, with stand-in adb and tb).

Run: python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

CI = Path(__file__).resolve().parent
ROOT = CI.parents[1]
sys.path.insert(0, str(CI))
import validating  # noqa: E402
import verdict  # noqa: E402


class ValidatingTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        flows = self.root / "e2e/flows"
        (flows / "subflows").mkdir(parents=True)
        for name, tags in [("a", "[x]"), ("b", "[x]"), ("c", "[x]"), ("bug", "[x, known-bug]")]:
            (flows / f"{name}.yaml").write_text(f"appId: x\ntags: {tags}\n---\n- launchApp\n")

    def tearDown(self):
        self.tmp.cleanup()

    def pick(self, body, changed=(), limit=3):
        picks, skipped = validating.pick(self.root, body, list(changed), limit)
        return [p for p, _ in picks], skipped

    def test_named_flows_in_every_spelling(self):
        body = "Fixes #1\n\n- Validates: a, b.yaml e2e/flows/c.yaml\nvalidates: bug\n"
        self.assertEqual(self.pick(body)[0], ["e2e/flows/a.yaml", "e2e/flows/b.yaml", "e2e/flows/c.yaml"])

    def test_changed_flows_follow_named_ones_without_repeats(self):
        picks, skipped = self.pick("validates: b", ["e2e/flows/a.yaml", "e2e/flows/b.yaml"])
        self.assertEqual(picks, ["e2e/flows/b.yaml", "e2e/flows/a.yaml"])
        self.assertEqual(skipped, [])

    def test_known_bug_flows_only_when_named(self):
        picks, skipped = self.pick("", ["e2e/flows/bug.yaml"])
        self.assertEqual(picks, [])
        self.assertIn("known-bug", skipped[0][1])
        self.assertEqual(self.pick("validates: bug", ["e2e/flows/bug.yaml"])[0], ["e2e/flows/bug.yaml"])

    def test_words_that_are_not_flows_are_refused(self):
        body = "validates: ../../etc/passwd $(reboot) a;b subflows/x nope `a` a\n"
        picks, skipped = self.pick(body)
        self.assertEqual(picks, ["e2e/flows/a.yaml"])
        self.assertEqual([why for _, why in skipped],
                         ["not a flow's name"] * 4 + ["no such flow", "not a flow's name"])
        for what, _ in skipped:
            self.assertRegex(what, r"^[ -~]*$")

    def test_limit(self):
        picks, skipped = self.pick("validates: a b c", ["e2e/flows/bug.yaml"], limit=2)
        self.assertEqual(len(picks), 2)
        self.assertIn(("e2e/flows/c.yaml", "over the limit of 2 flows"), skipped)

    def test_no_line_no_changes(self):
        self.assertEqual(self.pick("nothing here\nrun-flows: a\n"), ([], []))
        self.assertIn("no validating flow", validating.markdown([], []))


def flow(name, status, error=None, recording="saved"):
    return {"name": name, "status": status, "seconds": 5, "error": error,
            "video": f"videos/{name}.mp4" if recording == "saved" else None,
            "recording": recording, "recordingError": None, "screenshots": []}


class VerdictTest(unittest.TestCase):
    def run_verdict(self, before, after, missing=None):
        with tempfile.TemporaryDirectory() as d:
            out = Path(d)
            for side, flows in (("before", before), ("after", after)):
                if flows is not None:
                    (out / side).mkdir()
                    (out / side / "summary.json").write_text(json.dumps({"flows": flows}))
            if missing:
                (out / "before-missing.txt").write_text(missing + "\n")
            r = subprocess.run([sys.executable, "-B", str(CI / "verdict.py"), d], capture_output=True, text=True)
            return r.returncode, json.loads((out / "verdict.json").read_text()), (out / "verdict.md").read_text()

    def test_the_four_outcomes(self):
        before = [flow("fix", "failed", "Element not found: X"), flow("same", "passed"),
                  flow("bad", "failed", "boom"), flow("regress", "passed")]
        after = [flow("fix", "passed"), flow("same", "passed"),
                 flow("bad", "failed", "boom"), flow("regress", "failed", "boom")]
        code, v, md = self.run_verdict(before, after)
        self.assertEqual(code, 1)
        self.assertEqual({f["name"]: f["verdict"] for f in v["flows"]},
                         {"fix": "validated", "same": "not-demonstrated", "bad": "broken", "regress": "broken"})
        self.assertEqual(v["counts"], {"validated": 1, "not-demonstrated": 1, "broken": 2, "inconclusive": 0})
        self.assertEqual(v["flows"][0]["before"]["video"], "before/videos/fix.mp4")
        self.assertIn("**`fix`** before: Element not found: X", md)
        self.assertIn("this PR makes it fail", md)

    def test_validated_alone_passes(self):
        code, v, _ = self.run_verdict([flow("fix", "failed", "x")], [flow("fix", "passed")])
        self.assertEqual(code, 0)

    def test_no_base_app_is_inconclusive_not_validated(self):
        code, v, md = self.run_verdict(None, [flow("fix", "passed")], missing="the base app (abc) did not build")
        self.assertEqual((code, v["flows"][0]["verdict"]), (0, "inconclusive"))
        self.assertIn("did not build", v["flows"][0]["why"])

    def test_setup_failure_is_inconclusive(self):
        code, v, _ = self.run_verdict([flow("fix", "failed", "Setup failed: see setup.txt")], [flow("fix", "passed")])
        self.assertEqual(v["flows"][0]["verdict"], "inconclusive")
        code, v, _ = self.run_verdict([flow("fix", "failed", "x")], [flow("fix", "failed", "Setup failed: see setup.txt")])
        self.assertEqual((code, v["flows"][0]["verdict"]), (0, "inconclusive"))

    def test_lost_recording_is_shown(self):
        _, v, md = self.run_verdict([flow("fix", "failed", "x", recording="lost")], [flow("fix", "passed")])
        self.assertEqual(v["flows"][0]["verdict"], "validated")
        self.assertIn("recording lost", md)


FAKE_ADB = """#!/bin/sh
case " $* " in
  *" install "*) for a; do last=$a; done; cp "$last" "$FAKE_DIR/installed.apk" ;;
esac
exit 0
"""
# Passes on the "after" app; on the "before" app, fails the flow named signup-back. Records.
FAKE_TB = """#!/bin/sh
flow=$2
shift 2
out=
while [ $# -gt 0 ]; do
  [ "$1" = --debug-output ] && out=$2
  shift
done
app=$(cat "$FAKE_DIR/installed.apk" 2>/dev/null)
echo "$app $(basename "$flow")" >> "$FAKE_DIR/log"
mkdir -p "$out/Flow" && echo video > "$out/Flow/recording.mp4"
case "$app $flow" in
  before*signup-back*) echo "Element not found" > "$out/Flow/error.txt"; exit 1 ;;
esac
exit 0
"""


class RunJobTest(unittest.TestCase):
    def run_job(self, before_apk=True, **env):
        with tempfile.TemporaryDirectory() as d:
            bin_dir = Path(d, "bin")
            bin_dir.mkdir()
            for name, text in [("adb", FAKE_ADB), ("tb", FAKE_TB)]:
                Path(bin_dir, name).write_text(text)
                Path(bin_dir, name).chmod(0o755)
            Path(d, "after.apk").write_text("after")
            if before_apk:
                Path(d, "before.apk").write_text("before")
            Path(d, "installed.apk").write_text("after")
            out = Path(d, "out")
            # No ffmpeg on PATH for the GIFs: the stand-in videos are not videos.
            full_env = dict(os.environ, PATH=f"{bin_dir}:/usr/bin:/bin", ADB="adb", TB="tb", OUT=str(out),
                            SCREENSHOTS="none", FAKE_DIR=d, AFTER_APK=str(Path(d, "after.apk")),
                            BEFORE_APK=str(Path(d, "before.apk")), PYTHONDONTWRITEBYTECODE="1", **env)
            full_env.pop("ANNOTATE", None)
            r = subprocess.run(["bash", str(CI / "run-job.sh"), "-s", "emulator-5554"], cwd=ROOT,
                               env=full_env, capture_output=True, text=True)
            log = Path(d, "log").read_text().splitlines() if Path(d, "log").exists() else []
            verdict_json = out / "validate/verdict.json"
            v = json.loads(verdict_json.read_text()) if verdict_json.exists() else None
            summary = json.loads((out / "summary.json").read_text()) if (out / "summary.json").exists() else None
            return r, log, v, summary

    def test_before_and_after(self):
        r, log, v, summary = self.run_job(VALIDATE_FLOWS="e2e/flows/signup-back.yaml first-screen", FLOW_SELECTORS="")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertEqual([l.split()[0] for l in log], ["before", "before", "after", "after"])
        self.assertEqual({f["name"]: f["verdict"] for f in v["flows"]},
                         {"signup-back": "validated", "first-screen": "not-demonstrated"})
        self.assertEqual(v["flows"][0]["before"]["video"], "before/videos/signup-back.mp4")
        self.assertEqual(summary["flows"], [])

    def test_chosen_flows_run_first_on_the_pr_app(self):
        r, log, v, summary = self.run_job(VALIDATE_FLOWS="first-screen", FLOW_SELECTORS="signup-back@2")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertEqual([l.split()[0] for l in log], ["after", "before", "after"])
        self.assertEqual([f["name"] for f in summary["flows"]], ["signup-back"])

    def test_without_a_base_app_the_verdict_is_inconclusive(self):
        r, log, v, _ = self.run_job(before_apk=False, VALIDATE_FLOWS="first-screen", FLOW_SELECTORS="",
                                    BEFORE_MISSING="the base app (abc) did not build")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertEqual(v["flows"][0]["verdict"], "inconclusive")
        self.assertIn("did not build", v["flows"][0]["why"])

    def test_without_validating_flows_it_is_run_flows(self):
        r, log, v, summary = self.run_job(FLOW_SELECTORS="first-screen")
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        self.assertIsNone(v)
        self.assertEqual([f["name"] for f in summary["flows"]], ["first-screen"])
        r, *_ = self.run_job(FLOW_SELECTORS="")
        self.assertEqual(r.returncode, 2)


if __name__ == "__main__":
    unittest.main()
