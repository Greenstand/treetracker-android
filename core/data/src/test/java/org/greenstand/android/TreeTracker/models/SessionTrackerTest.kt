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
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
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

    private lateinit var editor: Preferences.Editor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        editor = mockk(relaxed = true)
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor

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

    // Starts a session with the user, org name and note given to setupStartSessionMocks.
    private var startSession: suspend () -> Unit = {}

    private fun setupStartSessionMocks(
        organizationName: String? = null,
        currentOrgName: String = "Kasiki Hai",
        sessionNote: String? = null,
    ) {
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        startSession = { sessionTracker.startSession(fakeUser.id, organizationName, sessionNote) }

        val org = Org(id = "123", name = currentOrgName, walletId = "wallet-123", logoPath = "", captureSetupFlow = emptyList(), captureFlow = emptyList())
        every { orgRepo.currentOrg() } returns org

        coEvery { dao.getUserById(fakeUser.id) } returns FakeFileGenerator.fakeUserEntity
        coEvery { dao.getLatestDeviceConfig() } returns FakeFileGenerator.fakeDeviceConfig
        coEvery { dao.insertSession(any()) } returns 1L
    }

    @Test
    fun `WHEN organizationName is null THEN session uses currentOrg name`() =
        runTest {
            setupStartSessionMocks(organizationName = null, currentOrgName = "Kasiki Hai")

            startSession()

            val slot = slot<SessionEntity>()
            coVerify { dao.insertSession(capture(slot)) }
            assertEquals("Kasiki Hai", slot.captured.organization)
        }

    @Test
    fun `WHEN organizationName is set THEN session uses that name`() =
        runTest {
            setupStartSessionMocks(organizationName = "User Typed Org", currentOrgName = "Kasiki Hai")

            startSession()

            val slot = slot<SessionEntity>()
            coVerify { dao.insertSession(capture(slot)) }
            assertEquals("User Typed Org", slot.captured.organization)
        }

    @Test
    fun `WHEN startSession THEN the capture setup note is saved and the session becomes current`() =
        runTest {
            setupStartSessionMocks(sessionNote = "a note")

            startSession()

            val slot = slot<SessionEntity>()
            coVerify { dao.insertSession(capture(slot)) }
            assertEquals("a note", slot.captured.note)
            assertEquals(1L, sessionTracker.currentSessionId)
        }

    @Test
    fun `WHEN createSession THEN it is saved without becoming the current session`() =
        runTest {
            setupStartSessionMocks(currentOrgName = "Kasiki Hai")
            coEvery { dao.insertSession(any()) } returns 9L

            val session =
                sessionTracker.createSession(
                    userId = FakeFileGenerator.fakeUsers.first().id,
                    organizationName = null,
                    sessionNote = "a note",
                )

            assertEquals(9L, session.id)
            assertEquals(FakeFileGenerator.fakeUserEntity.wallet, session.originWallet)
            assertEquals("Kasiki Hai", session.organization)
            assertEquals("a note", session.note)
            assertEquals(null, sessionTracker.currentSessionId)
            coVerify(exactly = 0) { preferences.edit() }
        }

    @Test
    fun `WHEN closeSession THEN only that session gets an end time and the current one is kept`() =
        runTest {
            setupStartSessionMocks()
            startSession()
            val detached = savedSession()
            coEvery { dao.getSessionById(9L) } returns detached
            every { timeProvider.currentTime() } returns NOW

            sessionTracker.closeSession(9L)

            assertEquals(NOW, detached.endTime)
            coVerify { dao.updateSession(detached) }
            coVerify(exactly = 0) { dao.getSessionById(1L) }
            assertEquals(1L, sessionTracker.currentSessionId)
        }

    @Test
    fun `WHEN endSession after startSession THEN the session is ended and nothing is current`() =
        runTest {
            setupStartSessionMocks()
            startSession()
            val started = savedSession()
            coEvery { dao.getSessionById(1L) } returns started
            every { timeProvider.currentTime() } returns NOW

            sessionTracker.endSession()

            assertEquals(NOW, started.endTime)
            coVerify { dao.updateSession(started) }
            assertEquals(null, sessionTracker.currentSessionId)
            verify { editor.putLong(SessionTracker.SESSION_ID_KEY, 1L) }
            verify { editor.putLong(SessionTracker.SESSION_ID_KEY, -1L) }
            coVerify { treesToSyncHelper.refreshTreeCountToSync() }
            verify { exceptionDataCollector.set(ExceptionDataCollector.IS_IN_SESSION, false) }
        }

    private fun savedSession() =
        SessionEntity(
            uuid = "session-uuid",
            originUserId = "user-uuid",
            originWallet = "wallet",
            destinationWallet = "wallet",
            startTime = Instant.fromEpochMilliseconds(0),
            organization = "Greenstand",
            isUploaded = false,
        )

    companion object {
        private val NOW = Instant.fromEpochMilliseconds(1_700_000_000_000)
    }
}