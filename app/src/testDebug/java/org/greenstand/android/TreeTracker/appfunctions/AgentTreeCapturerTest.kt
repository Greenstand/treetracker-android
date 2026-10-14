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
package org.greenstand.android.TreeTracker.appfunctions

import android.content.Context
import android.location.Location
import android.location.LocationManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AgentTreeCapturerTest {
    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    @MockK(relaxed = true)
    private lateinit var deviceConfigUpdater: DeviceConfigUpdater

    @MockK(relaxed = true)
    private lateinit var orgRepo: OrgRepo

    @MockK(relaxed = true)
    private lateinit var createTreeUseCase: CreateTreeUseCase

    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager

    @MockK(relaxed = true)
    private lateinit var locationManager: LocationManager

    private lateinit var capturer: AgentTreeCapturer

    private val powerUser = FakeFileGenerator.fakeUsers.first()
    private val savedTrees = mutableListOf<Tree>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(ImageUtils)
        every { ImageUtils.createTestImageFile(any(), any()) } returns File("/test/tree.jpg")
        coEvery { userRepo.getPowerUser() } returns powerUser
        every { sessionTracker.currentSessionId } returns null
        coEvery { sessionTracker.createSession(any(), any(), any()) } returns
            sessionFor(powerUser.wallet).apply { id = NEW_SESSION_ID }
        coEvery { createTreeUseCase.execute(capture(savedTrees)) } returns 1L

        // Without a passed location, trees go around the app's current GPS fix
        val currentLocation = mockk<Location>()
        every { currentLocation.latitude } returns 1.0
        every { currentLocation.longitude } returns 2.0
        every { locationUpdateManager.currentLocation } returns currentLocation

        capturer =
            AgentTreeCapturer(
                context,
                dao,
                userRepo,
                sessionTracker,
                deviceConfigUpdater,
                orgRepo,
                createTreeUseCase,
                treesToSyncHelper,
                locationUpdateManager,
                locationManager,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(ImageUtils)
    }

    @Test
    fun `WHEN no session is running THEN trees go in a new session that is closed and never made current`() =
        runTest {
            val result = capturer.captureTrees(count = 3, wallet = null, latitude = null, longitude = null, note = "hi")

            coVerify { sessionTracker.createSession(userId = powerUser.id, organizationName = null, sessionNote = null) }
            coVerify { sessionTracker.closeSession(NEW_SESSION_ID) }
            coVerify { treesToSyncHelper.refreshTreeCountToSync() }
            coVerify(exactly = 0) { sessionTracker.startSession() }
            coVerify(exactly = 0) { sessionTracker.endSession() }
            assertEquals(NEW_SESSION_ID, result.sessionId)
            assertEquals(powerUser.wallet, result.wallet)
            assertEquals(3, result.treeCount)
            assertEquals(savedTrees.map { it.treeUuid.toString() }, result.treeUuids)
            savedTrees.forEach { tree ->
                assertEquals(NEW_SESSION_ID, tree.sessionId)
                assertEquals("/test/tree.jpg", tree.photoPath)
                assertEquals("hi", tree.content)
                assertTrue(abs(tree.meanLatitude - 1.0) < 0.01 && abs(tree.meanLongitude - 2.0) < 0.01)
            }
        }

    @Test
    fun `WHEN the app was never opened THEN device config and org are set up before the session`() =
        runTest {
            capturer.captureTrees(1, wallet = null, latitude = null, longitude = null, note = null)

            coVerifyOrder {
                deviceConfigUpdater.saveLatestConfig()
                orgRepo.ensureInitialized()
                sessionTracker.createSession(any(), any(), any())
            }
        }

    @Test
    fun `WHEN a wallet is passed THEN its account captures the trees, ignoring email case`() =
        runTest {
            val other = FakeFileGenerator.fakeUsers[1]
            coEvery { userRepo.getUserWithWallet(any()) } returns null
            coEvery { userRepo.getUserWithWallet("ada@example.com") } returns other

            val result = capturer.captureTrees(1, wallet = "Ada@Example.com", latitude = null, longitude = null, note = null)

            coVerify { sessionTracker.createSession(userId = other.id, organizationName = null, sessionNote = null) }
            assertEquals(other.wallet, result.wallet)
        }

    @Test
    fun `WHEN a location is passed THEN trees are placed around it`() =
        runTest {
            capturer.captureTrees(5, wallet = null, latitude = -3.5, longitude = 37.3, note = null)

            assertEquals(5, savedTrees.size)
            savedTrees.forEach { tree ->
                assertTrue(abs(tree.meanLatitude + 3.5) < 0.01 && abs(tree.meanLongitude - 37.3) < 0.01)
            }
        }

    @Test
    fun `WHEN the location is at a pole or the antimeridian THEN trees stay in range`() =
        runTest {
            capturer.captureTrees(50, wallet = null, latitude = 90.0, longitude = 180.0, note = null)
            capturer.captureTrees(50, wallet = null, latitude = -89.9999, longitude = -180.0, note = null)

            assertEquals(100, savedTrees.size)
            savedTrees.forEach { tree ->
                assertTrue(tree.meanLatitude in -90.0..90.0, "latitude ${tree.meanLatitude}")
                assertTrue(tree.meanLongitude in -180.0..180.0, "longitude ${tree.meanLongitude}")
            }
        }

    @Test
    fun `WHEN a session is running THEN trees are added to it and it stays open`() =
        runTest {
            every { sessionTracker.currentSessionId } returns ACTIVE_SESSION_ID
            coEvery { dao.getSessionById(ACTIVE_SESSION_ID) } returns sessionFor("ada@example.com")

            val result = capturer.captureTrees(2, wallet = "Ada@Example.com", latitude = null, longitude = null, note = null)

            assertEquals(ACTIVE_SESSION_ID, result.sessionId)
            assertEquals(2, savedTrees.count { it.sessionId == ACTIVE_SESSION_ID })
            coVerify(exactly = 0) { sessionTracker.createSession(any(), any(), any()) }
            coVerify(exactly = 0) { sessionTracker.closeSession(any()) }
            coVerify(exactly = 0) { sessionTracker.endSession() }
            coVerify { treesToSyncHelper.refreshTreeCountToSync() }
        }

    @Test
    fun `WHEN a session for another wallet is running THEN it fails without saving`() =
        runTest {
            every { sessionTracker.currentSessionId } returns ACTIVE_SESSION_ID
            coEvery { dao.getSessionById(ACTIVE_SESSION_ID) } returns sessionFor(powerUser.wallet)

            assertFailsWith<IllegalArgumentException> {
                capturer.captureTrees(2, wallet = "someone-else", latitude = null, longitude = null, note = null)
            }
            assertTrue(savedTrees.isEmpty())
        }

    @Test
    fun `WHEN saving a tree fails THEN the session is still closed`() =
        runTest {
            coEvery { createTreeUseCase.execute(any()) } throws IllegalStateException("db full")

            assertFailsWith<IllegalStateException> {
                capturer.captureTrees(2, wallet = null, latitude = null, longitude = null, note = null)
            }
            coVerify(exactly = 1) { sessionTracker.closeSession(NEW_SESSION_ID) }
        }

    @Test
    fun `WHEN the call is cancelled THEN its session is still closed`() =
        runTest {
            coEvery { createTreeUseCase.execute(any()) } coAnswers { awaitCancellation() }
            var closedSessionId: Long? = null
            // yield() throws in a cancelled coroutine, so this only completes if closing is NonCancellable
            coEvery { sessionTracker.closeSession(any()) } coAnswers {
                yield()
                closedSessionId = firstArg<Long>()
            }

            val job = launch { capturer.captureTrees(3, wallet = null, latitude = null, longitude = null, note = null) }
            runCurrent()
            job.cancelAndJoin()

            assertEquals(NEW_SESSION_ID, closedSessionId)
        }

    @Test
    fun `WHEN there is no account THEN it fails`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null
            coEvery { userRepo.getUserWithWallet(any()) } returns null

            val noAdmin = assertFailsWith<IllegalArgumentException> { capturer.captureTrees(1, null, null, null, null) }
            val unknownWallet = assertFailsWith<IllegalArgumentException> { capturer.captureTrees(1, "nobody", null, null, null) }

            assertTrue(noAdmin.message!!.startsWith("No account exists yet"))
            assertTrue(unknownWallet.message!!.startsWith("No account with wallet 'nobody'"))
            coVerify(exactly = 0) { sessionTracker.createSession(any(), any(), any()) }
        }

    @Test
    fun `WHEN input is invalid THEN it fails`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { capturer.captureTrees(0, null, null, null, null) }
            assertFailsWith<IllegalArgumentException> {
                capturer.captureTrees(AgentTreeCapturer.MAX_TREES + 1, null, null, null, null)
            }
            assertFailsWith<IllegalArgumentException> { capturer.captureTrees(1, null, 10.0, null, null) }
            assertFailsWith<IllegalArgumentException> { capturer.captureTrees(1, null, 91.0, 0.0, null) }
            assertFailsWith<IllegalArgumentException> { capturer.captureTrees(1, null, 0.0, 181.0, null) }
            assertTrue(savedTrees.isEmpty())
        }

    private fun sessionFor(wallet: String) =
        SessionEntity(
            uuid = "session-uuid",
            originUserId = "user-uuid",
            originWallet = wallet,
            destinationWallet = wallet,
            startTime = Clock.System.now(),
            organization = "Greenstand",
            isUploaded = false,
            deviceConfigId = 1L,
        )

    companion object {
        private const val NEW_SESSION_ID = 7L
        private const val ACTIVE_SESSION_ID = 3L
    }
}