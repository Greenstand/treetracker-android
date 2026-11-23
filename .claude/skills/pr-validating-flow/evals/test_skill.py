#!/usr/bin/env python3
"""Checks that SKILL.md still matches the repository it describes: the files, scripts, options,
markers and tags it names exist. It fails when one of them is renamed without the skill.

    python3 -m unittest discover -s .claude/skills/pr-validating-flow/evals -p 'test_*.py'
"""
import re
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
SKILL = (HERE.parent / "SKILL.md").read_text()
sys.path.insert(0, str(ROOT / "e2e/ci"))


def frontmatter():
    m = re.match(r"---\n(.*?)\n---\n", SKILL, re.S)
    return dict(l.split(": ", 1) for l in m.group(1).splitlines())


class SkillTest(unittest.TestCase):
    def test_frontmatter(self):
        fm = frontmatter()
        self.assertEqual(fm["name"], HERE.parent.name)
        self.assertLessEqual(len(fm["description"]), 1024, "a longer description is cut off in the skill list")
        for word in ("bug-fix", "feature", "pull request", "e2e/flows/", "Not for"):
            self.assertIn(word, fm["description"])

    def test_paths_it_names_exist(self):
        paths = set(re.findall(r"`((?:\.github|\.claude|e2e|docs|app)/[A-Za-z0-9_./-]+)`", SKILL))
        paths |= set(re.findall(r"\bbash (e2e/[A-Za-z0-9_./-]+\.sh)", SKILL))
        self.assertGreater(len(paths), 8)
        missing = sorted(p for p in paths if not (ROOT / p).exists())
        self.assertEqual(missing, [])

    def test_sub_flows_and_tags_it_names_exist(self):
        for name in re.findall(r"`((?:subflows/)?[a-z-]+\.yaml)`", SKILL):
            sub = name if name.startswith("subflows/") else "subflows/" + name
            self.assertTrue((ROOT / "e2e/flows" / sub).is_file(), name)
        flows = "\n".join(p.read_text() for p in (ROOT / "e2e/flows").glob("*.yaml"))
        for tag in ("settings", "capture", "signup", "sad-path", "org-config", "dev-tools"):
            self.assertIn(f"`{tag}`", SKILL)
            self.assertRegex(flows, rf"tags: \[[^\]]*\b{tag}\b")

    def test_the_markers_are_the_ones_ci_writes(self):
        workflow = (ROOT / ".github/workflows/e2e-affected.yml").read_text()
        template = (ROOT / ".github/pull_request_template.md").read_text()
        for marker in ("<!-- e2e-videos:start -->", "<!-- e2e-videos:end -->"):
            self.assertIn(marker, SKILL)
            self.assertIn(marker, workflow)
            self.assertIn(marker, template)

    def test_the_validates_line_is_read_by_ci(self):
        import validating
        example = re.search(r"```markdown\n(.*?)```", SKILL, re.S).group(1)
        self.assertEqual(validating.named(example.replace("<flow-name>", "my-flow")), ["my-flow"])
        # The template's empty line names nothing.
        self.assertEqual(validating.named((ROOT / ".github/pull_request_template.md").read_text()), [])

    def test_commands_it_gives_exist(self):
        cloud = (ROOT / "e2e/ci/cloud-flows.sh").read_text()
        for option in ("--validate", "--affected", "--before"):
            self.assertIn(option + ")", cloud)
        self.assertIn("cloud-flows.sh run --validate", SKILL)
        self.assertIn("cloud-flows.sh fetch --affected", SKILL)
        runner = (ROOT / ".github/workflows/flow-runner.yml").read_text()
        self.assertIn("validate_flows:", runner)
        self.assertIn("before_ref:", runner)
        # The pin the skill reads Test Buddy's docs at.
        pin = re.search(r"TESTBUDDY_REF: ([0-9a-f]{40})", (ROOT / ".github/workflows/android-e2e.yml").read_text())
        self.assertIsNotNone(pin)
        self.assertIn("TESTBUDDY_REF: [0-9a-f]*", SKILL)
        job = (ROOT / ".github/workflows/flow-runner-job.yml").read_text()
        self.assertIn(pin.group(1), job, "the flow runner builds another tb than android-e2e.yml")

    def test_verdicts_match_verdict_py(self):
        import verdict
        for word in ("validated", "does not demonstrate the change", "broken", "inconclusive"):
            self.assertIn(word, SKILL)
            self.assertTrue(any(word in text for text in verdict.TEXT.values()), word)

    def test_what_it_says_about_the_media_check_is_what_ci_does(self):
        check = (ROOT / ".github/workflows/require-pr-media.yml").read_text()
        affected = (ROOT / ".github/workflows/e2e-affected.yml").read_text()
        # The label and the statement the check looks for.
        self.assertIn("`no-validating-flow`", SKILL)
        self.assertIn("'no-validating-flow'", check)
        self.assertIn("`No validating flow: <one sentence", SKILL)
        self.assertIn("no validating flow:", check)
        # CI re-runs the check by its name, reading the live description: no re-save by an agent.
        name = re.search(r"name: (Check for screenshot & video)", check).group(1)
        self.assertIn(f"check_name: '{name}'", affected)
        self.assertIn("reRunWorkflow", affected)
        self.assertIn("github.rest.pulls.get", check)
        self.assertIn("don't save the description again", SKILL)
        self.assertNotIn("Saving\n  the description yourself does", SKILL)

    def test_it_is_linked_where_agents_look(self):
        for f in ("CLAUDE.md", "AGENTS.md", "docs/README.md", "docs/testing/ci-flows.md", ".github/pull_request_template.md"):
            self.assertIn("pr-validating-flow", (ROOT / f).read_text(), f)


if __name__ == "__main__":
    unittest.main()
