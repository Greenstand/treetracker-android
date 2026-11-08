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
package org.greenstand.android.TreeTracker.uitest.permissions

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.LocationRationaleDialog
import org.greenstand.android.TreeTracker.models.PermissionDeniedPermanentlyDialog
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Tests for the [LocationRationaleDialog] and [PermissionDeniedPermanentlyDialog] on their own.
 * [PermissionRequestUiTest] cannot reach the rationale dialog because Robolectric never asks
 * to show a rationale, so it is driven here with a fake [PermissionState].
 */
@OptIn(ExperimentalPermissionsApi::class)
class PermissionDialogsUiTest : ComposeUiTest() {
    private val application = RuntimeEnvironment.getApplication()

    private class FakePermissionState : PermissionState {
        var requests = 0

        override val permission: String = Manifest.permission.ACCESS_FINE_LOCATION
        override val status: PermissionStatus = PermissionStatus.Denied(shouldShowRationale = true)

        override fun launchPermissionRequest() {
            requests++
        }
    }

    private val permissionState = FakePermissionState()

    @Before
    fun setUp() {
        backStack.add(TreeCaptureRoute(profilePicUrl = ""))
    }

    private fun approveDialog() {
        composeTestRule.onNode(hasTestTag(TestTags.DIALOG_POSITIVE_BUTTON) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()
    }

    private fun declineDialog() {
        composeTestRule.onNode(hasTestTag(TestTags.DIALOG_NEGATIVE_BUTTON) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()
    }

    /** Returns and clears every activity start Robolectric recorded so far. */
    private fun drainStartedActivities(): List<Intent> = generateSequence { shadowOf(application).nextStartedActivity }.toList()

    @Test
    fun `location rationale explains why location is needed`() {
        setContent { LocationRationaleDialog(navigator = navigator, perm = permissionState) }

        composeTestRule.onNodeWithText(string(R.string.accept_location_permission_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.accept_location_permission_message)).assertIsDisplayed()
    }

    @Test
    fun `accepting the location rationale asks for the permission again and leaves the screen`() {
        setContent { LocationRationaleDialog(navigator = navigator, perm = permissionState) }

        approveDialog()

        assertEquals(1, permissionState.requests)
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `declining the location rationale leaves the screen without asking again`() {
        setContent { LocationRationaleDialog(navigator = navigator, perm = permissionState) }

        declineDialog()

        assertEquals(0, permissionState.requests)
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `permanently denied dialog explains that settings must be opened`() {
        setContent { PermissionDeniedPermanentlyDialog(navigator) }

        composeTestRule.onNodeWithText(string(R.string.open_settings_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.open_settings_message)).assertIsDisplayed()
    }

    @Test
    fun `opening settings from the permanently denied dialog keeps the current screen`() {
        setContent { PermissionDeniedPermanentlyDialog(navigator) }
        drainStartedActivities()

        approveDialog()

        assertTrue(drainStartedActivities().any { it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS })
        assertEquals(listOf(DashboardRoute, TreeCaptureRoute(profilePicUrl = "")), backStack.toList())
    }

    @Test
    fun `declining the permanently denied dialog leaves without opening settings`() {
        setContent { PermissionDeniedPermanentlyDialog(navigator) }
        drainStartedActivities()

        declineDialog()

        assertTrue(drainStartedActivities().none { it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS })
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }
}