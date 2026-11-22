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
package org.greenstand.android.TreeTracker.models.setupflow

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [CaptureSetupRestorer]: a setup step restored after process death gets its scope and user back (#1447). */
@ExperimentalCoroutinesApi
class CaptureSetupRestorerTest {
    private val preferences = mockk<Preferences>(relaxed = true)
    private val editor = mockk<Preferences.Editor>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
    private val fakeUser = FakeFileGenerator.fakeUsers.first()
    private val restorer = CaptureSetupRestorer(preferences, userRepo)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.isOpen } returns true
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `GIVEN an open scope WHEN asked to reopen THEN the scope is left alone`() {
        assertFalse(restorer.reopenScopeIfClosed())

        verify(exactly = 0) { CaptureSetupScopeManager.open() }
    }

    @Test
    fun `GIVEN a closed scope WHEN asked to reopen THEN the scope is opened`() {
        every { CaptureSetupScopeManager.isOpen } returns false

        assertTrue(restorer.reopenScopeIfClosed())

        verify { CaptureSetupScopeManager.open() }
    }

    @Test
    fun `GIVEN an open scope WHEN the setup user is read THEN its id is saved for a restore`() =
        runTest {
            every { captureSetupData.user } returns fakeUser

            assertEquals(fakeUser, restorer.setupUser(restoring = false))

            verify { editor.putLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, fakeUser.id) }
            verify { editor.apply() }
            coVerify(exactly = 0) { userRepo.getUser(any()) }
        }

    @Test
    fun `GIVEN a restore WHEN the setup user is read THEN the saved user is reloaded into the scope`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, any()) } returns fakeUser.id
            coEvery { userRepo.getUser(fakeUser.id) } returns fakeUser

            assertEquals(fakeUser, restorer.setupUser(restoring = true))

            verify { captureSetupData.user = fakeUser }
            verify(exactly = 0) { CaptureSetupScopeManager.close() }
        }

    @Test
    fun `GIVEN a restore WHEN the saved user is gone THEN there is no user and the scope closes`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, any()) } returns -1L
            coEvery { userRepo.getUser(any()) } returns null

            assertNull(restorer.setupUser(restoring = true))

            verify { CaptureSetupScopeManager.close() }
            verify(exactly = 0) { editor.putLong(any(), any()) }
        }

    @Test
    fun `GIVEN an open scope without a user WHEN the setup user is read THEN the scope stays open`() =
        runTest {
            every { captureSetupData.user } returns null

            assertNull(restorer.setupUser(restoring = false))

            verify(exactly = 0) { CaptureSetupScopeManager.close() }
        }
}
