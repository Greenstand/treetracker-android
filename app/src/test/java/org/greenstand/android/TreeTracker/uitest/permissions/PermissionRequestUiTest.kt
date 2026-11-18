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
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.PermissionRequest
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowData
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScope
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScope
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.destinations
import org.greenstand.android.TreeTracker.navigation.orgRepoWith
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.TestTags
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for [PermissionRequest] with permissions granted or denied through Robolectric.
 * Robolectric never asks to show a rationale unless a test sets it on the package manager
 * (see [showCameraRationale]).
 *
 * [PermissionRequest] runs on the capture screen, so each test starts inside a capture session:
 * the setup and capture scopes are open, like after Next on the last setup step.
 */
@OptIn(ExperimentalPermissionsApi::class)
class PermissionRequestUiTest : ComposeUiTest() {
    private val application = RuntimeEnvironment.getApplication()
    private val locationManager = mockk<LocationManager>(relaxed = true)
    private val viewModel = PermissionViewModel(locationManager, mockk(relaxed = true))

    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val stepCounter = mockk<StepCounter>(relaxed = true)
    private val deviceOrientation = mockk<DeviceOrientation>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val sessionEnded = CountDownLatch(1)

    @Before
    fun setUp() {
        backStack.add(TreeCaptureRoute(profilePicUrl = ""))
        shadowOf(application).grantPermissions(*LOCATION_PERMISSIONS, Manifest.permission.ACTIVITY_RECOGNITION)
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        coEvery { sessionTracker.endSession() } answers { sessionEnded.countDown() }
        startCaptureSession()
    }

