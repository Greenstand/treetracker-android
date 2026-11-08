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
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.models.ConvergenceStatus
import org.greenstand.android.TreeTracker.models.LocationData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `WHEN init loads trees THEN markers are mapped and isLoading is false`() =
        runTest {
            val treeEntity1 =
                TreeEntity(
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

            val treeEntity2 =
                TreeEntity(
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
    fun `WHEN init with empty tree list THEN markers are empty`() =
        runTest {
            coEvery { dao.getAllTrees() } returns emptyList()

            val viewModel = MapViewModel(dao)

            val state = viewModel.state.first { !it.isLoading }
            assertTrue(state.markers.isEmpty())
        }

    @Test
    fun `WHEN selectMarker called THEN selectedMarkerId updates in state`() =
        runTest {
            coEvery { dao.getAllTrees() } returns emptyList()

            val viewModel = MapViewModel(dao)
            viewModel.state.first { !it.isLoading }

            viewModel.handleAction(MapAction.SelectMarker("tree_42"))

            assertEquals("tree_42", viewModel.state.value.selectedMarkerId)
        }

    private fun treeEntity(
        id: Long,
        sessionId: Long,
    ): TreeEntity =
        TreeEntity(
            uuid = "uuid-$id",
            sessionId = sessionId,
            photoPath = null,
            photoUrl = null,
            note = "",
            latitude = 1.0,
            longitude = 2.0,
            uploaded = false,
            createdAt = Instant.fromEpochMilliseconds(id * 1000),
        ).apply { this.id = id }

    private fun locationJson(
        latitude: Double,
        longitude: Double,
    ): String =
        Json.encodeToString(
            LocationData.serializer(),
            LocationData(
                planterCheckInId = null,
                latitude = latitude,
                longitude = longitude,
                accuracy = 3.5f,
                treeUuid = "uuid-1",
                convergenceStatus = ConvergenceStatus.CONVERGED,
                capturedAt = "2026-04-01T10:00:00Z",
            ),
        )

    @Test
    fun `WHEN data is still loading THEN isLoading is true and nothing is selected`() =
        runTest {
            val gate = CompletableDeferred<List<TreeEntity>>()
            coEvery { dao.getAllTrees() } coAnswers { gate.await() }
            coEvery { dao.getLocationsForTreeSessions(any()) } returns emptyList()

            val viewModel = MapViewModel(dao)

            val loading = viewModel.state.value
            assertTrue(loading.isLoading)
            assertTrue(loading.markers.isEmpty())
            assertTrue(loading.stepPoints.isEmpty())
            assertNull(loading.selectedMarkerId)
            assertNull(loading.selectedSessionId)

            gate.complete(emptyList())
            val loaded = viewModel.state.first { !it.isLoading }
            assertTrue(loaded.markers.isEmpty())
        }

    @Test
    fun `WHEN init loads trees THEN markers carry session id and plant date`() =
        runTest {
            coEvery { dao.getAllTrees() } returns listOf(treeEntity(id = 7, sessionId = 3))
            coEvery { dao.getLocationsForTreeSessions(any()) } returns emptyList()

            val viewModel = MapViewModel(dao)

            val marker =
                viewModel.state
                    .first { !it.isLoading }
                    .markers
                    .single()
            assertEquals(3L, marker.sessionId)
            assertEquals(Instant.fromEpochMilliseconds(7000), marker.plantDate)
            assertNull(marker.imagePath)
        }

    @Test
    fun `WHEN init loads session locations THEN step points are parsed from the location json`() =
        runTest {
            coEvery { dao.getAllTrees() } returns emptyList()
            coEvery { dao.getLocationsForTreeSessions(any()) } returns
                listOf(
                    LocationEntity(locationDataJson = locationJson(1.5, 2.5), sessionId = 10),
                    LocationEntity(locationDataJson = locationJson(3.5, 4.5), sessionId = 11),
                )

            val viewModel = MapViewModel(dao)

            val state = viewModel.state.first { !it.isLoading }
            assertEquals(
                listOf(
                    StepPoint(latitude = 1.5, longitude = 2.5, sessionId = 10),
                    StepPoint(latitude = 3.5, longitude = 4.5, sessionId = 11),
                ),
                state.stepPoints,
            )
            coVerify { dao.getLocationsForTreeSessions(5000) }
        }

    @Test
    fun `WHEN a location json is malformed THEN it is skipped and the rest still load`() =
        runTest {
            coEvery { dao.getAllTrees() } returns listOf(treeEntity(id = 1, sessionId = 10))
            coEvery { dao.getLocationsForTreeSessions(any()) } returns
                listOf(
                    LocationEntity(locationDataJson = "not json", sessionId = 10),
                    LocationEntity(locationDataJson = "{}", sessionId = 10),
                    LocationEntity(locationDataJson = locationJson(5.0, 6.0), sessionId = 10),
                )

            val viewModel = MapViewModel(dao)

            val state = viewModel.state.first { !it.isLoading }
            assertEquals(listOf(StepPoint(latitude = 5.0, longitude = 6.0, sessionId = 10)), state.stepPoints)
            assertEquals(1, state.markers.size)
        }

    @Test
    fun `WHEN known marker is selected THEN its session is selected too`() =
        runTest {
            coEvery { dao.getAllTrees() } returns listOf(treeEntity(id = 1, sessionId = 10), treeEntity(id = 2, sessionId = 20))
            coEvery { dao.getLocationsForTreeSessions(any()) } returns emptyList()
            val viewModel = MapViewModel(dao)
            viewModel.state.first { !it.isLoading }

            viewModel.handleAction(MapAction.SelectMarker("tree_2"))

            assertEquals("tree_2", viewModel.state.value.selectedMarkerId)
            assertEquals(20L, viewModel.state.value.selectedSessionId)
        }

    @Test
    fun `WHEN another marker is selected THEN selection moves to the new marker and session`() =
        runTest {
            coEvery { dao.getAllTrees() } returns listOf(treeEntity(id = 1, sessionId = 10), treeEntity(id = 2, sessionId = 20))
            coEvery { dao.getLocationsForTreeSessions(any()) } returns emptyList()
            val viewModel = MapViewModel(dao)
            viewModel.state.first { !it.isLoading }

            viewModel.handleAction(MapAction.SelectMarker("tree_2"))
            viewModel.handleAction(MapAction.SelectMarker("tree_1"))

            assertEquals("tree_1", viewModel.state.value.selectedMarkerId)
            assertEquals(10L, viewModel.state.value.selectedSessionId)
        }

    @Test
    fun `WHEN unknown marker is selected THEN selected session is cleared`() =
        runTest {
            coEvery { dao.getAllTrees() } returns listOf(treeEntity(id = 1, sessionId = 10))
            coEvery { dao.getLocationsForTreeSessions(any()) } returns emptyList()
            val viewModel = MapViewModel(dao)
            viewModel.state.first { !it.isLoading }
            viewModel.handleAction(MapAction.SelectMarker("tree_1"))

            viewModel.handleAction(MapAction.SelectMarker("tree_99"))

            assertEquals("tree_99", viewModel.state.value.selectedMarkerId)
            assertNull(viewModel.state.value.selectedSessionId)
            assertFalse(viewModel.state.value.isLoading)
            assertTrue(viewModel.events.replayCache.isEmpty())
        }
}