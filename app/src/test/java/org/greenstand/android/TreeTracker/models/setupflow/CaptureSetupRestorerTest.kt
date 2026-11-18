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
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaptureSetupRestorerTest {
    private val preferences = mockk<Preferences>(relaxed = true)
    private val editor = mockk<Preferences.Editor>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val setupData = mockk<CaptureSetupData>(relaxed = true)
    private val setupNav = mockk<CaptureSetupNavigationController>(relaxed = true)
    private val user = FakeFileGenerator.fakeUsers.first()
    private val restorer = CaptureSetupRestorer(preferences, userRepo)

    @Before
    fun setUp() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.open() } just runs
        every { CaptureSetupScopeManager.close() } just runs
        every { CaptureSetupScopeManager.getData() } returns setupData
        every { CaptureSetupScopeManager.nav } returns setupNav
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureSetupScopeManager)
    }

    @Test
    fun `an open scope is left alone`() {
        every { CaptureSetupScopeManager.isOpen } returns true

        assertFalse(restorer.reopenIfClosed(RouteRegistry.ROUTE_SESSION_NOTE))

        verify(exactly = 0) { CaptureSetupScopeManager.open() }
        verify(exactly = 0) { setupNav.restoreAt(any()) }
    }

    @Test
    fun `a closed scope is reopened at the restored step`() {
        every { CaptureSetupScopeManager.isOpen } returns false

        assertTrue(restorer.reopenIfClosed(RouteRegistry.ROUTE_SESSION_NOTE))

        verify { CaptureSetupScopeManager.open() }
        verify { setupNav.restoreAt(RouteRegistry.ROUTE_SESSION_NOTE) }
    }

    @Test
    fun `the user of an open setup is saved for a later restore`() =
        runTest {
            every { setupData.user } returns user

            assertEquals(user, restorer.loadUser(restored = false))

            verify { editor.putLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, user.id) }
            coVerify(exactly = 0) { userRepo.getUser(any()) }
        }

    @Test
    fun `a restored setup gets the saved user back`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, any()) } returns user.id
            coEvery { userRepo.getUser(user.id) } returns user

            assertEquals(user, restorer.loadUser(restored = true))

            verify { setupData.user = user }
            verify(exactly = 0) { CaptureSetupScopeManager.close() }
        }

    @Test
    fun `a restored setup whose user is gone is closed`() =
        runTest {
            every { preferences.getLong(CaptureSetupRestorer.SETUP_USER_ID_KEY, any()) } returns user.id
            coEvery { userRepo.getUser(any()) } returns null

            assertNull(restorer.loadUser(restored = true))

            verify { CaptureSetupScopeManager.close() }
            verify(exactly = 0) { editor.putLong(any(), any()) }
        }

    @Test
    fun `an open setup without a user is closed`() =
        runTest {
            every { setupData.user } returns null

            assertNull(restorer.loadUser(restored = false))

            verify { CaptureSetupScopeManager.close() }
        }
}