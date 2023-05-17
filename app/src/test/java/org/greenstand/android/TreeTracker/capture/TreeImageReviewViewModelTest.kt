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
package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TreeImageReviewViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = mockk<TreeCapturer>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)

    private lateinit var testSubject: TreeImageReviewViewModel

    @Before
    fun setUp() {
        testSubject = TreeImageReviewViewModel(treeCapturer = treeCapturer, userRepo = userRepo)
    }
    @Test
    fun `updateNote should update the note in the view model state`() = runTest {
        val note = "Note"
        testSubject.updateNote(note)
        val expected = testSubject.state.getOrAwaitValueTest().note
        assertEquals(expected, note)
    }
    @Test
    fun `updateReviewTutorialDialog should update showReviewTutorial flag state `() = runTest {
        val showTutorial = true
        testSubject.updateReviewTutorialDialog(showTutorial)
        val expected = testSubject.state.getOrAwaitValueTest().showReviewTutorial!!
        assertTrue(expected)
    }
    @Test
    fun `addNote should set the note in the tree capturer and close the dialog`() = runTest {
        val note = "Some note"
        testSubject.updateNote(note)
        testSubject.addNote()
        verify { treeCapturer.setNote(note) }
        val expected = testSubject.state.getOrAwaitValueTest().isDialogOpen
        assertFalse(expected)
    }
    @Test
    fun `setDialogState should update the dialog open state in the view model`() = runTest {
        val isDialogOpen = true
        testSubject.setDialogState(isDialogOpen)
        val expected = testSubject.state.getOrAwaitValueTest().isDialogOpen
        assertTrue(expected)
    }
}