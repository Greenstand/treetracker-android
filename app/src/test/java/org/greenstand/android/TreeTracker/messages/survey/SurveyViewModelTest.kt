/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.messages.survey

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material.SnackbarDuration
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.navigation.IndividualMessageListRoute
import org.greenstand.android.TreeTracker.navigation.MessagesUserSelectRoute
import org.greenstand.android.TreeTracker.navigation.SurveyRoute
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.greenstand.android.TreeTracker.viewmodel.ShowSnackbar
import org.greenstand.android.TreeTracker.viewmodel.TextRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SurveyViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val messageId = "message"
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private lateinit var testSubject: SurveyViewModel

    @Before
    fun setup() {
        coEvery { messagesRepo.getSurveyMessage(any()) } returns FakeFileGenerator.fakeSurveyMessage
        coEvery { userRepo.getUserWithWallet(any()) } returns FakeFileGenerator.fakeUsers.first()
        testSubject = SurveyViewModel(messageId, messagesRepo, userRepo)
    }

    @Test
    fun `verify message repo gets the correct survey Message`() =
        runTest {
            coVerify { messagesRepo.getSurveyMessage(messageId) }
        }

    @Test
    fun `verify user Repo gets the correct user with wallet`() =
        runTest {
            coVerify { userRepo.getUserWithWallet(FakeFileGenerator.fakeSurveyMessage.to) }
        }

    @Test
    fun `WHEN selected answer THEN selected answer index updates with correct value`() =
        runTest {
            testSubject.handleAction(SurveyAction.SelectAnswer(1))
            testSubject.state.test {
                assertEquals(awaitItem().selectedAnswerIndex, 1)
            }
        }

    @Test
    fun `WHEN you go to next question ,THEN current question updates to next question`() =
        runTest {
            testSubject.handleAction(SurveyAction.GoToNextQuestion)
            testSubject.state.test {
                assertEquals(awaitItem().currentQuestion, FakeFileGenerator.fakeSurveyMessage.questions[1])
            }
        }

    @Test
    fun `WHEN you go to prev question ,THEN current question updates to previous question`() =
        runTest {
            testSubject.handleAction(SurveyAction.GoToNextQuestion)
            testSubject.handleAction(SurveyAction.GoToPrevQuestion)
            testSubject.state.test {
                assertEquals(awaitItem().currentQuestion, FakeFileGenerator.fakeSurveyMessage.questions[0])
            }
        }

    @Test
    fun `WHEN current question is already first, go to previous question emits pop-back event`() =
        runTest {
            val questions = listOf(Question(prompt = "random", choices = listOf("one", "two")))
            coEvery { messagesRepo.getSurveyMessage(any()) } returns FakeFileGenerator.fakeSurveyMessage.copy(questions = questions)
            testSubject = SurveyViewModel(messageId, messagesRepo, userRepo)
            testSubject.handleAction(SurveyAction.GoToPrevQuestion)

            val event = testSubject.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
        }

    @Test
    fun `WHEN last question answered THEN saves answers, emits survey-completed snackbar and pops back`() =
        runTest {
            val questions = listOf(Question(prompt = "only one", choices = listOf("a", "b")))
            coEvery { messagesRepo.getSurveyMessage(any()) } returns FakeFileGenerator.fakeSurveyMessage.copy(questions = questions)
            testSubject = SurveyViewModel(messageId, messagesRepo, userRepo)
            testSubject.handleAction(SurveyAction.SelectAnswer(0))
            testSubject.handleAction(SurveyAction.GoToNextQuestion)

            val emitted =
                testSubject.events
                    .take(2)
                    .toList()
                    .mapNotNull { it.getContentIfNotConsumed() }
            val snackbar = emitted.filterIsInstance<ShowSnackbar>().single()
            assertEquals(TextRef.Res(R.string.survey_completed), snackbar.message)
            assertTrue(emitted.any { it is PopBackStackEvent })
            coVerify { messagesRepo.saveSurveyAnswers(messageId, listOf("a")) }
        }

    private val threeQuestions =
        listOf(
            Question(prompt = "Q1", choices = listOf("a", "b", "c")),
            Question(prompt = "Q2", choices = listOf("yes", "no")),
            Question(prompt = "Q3", choices = listOf("red", "green")),
        )

    private fun createSurveyViewModel(questions: List<Question> = threeQuestions): SurveyViewModel {
        coEvery { messagesRepo.getSurveyMessage("survey-1") } returns
            FakeFileGenerator.fakeSurveyMessage.copy(id = "survey-1", to = "planter-wallet", questions = questions)
        coEvery { userRepo.getUserWithWallet("planter-wallet") } returns
            FakeFileGenerator.fakeUsers.first().copy(photoPath = "/photos/planter.jpg")
        return SurveyViewModel("survey-1", messagesRepo, userRepo)
    }

    private fun SurveyViewModel.emittedEvents() = events.replayCache.mapNotNull { it.getContentIfNotConsumed() }

    @Test
    fun `WHEN created THEN shows the planter photo and the first question without an answer`() =
        runTest {
            val viewModel = createSurveyViewModel()

            assertEquals(
                SurveyScreenState(
                    userImagePath = "/photos/planter.jpg",
                    currentQuestion = threeQuestions[0],
                    selectedAnswerIndex = null,
                ),
                viewModel.state.value,
            )
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN created THEN the survey is loaded before being marked as read`() =
        runTest {
            createSurveyViewModel()

            coVerifyOrder {
                messagesRepo.getSurveyMessage("survey-1")
                userRepo.getUserWithWallet("planter-wallet")
                messagesRepo.markMessageAsRead("survey-1")
            }
        }

    @Test
    fun `WHEN a different answer is selected THEN it replaces the previous one`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(0))

            viewModel.handleAction(SurveyAction.SelectAnswer(2))

            assertEquals(2, viewModel.state.value.selectedAnswerIndex)
            assertEquals(threeQuestions[0], viewModel.state.value.currentQuestion)
        }

    @Test
    fun `WHEN moving to an unanswered question THEN no answer is selected`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(1))

            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            assertEquals(threeQuestions[1], viewModel.state.value.currentQuestion)
            assertNull(viewModel.state.value.selectedAnswerIndex)
        }

    @Test
    fun `WHEN going back THEN the earlier answer is restored`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(1))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)
            viewModel.handleAction(SurveyAction.SelectAnswer(0))

            viewModel.handleAction(SurveyAction.GoToPrevQuestion)

            assertEquals(threeQuestions[0], viewModel.state.value.currentQuestion)
            assertEquals(1, viewModel.state.value.selectedAnswerIndex)
        }

    @Test
    fun `WHEN going forward again THEN the later answer is restored`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(1))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)
            viewModel.handleAction(SurveyAction.SelectAnswer(0))
            viewModel.handleAction(SurveyAction.GoToPrevQuestion)

            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            assertEquals(threeQuestions[1], viewModel.state.value.currentQuestion)
            assertEquals(0, viewModel.state.value.selectedAnswerIndex)
        }

    @Test
    fun `WHEN moving between questions before the last THEN nothing is saved and no events are emitted`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(0))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)
            viewModel.handleAction(SurveyAction.SelectAnswer(0))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            assertEquals(threeQuestions[2], viewModel.state.value.currentQuestion)
            coVerify(exactly = 0) { messagesRepo.saveSurveyAnswers(any(), any()) }
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN all questions are answered and finished THEN saves the chosen answers in question order`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(1))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)
            viewModel.handleAction(SurveyAction.SelectAnswer(1))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)
            viewModel.handleAction(SurveyAction.SelectAnswer(0))

            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            coVerify(exactly = 1) { messagesRepo.saveSurveyAnswers("survey-1", listOf("b", "no", "red")) }
        }

    @Test
    fun `WHEN the survey is finished THEN a short snackbar is shown before popping back`() =
        runTest {
            val viewModel = createSurveyViewModel(listOf(Question(prompt = "Only", choices = listOf("x", "y"))))
            viewModel.handleAction(SurveyAction.SelectAnswer(1))

            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            val events = viewModel.emittedEvents()
            assertEquals(
                listOf(ShowSnackbar(TextRef.Res(R.string.survey_completed)), PopBackStackEvent),
                events,
            )
            val snackbar = events.first() as ShowSnackbar
            assertEquals(SnackbarDuration.Short, snackbar.duration)
            assertNull(snackbar.actionLabel)
            assertNull(snackbar.onAction)
        }

    @Test
    fun `WHEN the survey is finished THEN the current question stays on the last one`() =
        runTest {
            val viewModel = createSurveyViewModel(listOf(Question(prompt = "Only", choices = listOf("x", "y"))))
            viewModel.handleAction(SurveyAction.SelectAnswer(0))

            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            assertEquals(
                "Only",
                viewModel.state.value.currentQuestion
                    ?.prompt,
            )
            assertEquals(0, viewModel.state.value.selectedAnswerIndex)
        }

    @Test
    fun `WHEN the finish pop is dispatched THEN navigator returns to the message list`() =
        runTest {
            val navigator =
                fakeNavigator(
                    MessagesUserSelectRoute,
                    IndividualMessageListRoute(planterInfoId = 1L),
                    SurveyRoute(messageId = "survey-1"),
                )
            val viewModel = createSurveyViewModel(listOf(Question(prompt = "Only", choices = listOf("x", "y"))))
            viewModel.handleAction(SurveyAction.SelectAnswer(0))
            viewModel.handleAction(SurveyAction.GoToNextQuestion)

            assertTrue(viewModel.emittedEvents().contains(PopBackStackEvent))
            navigator.throttledPopBackStack()

            assertEquals(listOf(MessagesUserSelectRoute, IndividualMessageListRoute(planterInfoId = 1L)), navigator.backStack)
        }

    @Test
    fun `GIVEN the first question WHEN going back THEN pops back and keeps the state`() =
        runTest {
            val viewModel = createSurveyViewModel()
            viewModel.handleAction(SurveyAction.SelectAnswer(2))
            val before = viewModel.state.value

            viewModel.handleAction(SurveyAction.GoToPrevQuestion)

            assertEquals(listOf(PopBackStackEvent), viewModel.emittedEvents())
            assertEquals(before, viewModel.state.value)
            coVerify(exactly = 0) { messagesRepo.saveSurveyAnswers(any(), any()) }
        }

    @Test
    fun `WHEN NavigateBack action THEN state is unchanged and no events are emitted`() =
        runTest {
            val viewModel = createSurveyViewModel()
            val before = viewModel.state.value

            viewModel.handleAction(SurveyAction.NavigateBack)

            assertEquals(before, viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }
}