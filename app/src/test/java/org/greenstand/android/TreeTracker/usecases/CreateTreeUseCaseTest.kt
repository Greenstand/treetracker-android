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
package org.greenstand.android.TreeTracker.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class CreateTreeUseCaseTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var analytics: Analytics

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    private lateinit var createTreeUseCase: CreateTreeUseCase

    private val fakeTree =
        Tree(
            treeUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            sessionId = 100L,
            content = "Test note",
            photoPath = "/photos/tree.jpg",
            meanLongitude = 36.82,
            meanLatitude = -1.29,
        )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        createTreeUseCase =
            CreateTreeUseCase(
                dao = dao,
                analytics = analytics,
                timeProvider = timeProvider,
            )
    }

    @Test
    fun `WHEN execute called THEN inserts TreeEntity via DAO and returns ID`() =
        runTest {
            val fakeTime = Instant.fromEpochMilliseconds(1000000L)
            every { timeProvider.currentTime() } returns fakeTime
            coEvery { dao.insertTree(any()) } returns 55L

            val result = createTreeUseCase.execute(fakeTree)

            assertEquals(55L, result)
            coVerify { dao.insertTree(any()) }
        }

    @Test
    fun `WHEN execute called THEN calls analytics treePlanted`() =
        runTest {
            val fakeTime = Instant.fromEpochMilliseconds(1000000L)
            every { timeProvider.currentTime() } returns fakeTime
            coEvery { dao.insertTree(any()) } returns 1L

            createTreeUseCase.execute(fakeTree)

            verify { analytics.treePlanted() }
        }

    @Test
    fun `WHEN execute called THEN uses timeProvider for createdAt`() =
        runTest {
            val fakeTime = Instant.fromEpochMilliseconds(5555555L)
            every { timeProvider.currentTime() } returns fakeTime
            coEvery { dao.insertTree(any()) } returns 1L

            createTreeUseCase.execute(fakeTree)

            verify { timeProvider.currentTime() }
            coVerify {
                dao.insertTree(match { it.createdAt == fakeTime })
            }
        }

    @Test
    fun `WHEN execute called THEN maps every tree field onto the inserted entity`() =
        runTest {
            val fakeTime = Instant.fromEpochMilliseconds(1000000L)
            every { timeProvider.currentTime() } returns fakeTime
            val entitySlot = slot<TreeEntity>()
            coEvery { dao.insertTree(capture(entitySlot)) } returns 1L

            createTreeUseCase.execute(fakeTree)

            val entity = entitySlot.captured
            assertEquals("550e8400-e29b-41d4-a716-446655440000", entity.uuid)
            assertEquals(100L, entity.sessionId)
            assertEquals("/photos/tree.jpg", entity.photoPath)
            assertNull(entity.photoUrl)
            assertEquals("Test note", entity.note)
            assertEquals(-1.29, entity.latitude)
            assertEquals(36.82, entity.longitude)
            assertEquals(false, entity.uploaded)
            assertNull(entity.bundleId)
        }

    @Test
    fun `WHEN tree has attributes THEN they are stored as extra attributes`() =
        runTest {
            every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000000L)
            val entitySlot = slot<TreeEntity>()
            coEvery { dao.insertTree(capture(entitySlot)) } returns 1L
            val tree =
                Tree(
                    treeUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                    sessionId = 100L,
                    content = "With attributes",
                    photoPath = "/photos/tree2.jpg",
                    meanLongitude = 36.82,
                    meanLatitude = -1.29,
                ).apply {
                    addTreeAttribute(Tree.DBH_ATTR_KEY, "12.5")
                    addTreeAttribute(Tree.TREE_COLOR_ATTR_KEY, "green")
                }

            createTreeUseCase.execute(tree)

            assertEquals(
                mapOf(Tree.DBH_ATTR_KEY to "12.5", Tree.TREE_COLOR_ATTR_KEY to "green"),
                entitySlot.captured.extraAttributes,
            )
        }

    @Test
    fun `WHEN execute called THEN records analytics before inserting the tree`() =
        runTest {
            every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000000L)
            coEvery { dao.insertTree(any()) } returns 1L

            createTreeUseCase.execute(fakeTree)

            coVerifyOrder {
                analytics.treePlanted()
                dao.insertTree(any())
            }
        }

    @Test
    fun `WHEN insert fails THEN exception propagates to the caller`() =
        runTest {
            every { timeProvider.currentTime() } returns Instant.fromEpochMilliseconds(1000000L)
            coEvery { dao.insertTree(any()) } throws IllegalStateException("db closed")

            assertFailsWith<IllegalStateException> {
                createTreeUseCase.execute(fakeTree)
            }
        }
}