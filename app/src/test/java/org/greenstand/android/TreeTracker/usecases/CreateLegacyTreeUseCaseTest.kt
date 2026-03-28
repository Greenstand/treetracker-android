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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class CreateLegacyTreeUseCaseTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var timeProvider: TimeProvider

    private lateinit var createLegacyTreeUseCase: CreateLegacyTreeUseCase

    private val fakeTree = Tree(
        treeUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
        sessionId = 100L,
        content = "Legacy note",
        photoPath = "/photos/legacy-tree.jpg",
        meanLongitude = 36.82,
        meanLatitude = -1.29,
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        createLegacyTreeUseCase = CreateLegacyTreeUseCase(
            dao = dao,
            timeProvider = timeProvider,
        )
    }

    @Test
    fun `WHEN execute called THEN creates TreeCaptureEntity with correct fields and inserts with attributes`() = runTest {
        val fakeTime = Instant.fromEpochMilliseconds(5000000L)
        every { timeProvider.currentTime() } returns fakeTime
        coEvery { dao.insertTreeWithAttributes(any(), any()) } returns 77L

        val params = CreateLegacyTreeParams(
            planterCheckInId = 42L,
            tree = fakeTree,
        )

        val result = createLegacyTreeUseCase.execute(params)

        assertEquals(77L, result)
        coVerify {
            dao.insertTreeWithAttributes(
                match { entity ->
                    entity.uuid == "550e8400-e29b-41d4-a716-446655440000" &&
                        entity.planterCheckInId == 42L &&
                        entity.localPhotoPath == "/photos/legacy-tree.jpg" &&
                        entity.photoUrl == null &&
                        entity.noteContent == "Legacy note" &&
                        entity.longitude == 36.82 &&
                        entity.latitude == -1.29 &&
                        entity.accuracy == 0.0 &&
                        entity.createAt == fakeTime.epochSeconds
                },
                any()
            )
        }
    }

    @Test
    fun `WHEN execute called THEN uses timeProvider currentTime epochSeconds for createAt`() = runTest {
        val fakeTime = Instant.fromEpochMilliseconds(9999000L)
        every { timeProvider.currentTime() } returns fakeTime
        coEvery { dao.insertTreeWithAttributes(any(), any()) } returns 1L

        val params = CreateLegacyTreeParams(
            planterCheckInId = 1L,
            tree = fakeTree,
        )

        createLegacyTreeUseCase.execute(params)

        verify { timeProvider.currentTime() }
        coVerify {
            dao.insertTreeWithAttributes(
                match { it.createAt == fakeTime.epochSeconds },
                any()
            )
        }
    }
}
