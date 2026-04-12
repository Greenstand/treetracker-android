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
package org.greenstand.android.TreeTracker.models

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SessionTrackerTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @MockK(relaxed = true)
    private lateinit var preferences: Preferences

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo

    @MockK(relaxed = true)
    private lateinit var exceptionDataCollector: ExceptionDataCollector

    private lateinit var sessionTracker: SessionTracker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        val mockEditor = mockk<Preferences.Editor>(relaxed = true)
        every { preferences.edit() } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor

        sessionTracker =
            SessionTracker(
                dao = dao,
                treesToSyncHelper = treesToSyncHelper,
                preferences = preferences,
                timeProvider = timeProvider,
                orgRepo = orgRepo,
                exceptionDataCollector = exceptionDataCollector,
            )
    }

    @Test
    fun `WHEN no active session THEN endSession no-ops`() =
        runTest {
            sessionTracker.endSession()

            coVerify(exactly = 0) { dao.getSessionById(any()) }
            coVerify(exactly = 0) { dao.updateSession(any()) }
        }

    @Test
    fun `WHEN no currentSessionId but prefs has valid value THEN wasSessionInterrupted returns true`() =
        runTest {
            every { preferences.getLong(any(), any()) } returns 5L

            val result = sessionTracker.wasSessionInterrupted()

            assertTrue(result)
        }

    @Test
    fun `WHEN prefs returns negative one THEN wasSessionInterrupted returns false`() =
        runTest {
            every { preferences.getLong(any(), any()) } returns -1L

            val result = sessionTracker.wasSessionInterrupted()

            assertFalse(result)
        }

    // --- Org name attribution tests ---

    private fun setupStartSessionMocks(
        organizationName: String? = null,
        currentOrgName: String = "Kasiki Hai",
    ) {
        val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        every { captureSetupData.user } returns fakeUser
        every { captureSetupData.organizationName } returns organizationName
        every { captureSetupData.sessionNote } returns null

        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.getData() } returns captureSetupData

        val org = Org(id = "123", name = currentOrgName, walletId = "wallet-123", logoPath = "", captureSetupFlow = emptyList(), captureFlow = emptyList())
        every { orgRepo.currentOrg() } returns org

        coEvery { dao.getUserById(fakeUser.id) } returns FakeFileGenerator.fakeUserEntity
        coEvery { dao.getLatestDeviceConfig() } returns FakeFileGenerator.fakeDeviceConfig
        coEvery { dao.insertSession(any()) } returns 1L
    }

    @After
    fun tearDown() {
        try {
            unmockkObject(CaptureSetupScopeManager)
        } catch (_: Exception) {
        }
    }

    @Test
    fun `WHEN organizationName is null THEN session uses currentOrg name`() =
        runTest {
            setupStartSessionMocks(organizationName = null, currentOrgName = "Kasiki Hai")

            sessionTracker.startSession()

            val slot = slot<SessionEntity>()
            coVerify { dao.insertSession(capture(slot)) }
            assertEquals("Kasiki Hai", slot.captured.organization)
        }

    @Test
    fun `WHEN organizationName is set THEN session uses that name`() =
        runTest {
            setupStartSessionMocks(organizationName = "User Typed Org", currentOrgName = "Kasiki Hai")

            sessionTracker.startSession()

            val slot = slot<SessionEntity>()
            coVerify { dao.insertSession(capture(slot)) }
            assertEquals("User Typed Org", slot.captured.organization)
        }
}