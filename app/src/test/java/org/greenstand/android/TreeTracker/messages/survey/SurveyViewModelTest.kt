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
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.messages.Question
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.Assert
import org.junit.Assert.assertEquals
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
    fun `verify message repo gets the correct survey Message`() = runBlocking {
        coVerify { messagesRepo.getSurveyMessage(messageId) }
    }
    @Test
    fun `verify user Repo gets the correct user with wallet`() = runBlocking {
        coVerify { userRepo.getUserWithWallet(FakeFileGenerator.fakeSurveyMessage.to) }
    }

    @Test
    fun `WHEN selected answer THEN selected answer index updates with correct value`() = runBlocking {
        testSubject.selectAnswer(1)
        testSubject.state.test {
            assertEquals(awaitItem().selectedAnswerIndex, 1)
        }
    }
    @Test
    fun `WHEN you go to next question ,THEN current question updates to next question`() = runBlocking {
        val result = testSubject.goToNextQuestion()
        Assert.assertTrue(result)
        testSubject.state.test {
            assertEquals(awaitItem().currentQuestion, FakeFileGenerator.fakeSurveyMessage.questions[1])
        }
    }

    @Test
    fun `WHEN you go to prev question ,THEN current question updates to previous question`() = runBlocking {
        testSubject.goToNextQuestion()
        testSubject.goToPrevQuestion()
        testSubject.state.test {
            assertEquals(awaitItem().currentQuestion, FakeFileGenerator.fakeSurveyMessage.questions[0])
        }
    }
    @Test
    fun `WHEN current question is already first, go to previous question returns false`() = runBlocking {
        val questions = listOf(Question(prompt = "random", choices = listOf("one", "two")))
        coEvery { messagesRepo.getSurveyMessage(any()) } returns FakeFileGenerator.fakeSurveyMessage.copy(questions = questions)
        val result = testSubject.goToPrevQuestion()
        Assert.assertFalse(result)
    }
}