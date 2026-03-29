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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class CreateTreeRequestUseCaseTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    private lateinit var createTreeRequestUseCase: CreateTreeRequestUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        createTreeRequestUseCase = CreateTreeRequestUseCase(dao = dao)
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `WHEN execute called THEN builds NewTreeRequest from DAO data`() =
        runTest {
            val treeCapture = FakeFileGenerator.fakeTreeCapture.copy().also { it.id = 10L }
            val planterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy().also { it.id = treeCapture.planterCheckInId }
            val planterInfo = FakeFileGenerator.fakePlanterInfo.copy().also { it.id = planterCheckIn.planterInfoId }
            val attributes =
                listOf(
                    TreeAttributeEntity(key = "color", value = "green", treeCaptureId = 10L),
                )

            coEvery { dao.getTreeCaptureById(10L) } returns treeCapture
            coEvery { dao.getPlanterCheckInById(treeCapture.planterCheckInId) } returns planterCheckIn
            coEvery { dao.getPlanterInfoById(planterCheckIn.planterInfoId) } returns planterInfo
            coEvery { dao.getTreeAttributeByTreeCaptureId(10L) } returns attributes

            val params = CreateTreeRequestParams(treeId = 10L, treeImageUrl = "https://example.com/tree.jpg")
            val result = createTreeRequestUseCase.execute(params)

            assertNotNull(result)
            assertEquals(treeCapture.uuid, result.uuid)
            assertEquals("https://example.com/tree.jpg", result.imageUrl)
            assertEquals(planterCheckIn.id.toInt(), result.userId)
            assertEquals(treeCapture.id, result.sequenceId)
            assertEquals(treeCapture.latitude, result.lat)
            assertEquals(treeCapture.longitude, result.lon)
            assertEquals(planterInfo.identifier, result.planterIdentifier)
            assertEquals(planterCheckIn.photoUrl, result.planterPhotoUrl)
            assertEquals(treeCapture.createAt, result.timestamp)
            assertEquals(treeCapture.noteContent, result.note)
            assertEquals(1, result.attributes?.size)
            assertEquals("color", result.attributes?.first()?.key)
            assertEquals("green", result.attributes?.first()?.value)
        }

    @Test
    fun `WHEN execute called and planter info not found THEN throws IllegalStateException`() =
        runTest {
            val treeCapture = FakeFileGenerator.fakeTreeCapture.copy().also { it.id = 10L }
            val planterCheckIn = FakeFileGenerator.fakePlanterCheckInEntity.copy().also { it.id = treeCapture.planterCheckInId }

            coEvery { dao.getTreeCaptureById(10L) } returns treeCapture
            coEvery { dao.getPlanterCheckInById(treeCapture.planterCheckInId) } returns planterCheckIn
            coEvery { dao.getPlanterInfoById(planterCheckIn.planterInfoId) } returns null

            val params = CreateTreeRequestParams(treeId = 10L, treeImageUrl = "https://example.com/tree.jpg")

            assertFailsWith<IllegalStateException> {
                createTreeRequestUseCase.execute(params)
            }
        }
}