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
package org.greenstand.android.TreeTracker.sessionnote

import kotlin.test.assertFalse
import kotlinx.coroutines.CompletableDeferred
import io.mockk.verifyOrder
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.coEvery
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupRestorer
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SessionNoteViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var captureSetupData: CaptureSetupData
    private val setupRestorer = mockk<CaptureSetupRestorer>(relaxed = true)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(CaptureSetupScopeManager)
        captureSetupData = mockk(relaxed = true)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `WHEN updateNote called THEN updates state note`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            every { captureSetupData.user } returns fakeUser

            val viewModel = SessionNoteViewModel(setupRestorer)

            viewModel.handleAction(SessionNoteAction.UpdateNote("This is a test note"))

            val state = viewModel.state.value
            assertEquals("This is a test note", state.note)
            verify { captureSetupData.sessionNote = "This is a test note" }
        }

    @Test
    fun `WHEN created THEN state holds the setup user's photo and an empty note`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first().copy(photoPath = "/selfies/caleb.jpg")
            every { captureSetupData.user } returns fakeUser

            val viewModel = SessionNoteViewModel(setupRestorer)

            assertEquals(SessionNoteState(note = "", userImagePath = "/selfies/caleb.jpg"), viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN note is cleared THEN state and setup data hold an empty note`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = SessionNoteViewModel(setupRestorer)

            viewModel.handleAction(SessionNoteAction.UpdateNote("draft"))
            viewModel.handleAction(SessionNoteAction.UpdateNote(""))

            assertEquals("", viewModel.state.value.note)
            verify { captureSetupData.sessionNote = "" }
        }

    @Test
    fun `WHEN NavigateBack or NavigateNext THEN view model leaves state and setup data unchanged`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = SessionNoteViewModel(setupRestorer)
            val initialState = viewModel.state.value

            viewModel.handleAction(SessionNoteAction.NavigateBack)
            viewModel.handleAction(SessionNoteAction.NavigateNext)

            assertEquals(initialState, viewModel.state.value)
            assertTrue(viewModel.events.replayCache.isEmpty())
            verify(exactly = 0) { captureSetupData.sessionNote = any() }
        }

    // --- Process death restore (#1447) ---

    @Test
    fun `WHEN created with the setup scope open THEN saves the setup data without reloading the user`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()

            val viewModel = SessionNoteViewModel(setupRestorer)

            verify { setupRestorer.save() }
            coVerify(exactly = 0) { setupRestorer.reloadUser() }
            assertFalse(viewModel.state.value.restoring)
            assertFalse(viewModel.state.value.setupLost)
        }

    @Test
    fun `WHEN created after process death THEN shows the reloaded user and the saved note`() =
        runTest {
            val fakeUser = FakeFileGenerator.fakeUsers.first()
            stubRestoredAfterProcessDeath()
            every { captureSetupData.user } returns fakeUser
            every { captureSetupData.sessionNote } returns "Planting day"

            val viewModel = SessionNoteViewModel(setupRestorer)

            val state = viewModel.state.value
            assertEquals(fakeUser.photoPath, state.userImagePath)
            assertEquals("Planting day", state.note)
            assertFalse(state.restoring)
            assertFalse(state.setupLost)
        }

    @Test
    fun `WHEN a restored session note is still reloading its user THEN it is restoring`() =
        runTest {
            val reload = CompletableDeferred<Boolean>()
            every { setupRestorer.reopenIfClosed() } returns true
            coEvery { setupRestorer.reloadUser() } coAnswers { reload.await() }
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()

            val viewModel = SessionNoteViewModel(setupRestorer)
            assertTrue(viewModel.state.value.restoring)

            reload.complete(true)
            assertFalse(viewModel.state.value.restoring)
        }

    @Test
    fun `WHEN created after process death and the user is gone THEN setup is lost and the note is ignored`() =
        runTest {
            stubRestoredAfterProcessDeath(userReloaded = false)
            every { captureSetupData.sessionNote } returns null
            val viewModel = SessionNoteViewModel(setupRestorer)
            assertTrue(viewModel.state.value.setupLost)
            assertFalse(viewModel.state.value.restoring)
            clearMocks(CaptureSetupScopeManager, answers = false)

            viewModel.handleAction(SessionNoteAction.UpdateNote("late"))

            verify(exactly = 0) { CaptureSetupScopeManager.getData() }
            verify(exactly = 0) { setupRestorer.save() }
            assertEquals("", viewModel.state.value.note)
        }

    @Test
    fun `WHEN the note changes THEN the setup data is saved again`() =
        runTest {
            every { captureSetupData.user } returns FakeFileGenerator.fakeUsers.first()
            val viewModel = SessionNoteViewModel(setupRestorer)

            viewModel.handleAction(SessionNoteAction.UpdateNote("note"))

            verifyOrder {
                captureSetupData.sessionNote = "note"
                setupRestorer.save()
            }
        }

    private fun stubRestoredAfterProcessDeath(userReloaded: Boolean = true) {
        every { setupRestorer.reopenIfClosed() } returns true
        coEvery { setupRestorer.reloadUser() } returns userReloaded
    }
}
