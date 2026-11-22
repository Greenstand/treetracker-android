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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.CaptureSetupNavigationController
import org.greenstand.android.TreeTracker.navigation.orgRepoWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class CaptureSetupScopeManagerTest {
    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)
    private val stepCounter = mockk<StepCounter>(relaxed = true)
    private val sessionTracker = mockk<SessionTracker>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val deviceOrientation = mockk<DeviceOrientation>(relaxed = true)
    private val userRepo = mockk<UserRepo>()
    private val user =
        User(
            id = 7L,
            wallet = "5551230001",
            numberOfTrees = 0,
            firstName = "Ada",
            lastName = "Admin",
            photoPath = "/photos/ada.jpg",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    @Before
    fun setUp() {
        CaptureSetupScopeManager.close()
        stopKoin()
        startKoin {
            modules(
                module {
                    single { exceptionDataCollector }
                    single { orgRepoWith() }
                    single { stepCounter }
                    single { sessionTracker }
                    single { locationDataCapturer }
                    single { deviceOrientation }
                    scope<CaptureSetupScope> {
                        scoped { CaptureSetupData(get()) }
                        scoped { CaptureSetupNavigationController(get(), get(), get(), get(), get()) }
                    }
                },
            )
        }
    }

    @After
    fun tearDown() {
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    @Test
    fun `is closed before open is called`() {
        assertFalse(CaptureSetupScopeManager.isOpen)
    }

    @Test
    fun `getData before open throws`() {
        val error = runCatching { CaptureSetupScopeManager.getData() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("CaptureSetupScope not open. Call open() before getData().", error?.message)
    }

    @Test
    fun `nav before open throws`() {
        val error = runCatching { CaptureSetupScopeManager.nav }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("CaptureSetupScope not open. Call open() before accessing nav.", error?.message)
    }

    @Test
    fun `open makes the scope available`() {
        CaptureSetupScopeManager.open()

        assertTrue(CaptureSetupScopeManager.isOpen)
    }

    @Test
    fun `getData returns the same instance while the scope is open`() {
        CaptureSetupScopeManager.open()

        assertSame(CaptureSetupScopeManager.getData(), CaptureSetupScopeManager.getData())
    }

    @Test
    fun `data written through getData is visible to later reads`() {
        CaptureSetupScopeManager.open()

        CaptureSetupScopeManager.getData().sessionNote = "planted near the river"

        assertEquals("planted near the river", CaptureSetupScopeManager.getData().sessionNote)
    }

    @Test
    fun `nav returns the same controller while the scope is open`() {
        CaptureSetupScopeManager.open()

        assertSame(CaptureSetupScopeManager.nav, CaptureSetupScopeManager.nav)
    }

    @Test
    fun `reopening starts a fresh scope with new data`() {
        CaptureSetupScopeManager.open()
        val first = CaptureSetupScopeManager.getData()
        first.sessionNote = "first session"

        CaptureSetupScopeManager.open()
        val second = CaptureSetupScopeManager.getData()

        assertTrue(CaptureSetupScopeManager.isOpen)
        assertNotSame(first, second)
        assertNull(second.sessionNote)
    }

    @Test
    fun `reopening starts a fresh navigation controller`() {
        CaptureSetupScopeManager.open()
        val first = CaptureSetupScopeManager.nav

        CaptureSetupScopeManager.open()

        assertNotSame(first, CaptureSetupScopeManager.nav)
    }

    @Test
    fun `close makes data unavailable`() {
        CaptureSetupScopeManager.open()

        CaptureSetupScopeManager.close()

        assertFalse(CaptureSetupScopeManager.isOpen)
        assertTrue(runCatching { CaptureSetupScopeManager.getData() }.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `close when not open is a no-op`() {
        CaptureSetupScopeManager.close()
        CaptureSetupScopeManager.close()

        assertFalse(CaptureSetupScopeManager.isOpen)
    }

    @Test
    fun `snapshot is null while the scope is closed`() {
        assertNull(CaptureSetupScopeManager.snapshot())
    }

    @Test
    fun `snapshot holds the user id and the other setup data`() {
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().apply {
            user = this@CaptureSetupScopeManagerTest.user
            destinationWallet = "wallet-2"
            sessionNote = "planted near the river"
            organizationName = "Acme Forestry"
        }

        assertEquals(
            CaptureSetupSnapshot(
                userId = 7L,
                destinationWallet = "wallet-2",
                sessionNote = "planted near the river",
                organizationName = "Acme Forestry",
            ),
            CaptureSetupScopeManager.snapshot(),
        )
    }

    @Test
    fun `snapshot of a scope with nothing set is empty`() {
        CaptureSetupScopeManager.open()

        assertEquals(CaptureSetupSnapshot(), CaptureSetupScopeManager.snapshot())
    }

    @Test
    fun `restore after process death reopens the scope with the saved data`() =
        runTest {
            coEvery { userRepo.getUser(7L) } returns user
            CaptureSetupScopeManager.open()
            CaptureSetupScopeManager.getData().apply {
                user = this@CaptureSetupScopeManagerTest.user
                destinationWallet = "wallet-2"
                sessionNote = "planted near the river"
                organizationName = "Acme Forestry"
            }
            val snapshot = CaptureSetupScopeManager.snapshot()!!
            // Process death: the scope is gone, only the snapshot survives.
            CaptureSetupScopeManager.close()

            CaptureSetupScopeManager.restore(snapshot, userRepo)

            assertTrue(CaptureSetupScopeManager.isOpen)
            val data = CaptureSetupScopeManager.getData()
            assertEquals(user, data.user)
            assertEquals("wallet-2", data.destinationWallet)
            assertEquals("planted near the river", data.sessionNote)
            assertEquals("Acme Forestry", data.organizationName)
        }

    @Test
    fun `restore leaves an open scope alone`() =
        runTest {
            CaptureSetupScopeManager.open()
            val data = CaptureSetupScopeManager.getData()
            data.organizationName = "Typed after the state was saved"

            CaptureSetupScopeManager.restore(CaptureSetupSnapshot(userId = 7L, organizationName = "Saved"), userRepo)

            assertSame(data, CaptureSetupScopeManager.getData())
            assertEquals("Typed after the state was saved", data.organizationName)
            assertNull(data.user)
            coVerify(exactly = 0) { userRepo.getUser(any()) }
        }

    @Test
    fun `restore opens the scope without a user when the user is gone`() =
        runTest {
            coEvery { userRepo.getUser(7L) } returns null

            CaptureSetupScopeManager.restore(CaptureSetupSnapshot(userId = 7L, sessionNote = "note"), userRepo)

            assertTrue(CaptureSetupScopeManager.isOpen)
            assertNull(CaptureSetupScopeManager.getData().user)
            assertEquals("note", CaptureSetupScopeManager.getData().sessionNote)
        }

    @Test
    fun `restore without a selected user does not read the database`() =
        runTest {
            CaptureSetupScopeManager.restore(CaptureSetupSnapshot(), userRepo)

            assertTrue(CaptureSetupScopeManager.isOpen)
            assertNull(CaptureSetupScopeManager.getData().user)
            coVerify(exactly = 0) { userRepo.getUser(any()) }
        }
}