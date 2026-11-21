"""Tests for affected.py and cut_flow.py, on a small made-up app and its flows.

Run: python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import json
import tempfile
import textwrap
import unittest
from pathlib import Path

import affected
from affected import Change, select, selectors
from cut_flow import cut
import join_recording

PKG = "app/src/main/java/org/greenstand/android/TreeTracker"

FILES = {
    f"{PKG}/view/TestTags.kt": """
        package org.greenstand.android.TreeTracker.view
        object TestTags {
            const val TRACK_BUTTON = "TrackButton"
            const val SETTINGS_BUTTON = "SettingsButton"
            const val SETTINGS_LOGOUT = "SettingsLogout"
            const val PHONE_FIELD = "PhoneField"
            const val UNUSED = "NeverInAFlow"
            fun userButton(wallet: String) = "UserButton_$wallet"
        }
    """,
    f"{PKG}/view/NavigationButtons.kt": """
        package org.greenstand.android.TreeTracker.view
        const val ARROW_FORWARD_BUTTON_TAG = "ArrowForwardButton"
        @Composable
        fun ArrowButton() { Modifier.testTag(ARROW_FORWARD_BUTTON_TAG) }
    """,
    f"{PKG}/view/UserCard.kt": """
        package org.greenstand.android.TreeTracker.view
        @Composable
        fun UserCard(wallet: String) { Modifier.testTag(TestTags.userButton(wallet)) }
    """,
    f"{PKG}/signup/SignupScreen.kt": """
        package org.greenstand.android.TreeTracker.signup
        import org.greenstand.android.TreeTracker.view.TestTags
        import org.greenstand.android.TreeTracker.view.ArrowButton
        @Composable
        fun SignupScreen() { TextField(Modifier.testTag(TestTags.PHONE_FIELD)); ArrowButton() }
    """,
    f"{PKG}/dashboard/DashboardScreen.kt": """
        package org.greenstand.android.TreeTracker.dashboard
        import org.greenstand.android.TreeTracker.view.TestTags
        import org.greenstand.android.TreeTracker.R
        @Composable
        fun DashboardScreen(vm: DashboardViewModel) {
            Button(Modifier.testTag(TestTags.TRACK_BUTTON), text = stringResource(R.string.track))
            Button(Modifier.testTag(TestTags.SETTINGS_BUTTON))
        }
    """,
    f"{PKG}/dashboard/DashboardViewModel.kt": """
        package org.greenstand.android.TreeTracker.dashboard
        import org.greenstand.android.TreeTracker.models.TreeCounter
        class DashboardViewModel(private val counter: TreeCounter) : ViewModel()
    """,
    f"{PKG}/models/TreeCounter.kt": """
        package org.greenstand.android.TreeTracker.models
        class TreeCounter
    """,
    f"{PKG}/sync/Uploader.kt": """
        package org.greenstand.android.TreeTracker.sync
        class Uploader
    """,
    f"{PKG}/settings/SettingsScreen.kt": """
        package org.greenstand.android.TreeTracker.settings
        import org.greenstand.android.TreeTracker.view.TestTags
        @Composable
        fun SettingsScreen() { Button(Modifier.testTag(TestTags.SETTINGS_LOGOUT)) }
    """,
    f"{PKG}/userselect/UserSelectScreen.kt": """
        package org.greenstand.android.TreeTracker.userselect
        import org.greenstand.android.TreeTracker.view.UserCard
        import org.greenstand.android.TreeTracker.view.ArrowButton
        @Composable
        fun UserSelectScreen() { UserCard("a"); ArrowButton() }
    """,
    f"{PKG}/profile/ProfileSelectScreen.kt": """
        package org.greenstand.android.TreeTracker.profile
        import org.greenstand.android.TreeTracker.view.UserCard
        @Composable
        fun ProfileSelectScreen() { UserCard("a") }
    """,
    "app/src/main/res/values/strings.xml": """
        <resources>
            <string name="track">Track</string>
        </resources>
    """,
    "e2e/flows/subflows/signup.yaml": """
        appId: x
        ---
        - tapOn: {id: PhoneField}
        - inputText: "555"
        - tapOn: {id: ArrowForwardButton}
        - extendedWaitUntil:
            visible: {id: TrackButton}
            timeout: 15000
    """,
    # 1 + 4 (signup) = 5 steps to the Dashboard, then Settings.
    "e2e/flows/settings-logout.yaml": """
        appId: x
        name: Logout
        tags: [settings]
        ---
        - launchApp: {clearState: true}
        - runFlow:
            file: subflows/signup.yaml
        - tapOn: {id: SettingsButton}
        - tapOn: {id: SettingsLogout}
        - assertNotVisible: {id: TrackButton}
        - back
    """,
    # Reaches the Dashboard a step later, then user select.
    "e2e/flows/capture.yaml": """
        appId: x
        name: Capture
        tags: [capture]
        ---
        - launchApp: {clearState: true}
        - waitForAnimationToEnd
        - runFlow:
            file: subflows/signup.yaml
        - tapOn: {id: TrackButton}
        - tapOn: {id: "UserButton_${WALLET}"}
        - tapOn: {id: ArrowForwardButton}
        - back
        - back
        - back
    """,
    # Settings with fewer steps, but a known bug: never picked for a changed screen.
    "e2e/flows/settings-bug.yaml": """
        appId: x
        name: Bug
        tags: [known-bug]
        ---
        - tapOn: {id: SettingsLogout}
    """,
    "e2e/ci/affected-overrides.json": json.dumps({
        "screens": {
            "userselect/UserSelectScreen.kt": {"ids": ["UserButton_*"], "after": ["TrackButton"]},
        },
        "ignore": ["docs/**"],
    }),
}


class AffectedTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory()
        cls.root = Path(cls.tmp.name)
        for path, text in FILES.items():
            p = cls.root / path
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(textwrap.dedent(text).lstrip())

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def pick(self, *paths, max_flows=6, lines=None):
        changes = [Change(p, lines=lines or []) for p in paths]
        return select(changes, self.root, max_flows)

    def test_screen_change_runs_one_flow_cut_after_the_screen(self):
        r = self.pick(f"{PKG}/dashboard/DashboardScreen.kt")
        # settings-logout reaches the Dashboard at step 2 (the sign-up sub-flow ends on it) and
        # leaves it after one more step on it (SettingsButton); capture takes a step longer.
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@3"])

    def test_view_model_reaches_its_screen(self):
        r = self.pick(f"{PKG}/dashboard/DashboardViewModel.kt")
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@3"])
        self.assertIn("DashboardViewModel.kt` → `DashboardScreen.kt`", r["picks"][0].reasons[0])

    def test_code_two_hops_away_reaches_the_screen(self):
        r = self.pick(f"{PKG}/models/TreeCounter.kt")
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@3"])

    def test_code_no_screen_uses_selects_nothing(self):
        r = self.pick(f"{PKG}/sync/Uploader.kt", "build.gradle")
        self.assertEqual(selectors(r), [])
        self.assertEqual({c for c, _ in r["unmapped"]}, {f"{PKG}/sync/Uploader.kt", "build.gradle"})

    def test_changed_flow_runs_whole_and_covers_screens_it_shows(self):
        r = self.pick("e2e/flows/capture.yaml", f"{PKG}/dashboard/DashboardScreen.kt")
        self.assertEqual(selectors(r), ["e2e/flows/capture.yaml"])

    def test_one_flow_covers_two_screens(self):
        r = self.pick(f"{PKG}/settings/SettingsScreen.kt", f"{PKG}/dashboard/DashboardScreen.kt")
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@4"])

    def test_known_bug_flows_are_not_picked_for_a_screen(self):
        r = self.pick(f"{PKG}/settings/SettingsScreen.kt")
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@4"])

    def test_subflow_change_runs_one_flow_up_to_it(self):
        r = self.pick("e2e/flows/subflows/signup.yaml")
        self.assertEqual(len(r["picks"]), 1)
        self.assertEqual(r["picks"][0].cut, 2)

    def test_string_change_reaches_the_screen_using_it(self):
        r = self.pick("app/src/main/res/values/strings.xml",
                      lines=['    <string name="track">Track trees</string>'])
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@3"])

    def test_shared_component_shows_once(self):
        r = self.pick(f"{PKG}/view/NavigationButtons.kt")
        self.assertEqual(len(r["picks"]), 1)
        self.assertEqual(r["picks"][0].cut, 2)

    def test_override_after(self):
        # UserButton ids also show the profile select screen; the override says user select is
        # the one after TrackButton.
        r = self.pick(f"{PKG}/userselect/UserSelectScreen.kt")
        self.assertEqual(selectors(r), ["e2e/flows/capture.yaml@5"])

    def test_test_tags_change_reaches_screens_using_the_changed_tags(self):
        r = self.pick(f"{PKG}/view/TestTags.kt",
                      lines=['    const val SETTINGS_LOGOUT = "SettingsLogout"'])
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@4"])

    def test_ignored_and_deleted(self):
        changes = [Change("docs/x.md"), Change(f"{PKG}/settings/SettingsScreen.kt", "D")]
        r = select(changes, self.root, 6)
        self.assertEqual(selectors(r), [])

    def test_limit(self):
        r = self.pick("e2e/flows/capture.yaml", "e2e/flows/settings-logout.yaml", max_flows=1)
        self.assertEqual(len(r["picks"]), 1)
        self.assertEqual(len(r["skipped"]), 1)

    def test_deleted_renamed_and_unknown_flows_are_listed(self):
        changes = [Change("e2e/flows/gone.yaml", "D"), Change("e2e/flows/capture.yaml", "R100"),
                   Change("e2e/flows/not-there.yaml")]
        r = select(changes, self.root, 6)
        self.assertEqual(selectors(r), [])
        self.assertEqual(len(r["unmapped"]), 3)

    def test_flow_in_another_folder_is_a_subflow(self):
        r = self.pick("e2e/flows/subflows/signup.yaml")
        self.assertEqual(selectors(r), ["e2e/flows/settings-logout.yaml@2"])

    def test_values_file_without_names_says_so(self):
        r = self.pick("app/src/main/res/values/strings.xml")
        self.assertIn("no resource names", r["unmapped"][0][1])

    def test_tag_names_match_whole_words(self):
        # TestTags.SETTINGS_BUTTON must not credit a file that only uses SETTINGS_BUTTON_X.
        code = affected.CodeIndex(self.root, affected.FlowIndex(self.root).all_ids())
        f = code.files[f"{PKG}/settings/SettingsScreen.kt"]
        self.assertFalse(code.uses(f, "org.greenstand.android.TreeTracker.view", "SETTINGS", "TestTags"))
        self.assertTrue(code.uses(f, "org.greenstand.android.TreeTracker.view", "SETTINGS_LOGOUT", "TestTags"))

    def test_block_style_tags(self):
        self.assertEqual(affected.flow_tags("appId: x\ntags:\n  - a\n  - known-bug\n---\n"), {"a", "known-bug"})
        self.assertEqual(affected.flow_tags("appId: x\ntags: [a, b]\nname: y\n---\n"), {"a", "b"})

    def test_long_lists_are_capped(self):
        changes = [Change(f"docs/file{i}.txt") for i in range(80)]
        md = affected.markdown(select(changes, self.root, 6), 6)
        self.assertIn("and 30 more", md)

    def test_git_paths_with_unusual_characters(self):
        import subprocess
        with tempfile.TemporaryDirectory() as d:
            run = lambda *a: subprocess.run(["git", "-C", d, *a], check=True, capture_output=True)
            run("init", "-q")
            run("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "--allow-empty", "-m", "a")
            Path(d, "café.txt").write_text("x")
            Path(d, "old.txt").write_text("same\n" * 20)
            run("add", "-A")
            run("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "b")
            run("mv", "old.txt", "new.txt")
            run("-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "c")
            changes = affected.changes_from_git(d, "HEAD~2", "HEAD")
            self.assertEqual({(c.path, c.status[:1]) for c in changes}, {("café.txt", "A"), ("new.txt", "A")})
            changes = affected.changes_from_git(d, "HEAD~1", "HEAD")
            self.assertEqual([(c.path, c.status) for c in changes], [("new.txt", "R100")])

    def test_markdown_lists_everything(self):
        r = self.pick(f"{PKG}/dashboard/DashboardScreen.kt", f"{PKG}/sync/Uploader.kt")
        md = affected.markdown(r, 6)
        self.assertIn("`settings-logout` | steps 1–3 of 6", md)
        self.assertIn("Uploader.kt", md)


class RealRepoTest(unittest.TestCase):
    """The rules on this repository's own app and flows."""

    def test_a_dashboard_change_runs_one_short_flow(self):
        r = select([Change(f"{PKG}/dashboard/DashboardScreen.kt")], affected.ROOT, 6)
        self.assertEqual(len(r["picks"]), 1)
        pick = r["picks"][0]
        self.assertIsNotNone(pick.cut)
        self.assertLess(pick.cut, len(pick.flow.steps))

    def test_every_flow_has_steps(self):
        flows = affected.FlowIndex(affected.ROOT).flows
        self.assertTrue(flows)
        for f in flows.values():
            self.assertTrue(f.steps, f.path)


