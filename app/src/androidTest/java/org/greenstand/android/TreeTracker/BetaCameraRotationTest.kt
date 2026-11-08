/*
 * Copyright 2026 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.greenstand.android.TreeTracker.activities.ImageCaptureActivity
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.CAPTURE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Opt-in native camera check for a disposable large-screen emulator. The display must be rotated
 * from the host: after the test writes `files/camera-ready-<n>`, set `user_rotation` to `n` and
 * create `files/camera-rotated-<n>`.
 */
@RunWith(AndroidJUnit4::class)
class BetaCameraRotationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun largeScreenSelfieCapturesAfterDisplayRotation() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("betaCameraRotation") == "true")
        check(BuildConfig.API_GATEWAY == "https://test-k8s.treetracker.org")
        val context = instrumentation.targetContext
        check(context.resources.configuration.smallestScreenWidthDp >= 600) { "Configure a large-screen emulator before this test" }
        for (target in listOf(0, 2)) {
            File(context.filesDir, "camera-ready-$target").delete()
            File(context.filesDir, "camera-rotated-$target").delete()
        }
        for (target in listOf(0, 2)) {
            val activity =
                instrumentation.startActivitySync(
                    Intent(context, ImageCaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            try {
                compose.waitUntil(20_000) {
                    compose.onAllNodes(hasTestTag(CAPTURE_BUTTON_TAG)).fetchSemanticsNodes().isNotEmpty()
                }
                if (compose.onAllNodes(hasTestTag(TestTags.TUTORIAL_DONE_BUTTON)).fetchSemanticsNodes().isNotEmpty()) {
                    compose.onNodeWithTag(TestTags.TUTORIAL_DONE_BUTTON).performClick()
                }
                // The host rotates the display while this camera is open, including 180 degrees.
                File(context.filesDir, "camera-ready-$target").writeText("ready")
                val applied = File(context.filesDir, "camera-rotated-$target")
                val deadline = SystemClock.uptimeMillis() + 30_000
                while (!applied.exists() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
                assertTrue("Host did not apply rotation", applied.exists())
                val cameraDeadline = SystemClock.uptimeMillis() + 20_000
                var streaming = false
                while (!streaming && SystemClock.uptimeMillis() < cameraDeadline) {
                    instrumentation.runOnMainSync {
                        val preview = findPreview(activity.window.decorView)
                        streaming = preview?.display?.rotation == target &&
                            preview.previewStreamState.value == PreviewView.StreamState.STREAMING
                    }
                    if (!streaming) SystemClock.sleep(200)
                }
                assertTrue("Camera did not stream after rotation", streaming)
                val buttonBounds = compose.onNodeWithTag(CAPTURE_BUTTON_TAG).fetchSemanticsNode().boundsInRoot
                val previewBounds = Rect()
                instrumentation.runOnMainSync {
                    val preview = findPreview(activity.window.decorView) ?: error("No native camera preview")
                    preview.getGlobalVisibleRect(previewBounds)
                    assertEquals(preview.width, preview.height)
                    assertEquals(preview.height, previewBounds.height())
                }
                assertTrue("Camera overlaps shutter", previewBounds.bottom <= buttonBounds.top)
                compose.onNodeWithTag(CAPTURE_BUTTON_TAG).performClick()
                compose.waitUntil(20_000) {
                    compose.onAllNodes(hasTestTag(APPROVE_BUTTON_TAG)).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(APPROVE_BUTTON_TAG).assertExists()
            } finally {
                instrumentation.runOnMainSync { activity.finish() }
            }
        }
    }

    private fun findPreview(view: View): PreviewView? {
        if (view is PreviewView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                findPreview(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }
}