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
package org.greenstand.android.TreeTracker.usecases

import android.content.Context
import android.location.Location
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.location.LocationUpdateManager
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.LocationUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CreateFakeTreesUseCaseTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var createTreeUseCase: CreateTreeUseCase

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var locationUpdateManager: LocationUpdateManager

    @MockK(relaxed = true)
    private lateinit var createLegacyTreeUseCase: CreateLegacyTreeUseCase

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    private lateinit var createFakeTreesUseCase: CreateFakeTreesUseCase

    private val fakeImageFile = File("fake-images/test-tree.jpg")
    private val fakeTime = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    private val createdTrees = mutableListOf<Tree>()
    private val createdLegacyTrees = mutableListOf<CreateLegacyTreeParams>()
    private val insertedPlanters = mutableListOf<PlanterInfoEntity>()
    private val insertedCheckIns = mutableListOf<PlanterCheckInEntity>()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(FeatureFlags)
        every { FeatureFlags.DEBUG_ENABLED } returns true
        every { FeatureFlags.BETA } returns false
        mockkObject(ImageUtils)
        every { ImageUtils.createTestImageFile(any(), any()) } returns fakeImageFile
        mockkObject(LocationUtils)
        every { LocationUtils.generateRandomLocationInRadius(any(), any(), any()) } returns Pair(RANDOM_LAT, RANDOM_LON)

        every { sessionTracker.currentSessionId } returns SESSION_ID
        every { locationUpdateManager.currentLocation } returns null
        every { timeProvider.currentTime() } returns fakeTime
        coEvery { createTreeUseCase.execute(capture(createdTrees)) } returns 1L
        coEvery { createLegacyTreeUseCase.execute(capture(createdLegacyTrees)) } returns 1L
        coEvery { dao.insertPlanterCheckIn(capture(insertedCheckIns)) } returns 1L
        coEvery { dao.insertPlanterInfo(capture(insertedPlanters)) } returnsMany listOf(PLANTER_ONE_ID, PLANTER_TWO_ID)
        coEvery { dao.getPlanterInfoById(PLANTER_ONE_ID) } returns
            FakeFileGenerator.fakePlanterInfo.copy(identifier = "@TestUser1").also { it.id = PLANTER_ONE_ID }
        coEvery { dao.getPlanterInfoById(PLANTER_TWO_ID) } returns
            FakeFileGenerator.fakePlanterInfo.copy(identifier = "@TestUser2").also { it.id = PLANTER_TWO_ID }

        createFakeTreesUseCase =
            CreateFakeTreesUseCase(
                sessionTracker = sessionTracker,
                context = context,
                createTreeUseCase = createTreeUseCase,
                dao = dao,
                locationUpdateManager = locationUpdateManager,
                createLegacyTreeUseCase = createLegacyTreeUseCase,
                timeProvider = timeProvider,
            )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `WHEN neither debug nor beta build THEN refuses to create fake data`() =
        runTest {
            every { FeatureFlags.DEBUG_ENABLED } returns false
            every { FeatureFlags.BETA } returns false

            assertFailsWith<IllegalStateException> {
                createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 8))
            }

            coVerify(exactly = 0) { createTreeUseCase.execute(any()) }
            coVerify(exactly = 0) { dao.insertPlanterInfo(any()) }
        }

    @Test
    fun `WHEN beta build THEN creates fake data`() =
        runTest {
            every { FeatureFlags.DEBUG_ENABLED } returns false
            every { FeatureFlags.BETA } returns true

            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 2))

            coVerify(exactly = 2) { createTreeUseCase.execute(any()) }
        }

    @Test
    fun `WHEN amount is eight THEN creates five trees and four legacy trees for two planters`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 8))

            assertEquals(5, createdTrees.size)
            assertEquals(4, createdLegacyTrees.size)
            assertEquals(2, insertedPlanters.size)
            assertEquals(4, insertedCheckIns.size)
        }

    @Test
    fun `WHEN amount is zero THEN still creates one tree and both planters without legacy trees`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 0))

            assertEquals(1, createdTrees.size)
            assertTrue(createdLegacyTrees.isEmpty())
            assertEquals(2, insertedPlanters.size)
            assertEquals(4, insertedCheckIns.size)
        }

    @Test
    fun `WHEN trees created THEN they use the current session test image random location and fake attributes`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 2))

            val tree = createdTrees.first()
            assertEquals(SESSION_ID, tree.sessionId)
            assertEquals(fakeImageFile.absolutePath, tree.photoPath)
            assertEquals("My Note", tree.content)
            assertEquals(RANDOM_LAT, tree.meanLatitude)
            assertEquals(RANDOM_LON, tree.meanLongitude)
            assertEquals(
                mapOf(
                    Tree.DBH_ATTR_KEY to "10",
                    Tree.ABS_STEP_COUNT_KEY to "10",
                    Tree.DELTA_STEP_COUNT_KEY to "10",
                    Tree.ROTATION_MATRIX_KEY to "[12,12,12]",
                ),
                tree.treeCaptureAttributes(),
            )
        }

    @Test
    fun `WHEN trees created THEN every tree gets a unique uuid`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 8))

            val uuids = (createdTrees.map { it.treeUuid } + createdLegacyTrees.map { it.tree.treeUuid }).toSet()
            assertEquals(createdTrees.size + createdLegacyTrees.size, uuids.size)
        }

    @Test
    fun `WHEN legacy users created THEN inserts two not uploaded planters with test identities`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 0))

            assertEquals(listOf("@TestUser1", "@TestUser2"), insertedPlanters.map { it.identifier })
            assertEquals(listOf("TestUser1", "TestUser2"), insertedPlanters.map { it.firstName })
            insertedPlanters.forEach { planter ->
                assertEquals("Umbrella Corp", planter.organization)
                assertEquals(false, planter.uploaded)
                assertEquals(fakeTime.toEpochMilliseconds(), planter.createdAt)
                assertEquals(0.0, planter.latitude)
                assertEquals(0.0, planter.longitude)
                assertTrue(planter.recordUuid.isNotEmpty())
            }
        }

    @Test
    fun `WHEN legacy check ins created THEN planter one checks in three times and planter two once`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 0))

            assertEquals(
                listOf(PLANTER_ONE_ID, PLANTER_ONE_ID, PLANTER_TWO_ID, PLANTER_ONE_ID),
                insertedCheckIns.map { it.planterInfoId },
            )
            insertedCheckIns.forEach { checkIn ->
                assertNull(checkIn.photoUrl)
                assertEquals(fakeImageFile.absolutePath, checkIn.localPhotoPath)
                assertEquals(fakeTime.toEpochMilliseconds(), checkIn.createdAt)
                assertEquals(RANDOM_LAT, checkIn.latitude)
                assertEquals(RANDOM_LON, checkIn.longitude)
            }
        }

    @Test
    fun `WHEN legacy trees created THEN they use the legacy note and current session`() =
        runTest {
            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 8))

            createdLegacyTrees.forEach { params ->
                assertEquals("My Legacy Note", params.tree.content)
                assertEquals(SESSION_ID, params.tree.sessionId)
                assertEquals(fakeImageFile.absolutePath, params.tree.photoPath)
            }
        }

    @Test
    fun `WHEN a location is known THEN it is used as the center of the random locations`() =
        runTest {
            val location = mockk<Location>(relaxed = true)
            every { location.latitude } returns 10.0
            every { location.longitude } returns 20.0
            every { locationUpdateManager.currentLocation } returns location

            createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 0))

            verify(atLeast = 1) { LocationUtils.generateRandomLocationInRadius(10.0, 20.0, any()) }
            assertEquals(10.0, insertedPlanters.first().latitude)
            assertEquals(20.0, insertedPlanters.first().longitude)
        }

    @Test
    fun `WHEN no session is active THEN fails before creating trees`() =
        runTest {
            every { sessionTracker.currentSessionId } returns null

            assertFailsWith<NullPointerException> {
                createFakeTreesUseCase.execute(CreateFakeTreesParams(amount = 2))
            }

            coVerify(exactly = 0) { createTreeUseCase.execute(any()) }
        }

    private companion object {
        const val SESSION_ID = 7L
        const val PLANTER_ONE_ID = 11L
        const val PLANTER_TWO_ID = 22L
        const val RANDOM_LAT = 1.5
        const val RANDOM_LON = 2.5
    }
}