class JoinRecordingTest(unittest.TestCase):
    def test_parts_in_order_and_captions_shifted(self):
        with tempfile.TemporaryDirectory() as d:
            for name in ["recording-part2.mp4", "recording.mp4", "recording-part10.mp4", "other.mp4"]:
                Path(d, name).write_text("")
            self.assertEqual([p.name for p in join_recording.parts(d)],
                             ["recording.mp4", "recording-part2.mp4", "recording-part10.mp4"])
        self.assertEqual(join_recording.shift("00:00:59,900 --> 00:01:00,100", 61.25),
                         "00:02:01,150 --> 00:02:01,350")

    def test_single_part_is_moved(self):
        with tempfile.TemporaryDirectory() as d:
            Path(d, "tb").mkdir()
            Path(d, "tb", "recording.mp4").write_text("v")
            Path(d, "tb", "recording.srt").write_text("s")
            join_recording.keep_apart(join_recording.parts(Path(d, "tb")), Path(d, "out"))
            self.assertEqual(Path(d, "out.mp4").read_text(), "v")
            self.assertEqual(Path(d, "out.srt").read_text(), "s")


class CutFlowTest(unittest.TestCase):
    def test_cut(self):
        lines = textwrap.dedent("""
            appId: x
            ---
            # first
            - launchApp
            - runFlow:
                file: a.yaml
            # third
            - back
        """).strip().splitlines()
        self.assertEqual(cut(lines, 2), lines[:6])
        self.assertEqual(cut(lines, 3), lines)


if __name__ == "__main__":
    unittest.main()
