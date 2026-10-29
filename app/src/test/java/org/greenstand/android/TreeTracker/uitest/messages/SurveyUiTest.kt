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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.greenstand.android.TreeTracker.messages.survey.Survey
import org.greenstand.android.TreeTracker.messages.survey.SurveyAction
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreen
import org.greenstand.android.TreeTracker.messages.survey.SurveyScreenState
import org.greenstand.android.TreeTracker.messages.survey.SurveyViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.models.messages.SurveyMessage
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.SurveyRoute
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurveyUiTest : ComposeUiTest() {
    private val actions = mutableListOf<SurveyAction>()

    private val questionState = SurveyScreenState(currentQuestion = FIRST_QUESTION)

    @Test
    fun `survey shows the question prompt and its choices`() {
        setContent { Survey(state = questionState) }

        composeTestRule.onNodeWithText(FIRST_QUESTION.prompt).assertIsDisplayed()
        FIRST_QUESTION.choices.forEach { choice ->
            composeTestRule.onNodeWithText(choice).assertIsDisplayed()
        }
    }

    @Test
    fun `survey without a question shows no choices`() {
        setContent { Survey(state = SurveyScreenState()) }

        composeTestRule.onNodeWithText(FIRST_QUESTION.prompt).assertDoesNotExist()
        composeTestRule.onNodeWithText(FIRST_QUESTION.choices.first()).assertDoesNotExist()
    }

    @Test
    fun `clicking a choice selects its index`() {
        setContent { Survey(state = questionState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithText(FIRST_QUESTION.choices[1]).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(SurveyAction.SelectAnswer(1)), actions) }
    }

    @Test
    fun `forward is disabled until an answer is selected`() {
        setContent { Survey(state = questionState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun `forward goes to the next question once an answer is selected`() {
        setContent {
            Survey(
                state = questionState.copy(selectedAnswerIndex = 0),
                onHandleAction = { actions += it },
            )
        }

        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(SurveyAction.GoToNextQuestion), actions) }
    }

    @Test
    fun `back goes to the previous question`() {
        setContent { Survey(state = questionState, onHandleAction = { actions += it }) }

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle { assertEquals(listOf(SurveyAction.GoToPrevQuestion), actions) }
    }

    @Test
    fun `screen shows the first question of the survey`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNodeWithText(FIRST_QUESTION.prompt).assertIsDisplayed()
        composeTestRule.onNodeWithText(FIRST_QUESTION.choices.first()).assertIsDisplayed()
    }

    @Test
    fun `screen marks the survey as read`() {
        val messagesRepo = fakeMessagesRepo()
        setScreenContent(messagesRepo)

        composeTestRule.waitForIdle()

        coVerify { messagesRepo.markMessageAsRead(MESSAGE_ID) }
    }

    @Test
    fun `answering and going forward shows the next question`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNodeWithText(FIRST_QUESTION.choices.first()).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(SECOND_QUESTION.prompt).assertIsDisplayed()
        composeTestRule.onNodeWithText(FIRST_QUESTION.prompt).assertDoesNotExist()
    }

    @Test
    fun `going back from the second question shows the first question again`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNodeWithText(FIRST_QUESTION.choices.first()).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.onNodeWithText(FIRST_QUESTION.prompt).assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(SurveyRoute(MESSAGE_ID), backStack.last()) }
    }

    @Test
    fun `going back from the first question leaves the survey`() {
        setScreenContent(fakeMessagesRepo())

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, IndividualMessageListRoute(USER.id)), backStack.toList())
        }
    }

    @Test
    fun `answering the last question saves the answers and leaves the survey`() {
        val messagesRepo = fakeMessagesRepo()
        setScreenContent(messagesRepo)

        composeTestRule.onNodeWithText(FIRST_QUESTION.choices[1]).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()
        composeTestRule.onNodeWithText(SECOND_QUESTION.choices.first()).performClick()
        composeTestRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(DashboardRoute, IndividualMessageListRoute(USER.id)), backStack.toList())
        }
        coVerify {
            messagesRepo.saveSurveyAnswers(
                MESSAGE_ID,
                listOf(FIRST_QUESTION.choices[1], SECOND_QUESTION.choices.first()),
            )
        }
    }

    private fun fakeMessagesRepo(): MessagesRepo {
        val messagesRepo = mockk<MessagesRepo>(relaxed = true)
        coEvery { messagesRepo.getSurveyMessage(MESSAGE_ID) } returns SURVEY
        return messagesRepo
    }

    private fun setScreenContent(messagesRepo: MessagesRepo) {
        backStack.add(IndividualMessageListRoute(USER.id))
        backStack.add(SurveyRoute(MESSAGE_ID))
        val userRepo = mockk<UserRepo>()
        coEvery { userRepo.getUserWithWallet(USER.wallet) } returns USER
        val viewModel = SurveyViewModel(MESSAGE_ID, messagesRepo, userRepo)
        setContent(inspectionMode = false) { SurveyScreen(MESSAGE_ID, viewModel) }
    }

    private companion object {
        const val MESSAGE_ID = "survey-1"

        val FIRST_QUESTION = Question(prompt = "How many trees did you plant?", choices = listOf("One", "Two", "Three"))
        val SECOND_QUESTION = Question(prompt = "Did it rain today?", choices = listOf("Yes", "No"))

        val USER =
            User(
                id = 9,
                wallet = "planter@example.com",
                numberOfTrees = 3,
                firstName = "Ada",
                lastName = "Lovelace",
                photoPath = "/does/not/exist.jpg",
                isPowerUser = false,
                unreadMessagesAvailable = true,
            )

        val SURVEY =
            SurveyMessage(
                id = MESSAGE_ID,
                from = "greenstand",
                to = "planter@example.com",
                composedAt = "2026-01-01T00:00:00Z",
                isRead = false,
                surveyId = "survey",
                title = "Planting survey",
                questions = listOf(FIRST_QUESTION, SECOND_QUESTION),
                isComplete = false,
            )
    }
}