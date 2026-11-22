#!/usr/bin/env python3
"""Tests of grade.py on small git repositories made here.

    python3 -m unittest discover -s .claude/skills/pr-validating-flow/evals -p 'test_*.py'
"""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import grade  # noqa: E402

FLOW = "appId: x\nname: A flow\ntags: [{tags}]\n---\n- launchApp\n- assertVisible: {{id: Done}}\n"


class GradeTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        self.git("init", "-q")
        self.write("e2e/flows/old.yaml", FLOW.format(tags="settings"))
        self.write("e2e/run-org.sh", "session-note org-old.yaml\n")
        self.write("app/src/main/A.kt", "class A\n")
        self.base = self.commit()

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, *args):
        return subprocess.run(["git", "-C", str(self.repo), "-c", "user.name=t", "-c", "user.email=t@t", *args],
                              check=True, capture_output=True, text=True).stdout.strip()

    def write(self, path, text):
        f = self.repo / path
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(text)

    def commit(self):
        self.git("add", "-A")
        self.git("commit", "-q", "-m", "x")
        return self.git("rev-parse", "HEAD")

    def checks(self, kind, verdict=None, transcripts=()):
        self.commit()
        return {c["id"]: c["pass"] for c in grade.grade(self.repo, self.base, kind, verdict, transcripts)["checks"]}

    def test_a_good_flow_pr(self):
        self.write("app/src/main/A.kt", "class A { fun fixed() = 1 }\n")
        self.write("e2e/flows/new-flow.yaml", FLOW.format(tags="settings"))
        self.write("PR_DESCRIPTION.md", "Fix.\n\nvalidates: new-flow\n")
        verdict = {"flows": [{"name": "new-flow", "verdict": "validated"}]}
        c = self.checks("flow", verdict)
        for cid in ("new_flow_added", "validates_line_names_new_flow", "new_flow_not_known_bug",
                    "existing_flow_assertions_untouched", "no_test_only_app_code", "ci_verdict_validated"):
            self.assertTrue(c[cid], cid)
        self.assertIsNone(c["org_flow_listed_in_run_org"])

    def test_a_flow_that_passes_on_both_apps_is_not_validated(self):
        self.write("e2e/flows/new-flow.yaml", FLOW.format(tags="settings"))
        self.write("PR_DESCRIPTION.md", "validates: new-flow\n")
        c = self.checks("flow", {"flows": [{"name": "new-flow", "verdict": "not-demonstrated"}]})
        self.assertFalse(c["ci_verdict_validated"])

    def test_no_flow_and_no_line(self):
        self.write("app/src/main/A.kt", "class A { fun fixed() = 1 }\n")
        self.write("PR_DESCRIPTION.md", "Fixed it.\n")
        c = self.checks("flow")
        self.assertFalse(c["new_flow_added"])
        self.assertFalse(c["validates_line_names_new_flow"])
        self.assertFalse(c["ci_verdict_validated"])

    def test_cheats_are_caught(self):
        self.write("e2e/flows/old.yaml", FLOW.format(tags="settings").replace("- assertVisible: {id: Done}\n", ""))
        self.write("app/src/main/A.kt", "class A { suspend fun f() { delay(500) } }\n")
        self.write("e2e/flows/new-flow.yaml", FLOW.format(tags="settings, known-bug"))
        self.write("e2e/flows/org-new.yaml", FLOW.format(tags="org-config"))
        c = self.checks("flow")
        self.assertFalse(c["existing_flow_assertions_untouched"])
        self.assertFalse(c["no_test_only_app_code"])
        self.assertFalse(c["new_flow_not_known_bug"])
        self.assertFalse(c["org_flow_listed_in_run_org"])

    def test_an_honest_fallback(self):
        self.write("app/src/main/A.kt", "class A { fun fixed() = 1 }\n")
        self.write("app/src/test/ATest.kt", "class ATest\n")
        self.write("PR_DESCRIPTION.md", "No validating flow: a race no tap can hit. Proof: ATest.\n")
        c = self.checks("fallback")
        for cid in ("no_flow_claimed_as_proof", "test_added", "description_names_the_test", "says_no_validating_flow"):
            self.assertTrue(c[cid], cid)

    def test_a_flow_pushed_onto_a_no_symptom_bug_fails(self):
        self.write("e2e/flows/new-flow.yaml", FLOW.format(tags="settings"))
        self.write("PR_DESCRIPTION.md", "validates: new-flow\n")
        c = self.checks("fallback")
        self.assertFalse(c["no_flow_claimed_as_proof"])
        self.assertFalse(c["test_added"])

    def test_transcript_facts(self):
        t = self.repo / "t.jsonl"
        events = [
            {"type": "system", "message": "a string, not a message"},
            {"type": "assistant", "message": {"content": [
                {"type": "tool_use", "name": "Skill", "input": {"skill": "pr-validating-flow"}},
                {"type": "tool_use", "name": "Bash", "input": {"command": "cat /tmp/testbuddy/plugin/skills/tb/SKILL.md"}}]}},
        ]
        t.write_text("\n".join(json.dumps(e) for e in events) + "\nnot json\n")
        facts = grade.transcript_facts([t])
        self.assertTrue(facts["loaded_this_skill"])
        self.assertTrue(facts["used_tb_skill"])


if __name__ == "__main__":
    unittest.main()
