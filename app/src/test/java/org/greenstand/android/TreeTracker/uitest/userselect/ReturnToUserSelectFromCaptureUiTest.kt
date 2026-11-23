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
package org.greenstand.android.TreeTracker.uitest.userselect

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScope
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.CaptureFlowNavigationController
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.destinations
import org.greenstand.android.TreeTracker.navigation.orgRepoWith
import org.greenstand.android.TreeTracker.navigation.rememberScreenTrackingNavEntryDecorator
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.userselect.UserSelectAction
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Leaving capture through the profile button (`CaptureFlowNavigationController.goToUserSelect`)
 * ends the session and closes the capture setup scope, then shows user select again.
 *
 * Renders the same `NavDisplay` entry decorators as `Host` (saveable state, ViewModel store,
 * screen tracking), so the user select entry gets its ViewModel the way it does in the app.
 */
class ReturnToUserSelectFromCaptureUiTest : ComposeUiTest() {
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private var userSelectViewModelsCreated = 0

    private val captureFlowNav =
        CaptureFlowNavigationController(
            orgRepo = orgRepoWith(captureFlow = destinations("capture/{profilePicUrl}")),
            stepCounter = mockk(relaxed = true),
            sessionTracker = mockk(relaxed = true),
            locationDataCapturer = mockk(relaxed = true),
            treeCapturer = mockk(relaxed = true),
            deviceOrientation = mockk(relaxed = true),
        )

    @Before
    fun setUp() {
        CaptureFlowScopeManager.close()
        CaptureSetupScopeManager.close()
        every { userRepo.users() } returns flowOf(userSelectTestUsers)
        stopKoin()
        startKoin {
            modules(
                module {
                    single<ExceptionDataCollector> { mockk(relaxed = true) }
                    factory {
                        userSelectViewModelsCreated++
                        UserSelectViewModel(
                            userId = null,
                            userRepo = userRepo,
                            messageRepo = mockk(relaxed = true),
                            locationDataCapturer = mockk(relaxed = true),
                            prefs = mockk(relaxed = true),
                        )
                    }
                    scope<CaptureSetupScope> {
                        scoped { CaptureSetupData(mockk(relaxed = true)) }
                        scoped { setupNav }
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        CaptureFlowScopeManager.close()
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    private fun setNavDisplay() {
        backStack.clear()
        backStack.addAll(listOf(DashboardRoute, UserSelectRoute))
        setContent(inspectionMode = false) {
            CompositionLocalProvider(LocalViewModelFactory provides TreeTrackerViewModelFactory()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { navigator.popBackStack() },
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                            rememberScreenTrackingNavEntryDecorator(),
                        ),
                    entryProvider =
                        entryProvider {
                            entry<DashboardRoute> { BasicText("Dashboard") }
                            entry<UserSelectRoute> { UserSelectScreen() }
                            entry<AddOrgRoute> { BasicText("Add org") }
                            entry<TreeCaptureRoute> { BasicText("Capture") }
                        },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Selects [userName] on user select, then finishes setup into capture (setup keeps user select under it). */
    private fun selectUserAndStartCapture(userName: String) {
        composeTestRule.onNodeWithText(userName).performClick()
        composeTestRule.runOnIdle { navigator.navigate(TreeCaptureRoute(profilePicUrl = "")) }
        composeTestRule.onNodeWithText("Capture").assertExists()
    }

    @Test
    fun `returning to user select from capture reopens the setup scope so Next works`() {
        setNavDisplay()
        selectUserAndStartCapture(ANN_NAME)

        composeTestRule.runOnIdle { captureFlowNav.goToUserSelect(navigator) }
        composeTestRule.waitForIdle()

        assertEquals(listOf(UserSelectRoute), backStack.toList())
        assertTrue(
            "Returning to user select from capture reused the previous UserSelectViewModel " +
                "($userSelectViewModelsCreated created), so the capture setup scope closed by " +
                "goToUserSelect was never reopened: selecting a user or tapping Next would crash " +
                "with 'CaptureSetupScope not open'",
            CaptureSetupScopeManager.isOpen,
        )

        composeTestRule.onNodeWithText(BEN_NAME).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(userSelectTestUsers[1], CaptureSetupScopeManager.getData().user)
        coVerify(exactly = 1) { setupNav.navForward(any()) }
    }

    @Test
    fun `returning to user select from capture clears the previous selection`() {
        setNavDisplay()
        selectUserAndStartCapture(ANN_NAME)

        composeTestRule.runOnIdle { captureFlowNav.goToUserSelect(navigator) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(null, CaptureSetupScopeManager.getData().user)
        coVerify(exactly = 0) { setupNav.navForward(any()) }
    }

    @Test
    fun `back onto user select from a later setup screen keeps the setup data and selection`() {
        setNavDisplay()
        composeTestRule.onNodeWithText(ANN_NAME).performClick()
        val setupData = CaptureSetupScopeManager.getData()
        composeTestRule.runOnIdle { navigator.navigate(AddOrgRoute) }
        composeTestRule.onNodeWithText("Add org").assertExists()

        composeTestRule.runOnIdle { navigator.popBackStack() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertSame(setupData, CaptureSetupScopeManager.getData())
        assertEquals(userSelectTestUsers[0], setupData.user)
        assertEquals(1, userSelectViewModelsCreated)
        coVerify(exactly = 1) { setupNav.navForward(any()) }
    }

    @Test
    fun `after another user is deleted in Settings, Next on the returning user select sets up capture for the highlighted user`() {
        setNavDisplay()
        selectUserAndStartCapture(ANN_NAME)
        composeTestRule.runOnIdle { captureFlowNav.goToUserSelect(navigator) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(BEN_NAME).performClick()
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText("Dashboard").assertExists()
        assertEquals(listOf(UserSelectRoute, DashboardRoute), backStack.toList())

        val ann = userSelectTestUsers[0]
        coEvery { userRepo.deleteUser(ann.wallet) } returns true
        composeTestRule.runOnIdle {
            val deleteAccount =
                UserSelectViewModel(
                    userId = null,
                    userRepo = userRepo,
                    messageRepo = mockk(relaxed = true),
                    locationDataCapturer = mockk(relaxed = true),
                    prefs = mockk(relaxed = true),
                )
            deleteAccount.handleAction(UserSelectAction.SelectUser(ann))
            deleteAccount.handleAction(UserSelectAction.DeleteUser)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { navigator.navigate(UserSelectRoute) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            "Next on user select, with $BEN_NAME highlighted, set up capture for another user " +
                "(the deleted $ANN_NAME, or nobody)",
            userSelectTestUsers[1],
            CaptureSetupScopeManager.getData().user,
        )
        coVerify(exactly = 1) { setupNav.navForward(any()) }
    }
}