    @After
    fun tearDown() {
        CaptureFlowScopeManager.close()
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    /** Opens the setup and capture scopes as finishing setup and opening capture does. */
    private fun startCaptureSession() {
        CaptureFlowScopeManager.close()
        CaptureSetupScopeManager.close()
        stopKoin()
        startKoin {
            modules(
                module {
                    scope<CaptureSetupScope> {
                        scoped { CaptureSetupData(mockk(relaxed = true)) }
                    }
                    scope<CaptureFlowScope> {
                        scoped { CaptureFlowData() }
                        scoped {
                            CaptureFlowNavigationController(
                                orgRepo = orgRepoWith(captureFlow = destinations("capture/{profilePicUrl}")),
                                stepCounter = stepCounter,
                                sessionTracker = sessionTracker,
                                locationDataCapturer = locationDataCapturer,
                                treeCapturer = mockk<TreeCapturer>(relaxed = true),
                                deviceOrientation = deviceOrientation,
                            )
                        }
                    }
                },
            )
        }
        CaptureSetupScopeManager.open()
        CaptureFlowScopeManager.open()
    }

    /** Camera denied once: Android asks to explain why before asking again. */
    private fun showCameraRationale() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        shadowOf(application.packageManager).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, true)
    }

    private fun clickDialogButton(tag: String) {
        composeTestRule.onNode(hasTestTag(tag) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Asserts that leaving capture from a permission dialog ended the capture session: the
     * session, step counter and orientation sensor are stopped (GPS stops on the main looper,
     * which Robolectric doesn't run here) and the capture scope is closed, while the setup scope
     * stays open because the setup screens are still on the back stack (#1421).
     */
    private fun assertCaptureSessionEnded(dialog: String) {
        assertTrue(
            "$dialog left capture without ending the capture session (SessionTracker.endSession() was never called)",
            sessionEnded.await(SESSION_END_TIMEOUT_S, TimeUnit.SECONDS),
        )
        coVerify(timeout = SESSION_END_TIMEOUT_S * 1_000) {
            stepCounter.disable()
            deviceOrientation.disable()
        }
        assertFalse("$dialog left the capture scope open", CaptureFlowScopeManager.isOpen)
        assertTrue("$dialog closed the setup scope while setup is still on the back stack", CaptureSetupScopeManager.isOpen)
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

        composeTestRule.onNode(hasTestTag(TestTags.DIALOG_NEGATIVE_BUTTON) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `approving the open settings dialog opens the app settings`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        render()
        drainStartedActivities()

        composeTestRule.onNode(hasTestTag(TestTags.DIALOG_POSITIVE_BUTTON) and hasAnyAncestor(isDialog())).performClick()
        composeTestRule.waitForIdle()

        val appSettings = drainStartedActivities().single { it.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS }
        assertEquals(Uri.fromParts("package", application.packageName, null), appSettings.data)
        assertTrue((appSettings.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `denied location permission shows the open settings dialog and skips the gps check`() {
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        shadowOf(application).denyPermissions(*LOCATION_PERMISSIONS)

        render()

        composeTestRule.onAllNodesWithText(string(R.string.open_settings_header)).onFirst().assertIsDisplayed()
        assertEquals(null, viewModel.state.value.isLocationEnabled)
        assertFalse(drainStartedActivities().any { it.action == Settings.ACTION_LOCATION_SOURCE_SETTINGS })
    }

    @Test
    fun `granted permissions never show the camera rationale`() {
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)

        render()

        composeTestRule.onNodeWithText(string(R.string.accept_camera_permission_header)).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.accept_location_permission_header)).assertDoesNotExist()
    }

    // --- Leaving capture from a permission dialog ends the capture session ---

    @Test
    fun `cancelling the camera rationale ends the capture session`() {
        backStack.add(1, AddOrgRoute)
        showCameraRationale()
        render()
        composeTestRule.onNode(hasTestTag(TestTags.CAMERA_RATIONALE_DIALOG)).assertExists()

        clickDialogButton(TestTags.DIALOG_NEGATIVE_BUTTON)

        assertEquals(listOf(DashboardRoute, AddOrgRoute), backStack.toList())
        assertCaptureSessionEnded("Cancel on the camera rationale")
    }

    @Test
    fun `accepting the camera rationale ends the capture session`() {
        backStack.add(1, AddOrgRoute)
        showCameraRationale()
        render()
        composeTestRule.onNode(hasTestTag(TestTags.CAMERA_RATIONALE_DIALOG)).assertExists()

        clickDialogButton(TestTags.DIALOG_POSITIVE_BUTTON)

        assertEquals(listOf(DashboardRoute, AddOrgRoute), backStack.toList())
        assertCaptureSessionEnded("OK on the camera rationale")
    }

    @Test
    fun `declining the open settings dialog ends the capture session`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        render()

        clickDialogButton(TestTags.DIALOG_NEGATIVE_BUTTON)

        assertEquals(listOf(DashboardRoute), backStack.toList())
        assertCaptureSessionEnded("Cancel on the permission settings dialog")
    }

    @Test
    fun `opening the app settings keeps the capture session`() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        render()

        clickDialogButton(TestTags.DIALOG_POSITIVE_BUTTON)

        assertFalse(
            "Opening the app settings ended the capture session",
            sessionEnded.await(1, TimeUnit.SECONDS),
        )
        assertTrue(CaptureFlowScopeManager.isOpen)
        assertTrue(CaptureSetupScopeManager.isOpen)
    }

    @Test
    fun `cancelling the location rationale ends the capture session`() {
        backStack.add(1, AddOrgRoute)
        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        shadowOf(application).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        shadowOf(application.packageManager)
            .setShouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION, true)
        render()
        composeTestRule.onNode(hasTestTag(TestTags.LOCATION_RATIONALE_DIALOG)).assertExists()

        clickDialogButton(TestTags.DIALOG_NEGATIVE_BUTTON)

        assertEquals(listOf(DashboardRoute, AddOrgRoute), backStack.toList())
        assertCaptureSessionEnded("Cancel on the location rationale")
    }

    @Test
    fun `declining the open settings dialog without a capture scope still leaves the screen`() {
        // E.g. the scope was already closed by a first tap on another permission dialog.
        CaptureFlowScopeManager.close()
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        render()

        clickDialogButton(TestTags.DIALOG_NEGATIVE_BUTTON)

        assertEquals(listOf(DashboardRoute), backStack.toList())
        assertTrue(CaptureSetupScopeManager.isOpen)
    }

    private companion object {
        const val SESSION_END_TIMEOUT_S = 5L

        val LOCATION_PERMISSIONS =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
    }
}