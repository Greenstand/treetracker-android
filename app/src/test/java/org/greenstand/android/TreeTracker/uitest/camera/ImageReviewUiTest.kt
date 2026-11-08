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
package org.greenstand.android.TreeTracker.uitest.camera

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.greenstand.android.TreeTracker.activities.CaptureImageContract
import org.greenstand.android.TreeTracker.camera.ImageReview
import org.greenstand.android.TreeTracker.camera.ImageReviewScreen
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.ImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.SelfieRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Shadows.shadowOf

/** Tests for the stateless [ImageReview] selfie review and the reject and approve paths of [ImageReviewScreen]. */
class ImageReviewUiTest : ComposeUiTest() {
    private var rejectClicks = 0
    private var approveClicks = 0

    private fun render() {
        setContent {
            ImageReview(
                photoPath = PHOTO_PATH,
                onRejectClicked = { rejectClicks++ },
                onApproveClicked = { approveClicks++ },
            )
        }
    }

    @Test
    fun `review shows approve and reject buttons`() {
        render()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `reject button reports a reject`() {
        render()

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()

        assertEquals(1, rejectClicks)
        assertEquals(0, approveClicks)
    }

    @Test
    fun `approve button reports an approve`() {
        render()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()

        assertEquals(1, approveClicks)
        assertEquals(0, rejectClicks)
    }

    @Test
    fun `rejecting on the root screen returns to a single selfie screen`() {
        backStack.add(SelfieRoute)
        backStack.add(ImageReviewRoute(photoPath = PHOTO_PATH))
        setContent(inspectionMode = false) {
            ImageReviewScreen(photoPath = PHOTO_PATH)
        }

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SelfieRoute), backStack.toList())
    }

    /** Renders [ImageReviewScreen] and returns the activity hosting it. */
    private fun renderScreen(): Activity {
        lateinit var activity: Activity
        backStack.add(SelfieRoute)
        backStack.add(ImageReviewRoute(photoPath = PHOTO_PATH))
        setContent(inspectionMode = false) {
            activity = LocalContext.current as Activity
            ImageReviewScreen(photoPath = PHOTO_PATH)
        }
        composeTestRule.waitForIdle()
        return activity
    }

    @Test
    fun `approving on the root screen returns the photo path and finishes the capture activity`() {
        val activity = renderScreen()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val shadowActivity = shadowOf(activity)
        assertEquals(AppCompatActivity.RESULT_OK, shadowActivity.resultCode)
        assertEquals(PHOTO_PATH, shadowActivity.resultIntent.getStringExtra(CaptureImageContract.TAKEN_IMAGE_PATH))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `rejecting on the root screen keeps the capture activity open`() {
        val activity = renderScreen()

        composeTestRule.onNodeWithTag(DECLINE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertFalse(activity.isFinishing)
    }

    private companion object {
        const val PHOTO_PATH = "/no/such/selfie.jpg"
    }
}