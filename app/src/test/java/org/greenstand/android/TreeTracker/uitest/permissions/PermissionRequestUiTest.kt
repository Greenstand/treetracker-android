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
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.PermissionRequest
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.DECLINE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Tests for [PermissionRequest] with permissions granted or denied through Robolectric.
 * The rationale dialogs are not covered: Robolectric never asks to show a rationale.
 */
@OptIn(ExperimentalPermissionsApi::class)
class PermissionRequestUiTest : ComposeUiTest() {
    private val application = RuntimeEnvironment.getApplication()
    private val locationManager = mockk<LocationManager>(relaxed = true)
    private val viewModel = PermissionViewModel(locationManager)

    @Before
    fun setUp() {
        backStack.add(TreeCaptureRoute(profilePicUrl = ""))
        shadowOf(application).grantPermissions(*LOCATION_PERMISSIONS, Manifest.permission.ACTIVITY_RECOGNITION)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
    }

    private fun render() {
        setContent(inspectionMode = false) {
            PermissionRequest(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()
    }

    /** Returns and clears every activity start Robolectric recorded so far. */
    private fun drainStartedActivities(): List<Intent> = generateSequence { shadowOf(application).nextStartedActivity }.toList()

    @Test
    fun `no dialog is shown when every permission is granted and gps is on`() {
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)

        render()

        composeTestRule.onNodeWithText(string(R.string.open_settings_header)).assertDoesNotExist()
        assertEquals(true, viewModel.state.value.isLocationEnabled)
        assertFalse(drainStartedActivities().any { it.action == Settings.ACTION_LOCATION_SOURCE_SETTINGS })
    }

    @Test
    fun `location settings are opened when location is granted but gps is off`() {
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false

        render()

        assertEquals(false, viewModel.state.value.isLocationEnabled)
        assertTrue(drainStartedActivities().any { it.action == Settings.ACTION_LOCATION_SOURCE_SETTINGS })
    }

    @Test
    fun `denied camera permission shows the open settings dialog once`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)

        render()

        composeTestRule.onAllNodesWithText(string(R.string.open_settings_header)).assertCountEquals(1)
        composeTestRule.onNodeWithText(string(R.string.open_settings_header)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.open_settings_message)).assertIsDisplayed()
    }

    @Test
    fun `declining the open settings dialog leaves the screen`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        render()

        composeTestRule.onNode(hasTestTag(DECLINE_BUTTON_TAG) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `approving the open settings dialog opens the app settings`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        render()
        drainStartedActivities()

        composeTestRule.onNode(hasTestTag(APPROVE_BUTTON_TAG) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()

        val appSettings = drainStartedActivities().single { it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS }
        assertEquals(Uri.fromParts("package", application.packageName, null), appSettings.data)
        assertTrue((appSettings.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    private companion object {
        val LOCATION_PERMISSIONS =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
    }
}