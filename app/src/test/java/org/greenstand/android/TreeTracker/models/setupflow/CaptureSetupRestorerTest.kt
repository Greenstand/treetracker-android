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
package org.greenstand.android.TreeTracker.models.setupflow

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CaptureSetupRestorerTest {
    private val preferences = mockk<Preferences>(relaxed = true)
    private val editor = mockk<Preferences.Editor>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)

    /** A real one, so what the restorer writes into the scope can be read back. */
    private val data = CaptureSetupData(mockk(relaxed = true))
    private val user = FakeFileGenerator.fakeUsers.first()

    private val restorer = CaptureSetupRestorer(preferences, userRepo)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.isOpen } returns false
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { CaptureSetupScopeManager.getData() } returns data
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `WHEN the scope is open THEN reopenIfClosed leaves it alone`() {
        every { CaptureSetupScopeManager.isOpen } returns true
        data.sessionNote = "typed in this process"

        assertFalse(restorer.reopenIfClosed())

        verify(exactly = 0) { CaptureSetupScopeManager.open() }
        assertEquals("typed in this process", data.sessionNote)
    }

    @Test
    fun `WHEN the scope is closed THEN reopenIfClosed opens it with the saved note, wallet and org name`() {
        every { preferences.getString(CaptureSetupRestorer.SESSION_NOTE_KEY, any()) } returns "Planting day"
        every { preferences.getString(CaptureSetupRestorer.DESTINATION_WALLET_KEY, any()) } returns "tree-fund"
        every { preferences.getString(CaptureSetupRestorer.ORGANIZATION_NAME_KEY, any()) } returns "Acme"

        assertTrue(restorer.reopenIfClosed())

        verifyOrder {
            CaptureSetupScopeManager.open()
            CaptureSetupScopeManager.getData()
        }
        assertEquals("Planting day", data.sessionNote)
        assertEquals("tree-fund", data.destinationWallet)
        assertEquals("Acme", data.organizationName)
    }

    @Test
    fun `WHEN nothing was saved THEN reopenIfClosed leaves the note, wallet and org name unset`() {
        every { preferences.getString(any(), any()) } returns null

        assertTrue(restorer.reopenIfClosed())

        assertNull(data.sessionNote)
        assertNull(data.destinationWallet)
        assertNull(data.organizationName)
    }

    @Test
    fun `WHEN the saved user exists THEN reloadUser puts it in the scope and in the preferences`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.USER_ID_KEY, any()) } returns user.id
            coEvery { userRepo.getUser(user.id) } returns user

            assertTrue(restorer.reloadUser())

            assertEquals(user, data.user)
            verify { preferences.setUserId(user.id) }
            verify(exactly = 0) { CaptureSetupScopeManager.close() }
        }

    @Test
    fun `WHEN the saved user is gone THEN reloadUser closes the scope`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.USER_ID_KEY, any()) } returns 42L
            coEvery { userRepo.getUser(42L) } returns null

            assertFalse(restorer.reloadUser())

            verify { CaptureSetupScopeManager.close() }
            assertNull(data.user)
        }

    @Test
    fun `WHEN saved THEN the user id, note, wallet and org name are written`() {
        data.user = user
        data.sessionNote = "Planting day"
        data.destinationWallet = "tree-fund"
        data.organizationName = "Acme"

        restorer.save()

        verify { editor.putLong(CaptureSetupRestorer.USER_ID_KEY, user.id) }
        verify { editor.putString(CaptureSetupRestorer.SESSION_NOTE_KEY, "Planting day") }
        verify { editor.putString(CaptureSetupRestorer.DESTINATION_WALLET_KEY, "tree-fund") }
        verify { editor.putString(CaptureSetupRestorer.ORGANIZATION_NAME_KEY, "Acme") }
        verify { editor.apply() }
    }

    @Test
    fun `WHEN saved without a note, wallet or org name THEN the ones from an earlier setup are cleared`() {
        data.user = user

        restorer.save()

        verify { editor.putString(CaptureSetupRestorer.SESSION_NOTE_KEY, null) }
        verify { editor.putString(CaptureSetupRestorer.DESTINATION_WALLET_KEY, null) }
        verify { editor.putString(CaptureSetupRestorer.ORGANIZATION_NAME_KEY, null) }
    }
}
