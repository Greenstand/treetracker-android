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
package org.greenstand.android.TreeTracker.uitest.messages

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.messages.MessagesUserSelectScreen
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.root.LocalViewModelFactory
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ORANGE_ADD_BUTTON_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Covers what [MessagesUserSelectScreen] adds on top of the shared `UserSelect` screen:
 * no "create user" button, and forward navigation into the selected user's messages.
 */
class MessagesUserSelectScreenUiTest : ComposeUiTest() {
    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.open() } just Runs
        every { CaptureSetupScopeManager.getData() } returns mockk(relaxed = true)

        val userRepo = mockk<UserRepo>(relaxed = true)
        every { userRepo.users() } returns flowOf(listOf(ADA, GRACE))
        stopKoin()
        startKoin {
            modules(
                module {
                    factory {
                        UserSelectViewModel(
                            userId = null,
                            userRepo = userRepo,
                            messageRepo = mockk(relaxed = true),
                            locationDataCapturer = mockk(relaxed = true),
                            prefs = mockk(relaxed = true),
                        )
                    }
                },
            )
        }

        backStack.add(MessagesUserSelectRoute)
        setContent(inspectionMode = false) {
            CompositionLocalProvider(LocalViewModelFactory provides TreeTrackerViewModelFactory()) {
                MessagesUserSelectScreen()
            }
        }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun `every user is listed`() {
        composeTestRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grace Hopper").assertIsDisplayed()
    }

    @Test
    fun `users cannot be created from the messages user select`() {
        composeTestRule.onNodeWithTag(ORANGE_ADD_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `forward opens the messages of the selected user`() {
        composeTestRule.onNodeWithText("Grace Hopper").performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                listOf(DashboardRoute, MessagesUserSelectRoute, IndividualMessageListRoute(planterInfoId = GRACE.id)),
                backStack.toList(),
            )
        }
    }

    @Test
    fun `forward does nothing until a user is selected`() {
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, MessagesUserSelectRoute), backStack.toList())
        }
    }

    @Test
    fun `back returns to the dashboard`() {
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(DashboardRoute), backStack.toList()) }
    }

    private companion object {
        val ADA =
            User(
                id = 1,
                wallet = "ada@example.com",
                numberOfTrees = 4,
                firstName = "Ada",
                lastName = "Lovelace",
                photoPath = "/does/not/exist.jpg",
                isPowerUser = true,
                unreadMessagesAvailable = false,
            )

        val GRACE =
            User(
                id = 2,
                wallet = "grace@example.com",
                numberOfTrees = 9,
                firstName = "Grace",
                lastName = "Hopper",
                photoPath = "/does/not/exist.jpg",
                isPowerUser = false,
                unreadMessagesAvailable = true,
            )
    }
}