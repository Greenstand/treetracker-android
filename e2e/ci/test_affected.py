"""Tests for affected.py and cut_flow.py, on a small made-up app and its flows.

Run: python3 -m unittest discover -s e2e/ci -p 'test_*.py'
"""
import json
import os
import shutil
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path

import sys

import affected
from affected import Change, select, selectors
from cut_flow import cut
import join_recording
import flowfile

CI = Path(__file__).resolve().parent
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
        - back
        - back
        - back
        - back
        - back
        - back
        - back
        - tapOn: {id: SettingsLogout}
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

    def test_tag_forms(self):
        cases = {
            "tags: [a, b]\nname: y": {"a", "b"},
            "tags: [a, known-bug] # until #12 is fixed": {"a", "known-bug"},
            "tags: a": {"a"},
            "tags:\n  - a\n  - known-bug": {"a", "known-bug"},
            "tags:\n- dev-tools\n- known-bug\nname: y": {"dev-tools", "known-bug"},
            "tags:\n  - a\n  # the bug\n  - known-bug": {"a", "known-bug"},
            "tags:\n  - 'a'\n  - \"b\"  # quoted": {"a", "b"},
            "name: y": set(),
        }
        for header, want in cases.items():
            self.assertEqual(affected.flow_tags(f"appId: x\n{header}\n---\n- back\n"), want, header)

    def test_at_the_limit_a_picked_flow_runs_further(self):
        # User select is only in capture (cut at 5). Settings is cheaper in settings-logout, but
        # with room for one flow, capture runs on to its own settings step.
        r = self.pick(f"{PKG}/userselect/UserSelectScreen.kt", f"{PKG}/settings/SettingsScreen.kt",
                      max_flows=1)
        self.assertEqual(selectors(r), ["e2e/flows/capture.yaml"])
        self.assertEqual(r["skipped"], [])
        # With room, each gets its own short flow.
        r = self.pick(f"{PKG}/userselect/UserSelectScreen.kt", f"{PKG}/settings/SettingsScreen.kt")
        self.assertEqual(sorted(selectors(r)),
                         ["e2e/flows/capture.yaml@5", "e2e/flows/settings-logout.yaml@4"])

    def test_control_characters_in_paths_stay_on_one_line(self):
        md = affected.markdown(select([Change("docs/a\nb.txt")], self.root, 6), 6)
        self.assertIn("`docs/a\\x0ab.txt`", md)

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


class JoinRecordingMainTest(unittest.TestCase):
    """join_recording.py on real (tiny) videos."""

    def make(self, d, names):
        for i, name in enumerate(names):
            subprocess.run(["ffmpeg", "-loglevel", "error", "-f", "lavfi", "-i",
                            "color=c=red:s=32x32:d=1:r=30", "-c:v", "libx264", "-pix_fmt",
                            "yuv420p", str(Path(d, name + ".mp4"))], check=True)
            Path(d, name + ".srt").write_text(f"1\n00:00:00,100 --> 00:00:00,900\nstep {i + 1}\n")

    def run_main(self, src, out, env=None):
        return subprocess.run([sys.executable, "-B", str(Path(join_recording.__file__)), src, out],
                              capture_output=True, text=True, env=env)

    @unittest.skipUnless(shutil.which("ffmpeg") and shutil.which("ffprobe"), "needs ffmpeg")
    def test_parts_joined(self):
        with tempfile.TemporaryDirectory() as d:
            src = Path(d, "tb", "flow")
            src.mkdir(parents=True)
            self.make(src, ["recording", "recording-part2"])
            r = self.run_main(str(Path(d, "tb")), str(Path(d, "out", "f")))
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertEqual(sorted(p.name for p in Path(d, "out").iterdir()), ["f.mp4", "f.srt"])
            self.assertIn("00:00:01,100 --> 00:00:01,900\nstep 2", Path(d, "out", "f.srt").read_text())
            self.assertEqual(list(src.iterdir()), [])

    @unittest.skipUnless(shutil.which("ffmpeg"), "needs ffmpeg")
    def test_without_ffmpeg_parts_stay_apart(self):
        with tempfile.TemporaryDirectory() as d:
            src = Path(d, "tb")
            src.mkdir()
            self.make(src, ["recording", "recording-part2"])
            env = dict(os.environ, PATH=str(Path(sys.executable).parent))
            r = self.run_main(str(src), str(Path(d, "f")), env)
            self.assertEqual(r.returncode, 0, r.stderr)
            for name in ["f.mp4", "f.srt", "f-part2.mp4", "f-part2.srt"]:
                self.assertTrue(Path(d, name).exists(), name)

    @unittest.skipUnless(shutil.which("ffmpeg") and shutil.which("ffprobe"), "needs ffmpeg")
    def test_unreadable_parts_stay_apart_and_nothing_is_left(self):
        with tempfile.TemporaryDirectory() as d, tempfile.TemporaryDirectory() as tmp:
            src = Path(d, "tb")
            src.mkdir()
            for name in ["recording.mp4", "recording-part2.mp4"]:
                Path(src, name).write_text("not a video")
            r = self.run_main(str(src), str(Path(d, "f")), dict(os.environ, TMPDIR=tmp))
            self.assertEqual(r.returncode, 0, r.stderr)
            self.assertTrue(Path(d, "f.mp4").exists() and Path(d, "f-part2.mp4").exists())
            self.assertEqual(list(Path(tmp).iterdir()), [])

    def test_no_recording(self):
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual(self.run_main(d, str(Path(d, "f"))).returncode, 1)


class SummarizeTest(unittest.TestCase):
    def test_four_and_five_columns_and_video_parts(self):
        with tempfile.TemporaryDirectory() as d:
            out = Path(d)
            for name in ["a", "b"]:
                Path(out, "flows", name).mkdir(parents=True)
            Path(out, "videos").mkdir()
            for f in ["b.mp4", "b-part2.mp4", "b-part10.mp4", "b.srt"]:
                Path(out, "videos", f).write_text("")
            Path(out, "results.tsv").write_text("a\te2e/flows/a.yaml\tpassed\t3\n"
                                                "b\te2e/flows/b.yaml\tfailed\t4\t7\n")
            subprocess.run([sys.executable, "-B", str(CI / "summarize.py"), d], check=True,
                           capture_output=True)
            flows = json.loads(Path(out, "summary.json").read_text())["flows"]
            self.assertEqual([f["steps"] for f in flows], [None, 7])
            self.assertEqual(flows[1]["videoParts"], ["videos/b-part2.mp4", "videos/b-part10.mp4"])
            self.assertEqual(flows[1]["captions"], "videos/b.srt")
            self.assertIn("`b` (steps 1–7)", Path(out, "summary.md").read_text())


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
