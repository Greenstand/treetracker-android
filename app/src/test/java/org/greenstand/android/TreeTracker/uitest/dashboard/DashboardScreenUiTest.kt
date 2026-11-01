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
package org.greenstand.android.TreeTracker.uitest.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.background.NotificationConstants
import org.greenstand.android.TreeTracker.dashboard.DashboardScreen
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.usecases.CheckForInternetUseCase
import org.greenstand.android.TreeTracker.view.APPROVE_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DashboardScreenUiTest : RootScreenUiTest() {
    private val dao = mockk<TreeTrackerDAO>()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val analytics = mockk<Analytics>(relaxed = true)
    private val treesToSyncHelper = mockk<TreesToSyncHelper>(relaxed = true)
    private val orgRepo = mockk<OrgRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private val checkForInternetUseCase = mockk<CheckForInternetUseCase>()
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)

    @Before
    fun setUp() {
        every { workManager.getWorkInfosForUniqueWorkLiveData(any()) } returns MutableLiveData<List<WorkInfo>>()
        coEvery { checkForInternetUseCase.execute(Unit) } returns false
        every { dao.getUploadedLegacyTreeImageCountFlow() } returns flowOf(0)
        every { dao.getUploadedTreeImageCountFlow() } returns flowOf(0)
        every { dao.getNonUploadedLegacyTreeCaptureImageCountFlow() } returns flowOf(0)
        startKoinWith(languageButtonModule())
    }

    private fun launchDashboard(treesToUpload: Int): DashboardViewModel {
        every { dao.getNonUploadedTreeImageCountFlow() } returns flowOf(treesToUpload)
        val viewModel =
            DashboardViewModel(
                dao = dao,
                workManager = workManager,
                analytics = analytics,
                treesToSyncHelper = treesToSyncHelper,
                orgRepo = orgRepo,
                messagesRepo = messagesRepo,
                checkForInternetUseCase = checkForInternetUseCase,
                locationDataCapturer = locationDataCapturer,
            )
        setRootContent { DashboardScreen(viewModel = viewModel) }
        composeTestRule.waitUntil { viewModel.state.value.treesRemainingToSync == treesToUpload }
        return viewModel
    }

    @Test
    fun `shows the trees waiting to upload`() {
        launchDashboard(treesToUpload = 37)

        composeTestRule.onNodeWithText("37").assertIsDisplayed()
    }

    @Test
    fun `track opens user select when below the sync reminder threshold`() {
        launchDashboard(treesToUpload = 5)

        composeTestRule.onNodeWithText(string(R.string.track)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(string(R.string.upload_trees_soon_title)).assertDoesNotExist()
        assertEquals(listOf(DashboardRoute, UserSelectRoute), backStack.toList())
    }

    @Test
    fun `track shows the sync reminder instead of navigating at the threshold`() {
        launchDashboard(treesToUpload = SYNC_REMINDER_THRESHOLD)

        composeTestRule.onNodeWithText(string(R.string.track)).performClick()

        composeTestRule.onNodeWithText(string(R.string.upload_trees_soon_title)).assertExists()
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    @Test
    fun `confirming the sync reminder opens user select`() {
        launchDashboard(treesToUpload = SYNC_REMINDER_THRESHOLD)
        composeTestRule.onNodeWithText(string(R.string.track)).performClick()

        composeTestRule.onNodeWithTag(APPROVE_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, UserSelectRoute), backStack.toList())
    }

    @Test
    fun `messages syncs messages and opens messages user select`() {
        launchDashboard(treesToUpload = 0)

        composeTestRule.onNodeWithText(string(R.string.messages)).performClick()
        composeTestRule.waitForIdle()

        coVerify { checkForInternetUseCase.execute(Unit) }
        assertEquals(listOf(DashboardRoute, MessagesUserSelectRoute), backStack.toList())
    }

    @Test
    fun `settings icon opens settings`() {
        launchDashboard(treesToUpload = 0)

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }

    @Test
    fun `upload with trees to sync starts the sync worker`() {
        launchDashboard(treesToUpload = 5)

        composeTestRule.onNodeWithText(string(R.string.upload)).performClick()
        composeTestRule.waitForIdle()

        verify {
            workManager.enqueueUniqueWork(
                NotificationConstants.UNIQUE_WORK_ID,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
        verify { analytics.syncButtonTapped(5, 0, 5) }
        assertEquals(listOf(DashboardRoute), backStack.toList())
    }

    private companion object {
        const val SYNC_REMINDER_THRESHOLD = 2000
    }
}