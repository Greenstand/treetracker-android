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
package org.greenstand.android.TreeTracker.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MapViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN init loads trees THEN markers are mapped and isLoading is false`() = runTest {
        val treeEntity1 = TreeEntity(
            uuid = "uuid-1",
            sessionId = 1,
            photoPath = "/photo1.jpg",
            photoUrl = null,
            note = "tree note 1",
            latitude = 12.34,
            longitude = 56.78,
            uploaded = false,
            createdAt = Instant.fromEpochMilliseconds(1000000),
        ).apply { id = 1L }

        val treeEntity2 = TreeEntity(
            uuid = "uuid-2",
            sessionId = 2,
            photoPath = "/photo2.jpg",
            photoUrl = null,
            note = "tree note 2",
            latitude = 34.56,
            longitude = 78.90,
            uploaded = true,
            createdAt = Instant.fromEpochMilliseconds(2000000),
        ).apply { id = 2L }

        coEvery { dao.getAllTrees() } returns listOf(treeEntity1, treeEntity2)

        val viewModel = MapViewModel(dao)

        val state = viewModel.state.first { !it.isLoading }
        assertEquals(2, state.markers.size)
        assertEquals("tree_1", state.markers[0].id)
        assertEquals(12.34, state.markers[0].latitude)
        assertEquals(56.78, state.markers[0].longitude)
        assertFalse(state.markers[0].isUploaded)
        assertEquals("tree note 1", state.markers[0].note)
        assertEquals("/photo1.jpg", state.markers[0].imagePath)
        assertEquals("tree_2", state.markers[1].id)
        assertTrue(state.markers[1].isUploaded)
    }

    @Test
    fun `WHEN init with empty tree list THEN markers are empty`() = runTest {
        coEvery { dao.getAllTrees() } returns emptyList()

        val viewModel = MapViewModel(dao)

        val state = viewModel.state.first { !it.isLoading }
        assertTrue(state.markers.isEmpty())
    }

    @Test
    fun `WHEN selectMarker called THEN selectedMarkerId updates in state`() = runTest {
        coEvery { dao.getAllTrees() } returns emptyList()

        val viewModel = MapViewModel(dao)
        viewModel.state.first { !it.isLoading }

        viewModel.handleAction(MapAction.SelectMarker("tree_42"))

        assertEquals("tree_42", viewModel.state.value.selectedMarkerId)
    }
}
