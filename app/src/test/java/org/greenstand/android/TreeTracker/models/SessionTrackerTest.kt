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
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.DeviceConfigEntity
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
import kotlin.test.assertFailsWith
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

    private fun setupStartSessionMocks(
        organizationName: String? = null,
        currentOrgName: String = "Kasiki Hai",
        sessionNote: String? = null,
    ) {
        val captureSetupData = mockk<CaptureSetupData>(relaxed = true)
        val fakeUser = FakeFileGenerator.fakeUsers.first()
        every { captureSetupData.user } returns fakeUser
        every { captureSetupData.organizationName } returns organizationName
        every { captureSetupData.sessionNote } returns sessionNote

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

    @Test
    fun `WHEN startSession THEN the capture setup note is saved and the session becomes current`() =
        runTest {
            setupStartSessionMocks(sessionNote = "a note")

            sessionTracker.startSession()

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
            sessionTracker.startSession()
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
            sessionTracker.startSession()
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

    @Test
    fun `WHEN startSession THEN the session details are recorded for crash reports`() =
        runTest {
            setupStartSessionMocks(organizationName = "Org A", sessionNote = "a note")

            sessionTracker.startSession()

            verify { exceptionDataCollector.set(ExceptionDataCollector.ORG_NAME, "Org A") }
            verify {
                exceptionDataCollector.set(
                    ExceptionDataCollector.USER_WALLET,
                    FakeFileGenerator.fakeUserEntity.wallet,
                )
            }
            verify { exceptionDataCollector.set(ExceptionDataCollector.SESSION_NOTE, "a note") }
            verify { exceptionDataCollector.set(ExceptionDataCollector.IS_IN_SESSION, true) }
        }

    @Test
    fun `WHEN startSession THEN the session id is persisted`() =
        runTest {
            setupStartSessionMocks()

            sessionTracker.startSession()

            verify { editor.putLong(SessionTracker.SESSION_ID_KEY, 1L) }
            verify { editor.commit() }
        }

    @Test
    fun `WHEN startSession while a session is active THEN the previous session is ended first`() =
        runTest {
            setupStartSessionMocks()
            sessionTracker.startSession()
            val previous = savedSession()
            coEvery { dao.getSessionById(1L) } returns previous
            every { timeProvider.currentTime() } returns NOW
            coEvery { dao.insertSession(any()) } returns 2L

            sessionTracker.startSession()

            assertEquals(NOW, previous.endTime)
            coVerify { dao.updateSession(previous) }
            assertEquals(2L, sessionTracker.currentSessionId)
        }

    @Test
    fun `WHEN createSession THEN it records the user, start time and latest device config`() =
        runTest {
            setupStartSessionMocks()
            val deviceConfig = deviceConfig(id = 7L)
            coEvery { dao.getLatestDeviceConfig() } returns deviceConfig
            every { timeProvider.currentTime() } returns NOW

            val session =
                sessionTracker.createSession(
                    userId = FakeFileGenerator.fakeUsers.first().id,
                    organizationName = "Org B",
                    sessionNote = null,
                )

            assertEquals(FakeFileGenerator.fakeUserEntity.uuid, session.originUserId)
            assertEquals(NOW, session.startTime)
            assertEquals(7L, session.deviceConfigId)
            assertEquals("Org B", session.organization)
            assertEquals(null, session.note)
            assertFalse(session.isUploaded)
            assertEquals(null, session.endTime)
        }

    @Test
    fun `WHEN createSession twice THEN each session gets a unique uuid`() =
        runTest {
            setupStartSessionMocks()
            val userId = FakeFileGenerator.fakeUsers.first().id

            val first = sessionTracker.createSession(userId, null, null)
            val second = sessionTracker.createSession(userId, null, null)

            assertTrue(first.uuid != second.uuid)
        }

    @Test
    fun `WHEN createSession for an unknown user THEN it throws and saves nothing`() =
        runTest {
            coEvery { dao.getUserById(any()) } returns null

            assertFailsWith<IllegalStateException> {
                sessionTracker.createSession(userId = 99L, organizationName = null, sessionNote = null)
            }
            coVerify(exactly = 0) { dao.insertSession(any()) }
        }

    @Test
    fun `WHEN endSession without an active session THEN crash report session data is still cleared`() =
        runTest {
            sessionTracker.endSession()

            verify { exceptionDataCollector.clear(ExceptionDataCollector.ORG_NAME) }
            verify { exceptionDataCollector.clear(ExceptionDataCollector.DESTINATION_WALLET) }
            verify { exceptionDataCollector.clear(ExceptionDataCollector.SESSION_NOTE) }
            verify { exceptionDataCollector.set(ExceptionDataCollector.IS_IN_SESSION, false) }
            coVerify(exactly = 0) { treesToSyncHelper.refreshTreeCountToSync() }
            verify(exactly = 0) { preferences.edit() }
        }

    @Test
    fun `WHEN a session is active THEN wasSessionInterrupted returns false`() =
        runTest {
            setupStartSessionMocks()
            sessionTracker.startSession()
            every { preferences.getLong(any(), any()) } returns 1L

            assertFalse(sessionTracker.wasSessionInterrupted())
        }

    private fun deviceConfig(id: Long) =
        DeviceConfigEntity(
            uuid = "device-config-uuid",
            appVersion = "2.2.0",
            appBuild = 1,
            osVersion = "14",
            sdkVersion = 34,
            loggedAt = Instant.fromEpochMilliseconds(0),
        ).apply { this.id = id }

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