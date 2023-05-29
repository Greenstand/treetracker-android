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
package org.greenstand.android.TreeTracker.messages.individualmessagelist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class IndividualMessageListViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()
    private val userId = 2L
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    private lateinit var testSubject: IndividualMessageListViewModel

    @Before
    fun setup() {
        coEvery { userRepo.getUser(any()) } returns FakeFileGenerator.fakeUsers.first()
        coEvery { messagesRepo.getMessageFlow(any()) } returns flowOf(FakeFileGenerator.messages)
        testSubject = IndividualMessageListViewModel(userId, userRepo, messagesRepo)
    }

    @Test
    fun `verify user repo gets the correct user`() = runBlocking {
        coVerify { userRepo.getUser(userId) }
    }

    @Test
    fun `WHEN message flow is triggered THEN state updates with correct message and user`() = runBlocking {
        val message = testSubject.state.getOrAwaitValueTest().messages.first()
        val currentUser = testSubject.state.getOrAwaitValueTest().currentUser
        assertEquals(message, FakeFileGenerator.messages.first())
        assertEquals(currentUser, FakeFileGenerator.fakeUsers.first())
    }
    @Test
    fun `WHEN selected message is triggered THEN state updates with correct message`() = runBlocking {
        testSubject.selectMessage(FakeFileGenerator.fakeSurveyMessage)
        val result = testSubject.state.getOrAwaitValueTest().selectedMessage
        assertEquals(result, FakeFileGenerator.messages[2])
    }

    @Test
    fun `WHEN selected message is null, Assert Null, THEN when we select message, returns correct data`() = runBlocking {
        val message = FakeFileGenerator.fakeDirectMessage
        assertNull(testSubject.state.getOrAwaitValueTest().selectedMessage)
        testSubject.selectMessage(message)
        val result = testSubject.state.getOrAwaitValueTest().selectedMessage
        assertEquals(result, FakeFileGenerator.messages[1])
    }
}