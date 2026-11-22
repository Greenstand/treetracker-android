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
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * The capture setup survives process death (#1418): [CaptureSetupData] saves each change,
 * [CaptureSetupScopeManager] forgets the copy when a setup starts or ends, and
 * [CaptureSetupRestorer] reopens the scope from the copy.
 */
class CaptureSetupRestorerTest {
    private val store = mockk<CaptureSetupStore>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val preferences = mockk<Preferences>(relaxed = true)
    private val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    private val restorer = CaptureSetupRestorer(store, userRepo, preferences, locationDataCapturer)

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
                    single { mockk<ExceptionDataCollector>(relaxed = true) }
                    scope<CaptureSetupScope> {
                        scoped { CaptureSetupData(get(), store) }
                    }
                },
            )
        }
        every { store.load() } returns null
    }

    @After
    fun tearDown() {
        CaptureSetupScopeManager.close()
        stopKoin()
    }

    @Test
    fun `picking a user saves the setup, and later changes save again`() {
        CaptureSetupScopeManager.open()

        CaptureSetupScopeManager.getData().user = user
        CaptureSetupScopeManager.getData().organizationName = "Org"

        verify { store.save(SavedCaptureSetup(userId = 7L)) }
        verify { store.save(SavedCaptureSetup(userId = 7L, organizationName = "Org")) }
    }

    @Test
    fun `nothing is saved before a user is picked`() {
        CaptureSetupScopeManager.open()

        CaptureSetupScopeManager.getData().organizationName = "Org"

        verify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `open forgets the saved setup`() {
        CaptureSetupScopeManager.open()

        verify(exactly = 1) { store.clear() }
    }

    @Test
    fun `close forgets the saved setup`() {
        CaptureSetupScopeManager.restore {}

        CaptureSetupScopeManager.close()

        verify(exactly = 1) { store.clear() }
        assertFalse(CaptureSetupScopeManager.isOpen)
    }

    @Test
    fun `restore reopens the scope with the saved setup and keeps the saved copy`() =
        runTest {
            every { store.load() } returns
                SavedCaptureSetup(userId = 7L, destinationWallet = "wallet", sessionNote = "note", organizationName = "Org")
            coEvery { userRepo.getUser(7L) } returns user

            restorer.restoreIfNeeded()

            assertTrue(CaptureSetupScopeManager.isOpen)
            val data = CaptureSetupScopeManager.getData()
            assertEquals(user, data.user)
            assertEquals("wallet", data.destinationWallet)
            assertEquals("note", data.sessionNote)
            assertEquals("Org", data.organizationName)
            verify { preferences.setUserId(7L) }
            verify { locationDataCapturer.startGpsUpdates() }
            verify(exactly = 0) { store.clear() }
        }

    @Test
    fun `restore does nothing when no setup was saved`() =
        runTest {
            restorer.restoreIfNeeded()

            assertFalse(CaptureSetupScopeManager.isOpen)
            verify(exactly = 0) { locationDataCapturer.startGpsUpdates() }
        }

    @Test
    fun `restore drops a saved setup whose user is gone`() =
        runTest {
            every { store.load() } returns SavedCaptureSetup(userId = 7L)
            coEvery { userRepo.getUser(7L) } returns null

            restorer.restoreIfNeeded()

            assertFalse(CaptureSetupScopeManager.isOpen)
            verify(exactly = 1) { store.clear() }
        }

    @Test
    fun `restore leaves an open setup alone`() =
        runTest {
            CaptureSetupScopeManager.open()
            every { store.load() } returns SavedCaptureSetup(userId = 7L)
            coEvery { userRepo.getUser(7L) } returns user

            restorer.restoreIfNeeded()

            assertNull(CaptureSetupScopeManager.getData().user)
            verify(exactly = 0) { store.load() }
        }
}
