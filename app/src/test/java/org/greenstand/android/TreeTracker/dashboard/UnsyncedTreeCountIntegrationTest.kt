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
package org.greenstand.android.TreeTracker.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.amazonaws.AmazonClientException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The "trees left to sync" count after a sync where the image uploaded but the bundle PUT failed,
 * using a real database, the real TreeUploader (with mocked storage) and the real TreesToSyncHelper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UnsyncedTreeCountIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database =
        Room
            .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = database.treeTrackerDao()

    // Same settings as the app's Json (AppModule)
    private val json =
        Json {
            explicitNulls = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Before
    fun setUp() {
        // UploadBundle reads the device id, which needs the Application context that unit tests don't set up
        mockkObject(DeviceUtils)
        every { DeviceUtils.deviceId } returns "test-device-id"
    }

    @After
    fun tearDown() {
        unmockkObject(DeviceUtils)
        database.close()
    }

    @Test
    fun `tree whose image uploaded but bundle failed is still counted as left to sync`() =
        runTest {
            val deviceConfigId = dao.insertDeviceConfig(FakeFileGenerator.fakeDeviceConfig)
            val sessionId = dao.insertSession(FakeFileGenerator.fakeSession.copy(deviceConfigId = deviceConfigId))
            val treeId =
                dao.insertTree(
                    FakeFileGenerator.fakeTree.first().copy(sessionId = sessionId, photoUrl = null, uploaded = false),
                )

            val uploadImageUseCase = mockk<UploadImageUseCase>()
            coEvery { uploadImageUseCase.execute(any()) } returns "https://images/tree.jpg"
            val objectStorageClient = mockk<ObjectStorageClient>()
            every { objectStorageClient.uploadBundle(any(), any()) } throws AmazonClientException("Unable to execute HTTP request")
            val treeUploader = TreeUploader(uploadImageUseCase, objectStorageClient, mockk(relaxed = true), dao, json)

            treeUploader.uploadTrees(listOf(treeId))

            // The image uploaded and the bundle PUT was attempted and failed
            verify(exactly = 1) { objectStorageClient.uploadBundle(any(), any()) }
            val tree = dao.getTreesByIds(listOf(treeId)).single()
            assertEquals("https://images/tree.jpg", tree.photoUrl)
            assertFalse(tree.uploaded)

            val treesToSyncHelper =
                TreesToSyncHelper(Preferences(context.getSharedPreferences("test", Context.MODE_PRIVATE)), dao)
            treesToSyncHelper.refreshTreeCountToSync()
            assertEquals(1, treesToSyncHelper.getTreeCountToSync())
        }
}