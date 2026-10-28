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
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.UserEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterCheckInEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.PlanterInfoEntity
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class PlanterUploaderTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var uploadImageUseCase: UploadImageUseCase

    @MockK(relaxed = true)
    private lateinit var objectStorageClient: ObjectStorageClient

    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private lateinit var planterUploader: PlanterUploader

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
        planterUploader =
            PlanterUploader(
                dao = dao,
                uploadImageUseCase = uploadImageUseCase,
                json = json,
                objectStorageClient = objectStorageClient,
            )
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
    }

    @Test
    fun `WHEN upload called THEN uploads user images planter info bundles and user bundles`() =
        runTest {
            val userEntity =
                UserEntity(
                    uuid = "user-uuid-1",
                    wallet = "wallet-1",
                    firstName = "John",
                    lastName = "Doe",
                    phone = "555-1234",
                    email = "john@test.com",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                    photoPath = "/test/user-photo.jpg",
                    photoUrl = null,
                    powerUser = false,
                ).apply { id = 1L }

            val planterInfo =
                PlanterInfoEntity(
                    identifier = "planter-id",
                    firstName = "Jane",
                    lastName = "Doe",
                    organization = "TestOrg",
                    phone = "555-5678",
                    email = "jane@test.com",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = System.currentTimeMillis(),
                    recordUuid = "record-uuid-1",
                ).apply { id = 1L }

            val planterCheckIn =
                PlanterCheckInEntity(
                    planterInfoId = 1L,
                    localPhotoPath = "/test/checkin-photo.jpg",
                    photoUrl = "https://uploaded.url/checkin.jpg",
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = System.currentTimeMillis(),
                ).apply { id = 1L }

            coEvery { dao.getAllUsersToUpload() } returns listOf(userEntity)
            coEvery { uploadImageUseCase.execute(any()) } returns "https://uploaded.url/user.jpg"
            coEvery { dao.getAllPlanterInfoToUpload() } returns listOf(planterInfo)
            coEvery { dao.getAllPlanterCheckInsForPlanterInfoId(1L) } returns listOf(planterCheckIn)
            coEvery { dao.getPlanterCheckInsToUpload() } returns listOf(planterCheckIn)

            planterUploader.upload("instance-123")

            coVerify(atLeast = 1) { uploadImageUseCase.execute(any()) }
            coVerify(atLeast = 1) { objectStorageClient.uploadBundle(any(), any()) }
            coVerify(exactly = 1) { dao.updateUserUploadStatus(listOf(1L), true) }
        }

    @Test
    fun `WHEN users have existing photoUrl THEN skips image upload for those users`() =
        runTest {
            val userWithPhoto =
                UserEntity(
                    uuid = "user-uuid-2",
                    wallet = "wallet-2",
                    firstName = "Existing",
                    lastName = "Photo",
                    phone = null,
                    email = null,
                    latitude = 37.0,
                    longitude = -122.0,
                    createdAt = Instant.parse("2023-01-01T00:00:00Z"),
                    photoPath = "/test/user-photo2.jpg",
                    photoUrl = "https://already-uploaded.url/photo.jpg",
                    powerUser = false,
                ).apply { id = 2L }

            coEvery { dao.getAllUsersToUpload() } returns listOf(userWithPhoto)
            coEvery { dao.getAllPlanterInfoToUpload() } returns emptyList()
            coEvery { dao.getPlanterCheckInsToUpload() } returns emptyList()

            planterUploader.upload("instance-123")

            coVerify(exactly = 0) { uploadImageUseCase.execute(any()) }
        }
}