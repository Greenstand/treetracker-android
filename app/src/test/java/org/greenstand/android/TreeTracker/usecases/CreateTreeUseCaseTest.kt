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
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

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

    private val fakeTree = Tree(
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
        createTreeUseCase = CreateTreeUseCase(
            dao = dao,
            analytics = analytics,
            timeProvider = timeProvider,
        )
    }

    @Test
    fun `WHEN execute called THEN inserts TreeEntity via DAO and returns ID`() = runTest {
        val fakeTime = Instant.fromEpochMilliseconds(1000000L)
        every { timeProvider.currentTime() } returns fakeTime
        coEvery { dao.insertTree(any()) } returns 55L

        val result = createTreeUseCase.execute(fakeTree)

        assertEquals(55L, result)
        coVerify { dao.insertTree(any()) }
    }

    @Test
    fun `WHEN execute called THEN calls analytics treePlanted`() = runTest {
        val fakeTime = Instant.fromEpochMilliseconds(1000000L)
        every { timeProvider.currentTime() } returns fakeTime
        coEvery { dao.insertTree(any()) } returns 1L

        createTreeUseCase.execute(fakeTree)

        verify { analytics.treePlanted() }
    }

    @Test
    fun `WHEN execute called THEN uses timeProvider for createdAt`() = runTest {
        val fakeTime = Instant.fromEpochMilliseconds(5555555L)
        every { timeProvider.currentTime() } returns fakeTime
        coEvery { dao.insertTree(any()) } returns 1L

        createTreeUseCase.execute(fakeTree)

        verify { timeProvider.currentTime() }
        coVerify {
            dao.insertTree(match { it.createdAt == fakeTime })
        }
    }
}
