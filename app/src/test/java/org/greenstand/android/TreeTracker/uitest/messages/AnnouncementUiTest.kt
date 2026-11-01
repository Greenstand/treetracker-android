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

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.greenstand.android.TreeTracker.messages.announcementmessage.Announcement
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementAction
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementScreen
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementState
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementViewModel
import org.greenstand.android.TreeTracker.models.messages.AnnouncementMessage
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.navigation.AnnouncementRoute
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

class AnnouncementUiTest : ComposeUiTest() {
    private val actions = mutableListOf<AnnouncementAction>()

    private val fullState =
        AnnouncementState(
            from = "greenstand",
            currentTitle = TITLE,
            currentBody = BODY,
            currentUrl = URL,
        )

    @Test
    fun `announcement shows the sender in upper case`() {
        setContent { Announcement(state = fullState) }

        composeTestRule.onNodeWithText("GREENSTAND").assertIsDisplayed()
    }

    @Test
    fun `announcement shows the title, body and link`() {
        setContent { Announcement(state = fullState) }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(BODY).assertIsDisplayed()
        composeTestRule.onNodeWithText(URL).assertIsDisplayed()
    }

    @Test
    fun `announcement without title, body or link renders none of them`() {
        setContent { Announcement(state = AnnouncementState(from = "greenstand")) }

        composeTestRule.onNodeWithText("GREENSTAND").assertIsDisplayed()
        composeTestRule.onNodeWithText(TITLE).assertDoesNotExist()
        composeTestRule.onNodeWithText(BODY).assertDoesNotExist()
        composeTestRule.onNodeWithText(URL).assertDoesNotExist()
    }

    @Test
    fun `announcement only offers a back button`() {
        setContent { Announcement(state = fullState) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `clicking back sends NavigateBack`() {
        setContent { Announcement(state = fullState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(AnnouncementAction.NavigateBack), actions) }
    }

    @Test
    fun `clicking the link sends OpenLink with the url`() {
        setContent { Announcement(state = fullState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithText(URL).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(AnnouncementAction.OpenLink(URL)), actions) }
    }

    @Test
    fun `screen shows the announcement loaded by the view model`() {
        val viewModel = AnnouncementViewModel(MESSAGE_ID, fakeRepo())
        setContent(inspectionMode = false) { AnnouncementScreen(MESSAGE_ID, viewModel) }

        composeTestRule.onNodeWithText("GREENSTAND").assertIsDisplayed()
        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(BODY).assertIsDisplayed()
        composeTestRule.onNodeWithText(URL).assertIsDisplayed()
    }

    @Test
    fun `screen marks the announcement as read`() {
        val repo = fakeRepo()
        val viewModel = AnnouncementViewModel(MESSAGE_ID, repo)
        setContent(inspectionMode = false) { AnnouncementScreen(MESSAGE_ID, viewModel) }

        composeTestRule.waitForIdle()

        coVerify { repo.markMessageAsRead(MESSAGE_ID) }
    }

    @Test
    fun `clicking back on the screen pops the announcement`() {
        backStack.add(IndividualMessageListRoute(planterInfoId = 1))
        backStack.add(AnnouncementRoute(messageId = MESSAGE_ID))
        val viewModel = AnnouncementViewModel(MESSAGE_ID, fakeRepo())
        setContent(inspectionMode = false) { AnnouncementScreen(MESSAGE_ID, viewModel) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, IndividualMessageListRoute(planterInfoId = 1)), backStack.toList())
        }
    }

    @Test
    fun `clicking the link on the screen opens it in another app`() {
        val viewModel = AnnouncementViewModel(MESSAGE_ID, fakeRepo())
        setContent(inspectionMode = false) { AnnouncementScreen(MESSAGE_ID, viewModel) }

        composeTestRule.onNodeWithText(URL).performClick()

        composeTestRule.runOnIdle {
            val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(Uri.parse(URL), intent.data)
        }
    }

    private fun fakeRepo(): MessagesRepo {
        val repo = mockk<MessagesRepo>(relaxed = true)
        coEvery { repo.getAnnouncementMessages(MESSAGE_ID) } returns
            AnnouncementMessage(
                id = MESSAGE_ID,
                from = "greenstand",
                to = "planter@example.com",
                composedAt = "2026-01-01T00:00:00Z",
                isRead = false,
                subject = TITLE,
                body = BODY,
                videoLink = URL,
            )
        return repo
    }

    private companion object {
        const val MESSAGE_ID = "announcement-1"
        const val TITLE = "Planting season"
        const val BODY = "Planting season starts next week."
        const val URL = "https://greenstand.org/video"
    }
}