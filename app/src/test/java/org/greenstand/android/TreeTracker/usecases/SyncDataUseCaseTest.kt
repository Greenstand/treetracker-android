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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.SyncStep
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class SyncDataUseCaseTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK(relaxed = true)
    private lateinit var treeUploader: TreeUploader

    @MockK(relaxed = true)
    private lateinit var uploadLocationDataUseCase: UploadLocationDataUseCase

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var planterUploader: PlanterUploader

    @MockK(relaxed = true)
    private lateinit var sessionUploader: SessionUploader

    @MockK(relaxed = true)
    private lateinit var deviceConfigUploader: DeviceConfigUploader

    @MockK(relaxed = true)
    private lateinit var messagesRepo: MessagesRepo

    @MockK(relaxed = true)
    private lateinit var syncProgressTracker: SyncProgressTracker

    private lateinit var syncDataUseCase: SyncDataUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(FirebaseInstallations::class)
        val mockInstallations = mockk<FirebaseInstallations>()
        every { FirebaseInstallations.getInstance() } returns mockInstallations
        every { mockInstallations.id } returns Tasks.forResult("test-instance-id")

        syncDataUseCase =
            SyncDataUseCase(
                treeUploader = treeUploader,
                uploadLocationDataUseCase = uploadLocationDataUseCase,
                dao = dao,
                planterUploader = planterUploader,
                sessionUploader = sessionUploader,
                deviceConfigUploader = deviceConfigUploader,
                messagesRepo = messagesRepo,
                syncProgressTracker = syncProgressTracker,
            )
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseInstallations::class)
    }

    @Test
    fun `WHEN all steps succeed THEN returns true`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            val result = syncDataUseCase.execute(Unit)

            assertTrue(result)
        }

    @Test
    fun `WHEN all steps succeed THEN calls each uploader in order`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            syncDataUseCase.execute(Unit)

            coVerify { messagesRepo.syncMessages() }
            coVerify { deviceConfigUploader.upload(any()) }
            coVerify { planterUploader.upload(any()) }
            coVerify { sessionUploader.upload() }
            coVerify { uploadLocationDataUseCase.execute(Unit) }
        }

    @Test
    fun `WHEN message sync fails THEN returns false`() =
        runTest {
            coEvery { messagesRepo.syncMessages() } throws RuntimeException("Network error")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
        }

    @Test
    fun `WHEN tree upload fails THEN returns false`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns listOf(1L, 2L)
            coEvery { treeUploader.uploadTrees(any()) } throws RuntimeException("Upload failed")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
        }

    @Test
    fun `WHEN sync succeeds THEN starts and ends sync on progress tracker`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            syncDataUseCase.execute(Unit)

            verify { syncProgressTracker.startSync() }
            verify { syncProgressTracker.endSync() }
        }

    @Test
    fun `WHEN sync fails THEN ends sync with error`() =
        runTest {
            coEvery { messagesRepo.syncMessages() } throws RuntimeException("fail")

            syncDataUseCase.execute(Unit)

            verify { syncProgressTracker.startSync() }
            verify { syncProgressTracker.endSync(error = any()) }
        }

    @Test
    fun `WHEN sync has trees to upload THEN uploads them`() =
        runTest {
            val treeIds = listOf(1L, 2L, 3L)
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(treeIds, emptyList())

            syncDataUseCase.execute(Unit)

            coVerify { treeUploader.uploadTrees(treeIds) }
        }

    @Test
    fun `WHEN some trees are left after upload THEN still uploads locations and returns false`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            // Tree 2 is still pending after the upload pass
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L, 2L, 3L), listOf(2L))

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            coVerify(exactly = 1) { treeUploader.uploadTrees(listOf(1L, 2L, 3L)) }
            verify { syncProgressTracker.failStep(SyncStep.TREES, any()) }
            verify(exactly = 0) { syncProgressTracker.completeStep(SyncStep.TREES) }
            coVerify { uploadLocationDataUseCase.execute(Unit) }
            verify { syncProgressTracker.endSync(error = "Some trees failed to upload") }
        }

    @Test
    fun `WHEN legacy trees are left THEN the new trees step still runs`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns listOf(10L)
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L), emptyList())

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.LEGACY_TREES, any()) }
            coVerify(exactly = 1) { treeUploader.uploadTrees(listOf(1L)) }
            verify { syncProgressTracker.completeStep(SyncStep.TREES) }
        }

    @Test
    fun `WHEN trees are captured during sync and one keeps failing THEN uploads the new ones and returns false`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            // Tree 2 keeps failing; tree 3 is captured while the first pass runs
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L, 2L), listOf(2L, 3L), listOf(2L))

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            coVerify(exactly = 1) { treeUploader.uploadTrees(listOf(1L, 2L)) }
            coVerify(exactly = 1) { treeUploader.uploadTrees(listOf(2L, 3L)) }
            verify { syncProgressTracker.failStep(SyncStep.TREES, "1 trees failed to upload") }
        }

    @Test
    fun `WHEN sync has legacy trees to upload THEN uploads them`() =
        runTest {
            val legacyTreeIds = listOf(10L, 20L)
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returnsMany listOf(legacyTreeIds, emptyList())
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            syncDataUseCase.execute(Unit)

            coVerify { treeUploader.uploadLegacyTrees(legacyTreeIds, any()) }
        }

    @Test
    fun `WHEN FirebaseInstallations fails THEN uses empty instanceId and continues`() =
        runTest {
            val mockInstallations = mockk<FirebaseInstallations>()
            every { FirebaseInstallations.getInstance() } returns mockInstallations
            every { mockInstallations.id } returns Tasks.forException(RuntimeException("Firebase unavailable"))

            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            val result = syncDataUseCase.execute(Unit)

            assertTrue(result)
            coVerify { planterUploader.upload(any()) }
        }

    @Test
    fun `WHEN sync succeeds THEN tracks each step`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            syncDataUseCase.execute(Unit)

            verify { syncProgressTracker.startStep(SyncStep.MESSAGES) }
            verify { syncProgressTracker.completeStep(SyncStep.MESSAGES) }
            verify { syncProgressTracker.startStep(SyncStep.DEVICE_CONFIG) }
            verify { syncProgressTracker.completeStep(SyncStep.DEVICE_CONFIG) }
            verify { syncProgressTracker.startStep(SyncStep.USERS) }
            verify { syncProgressTracker.completeStep(SyncStep.USERS) }
            verify { syncProgressTracker.startStep(SyncStep.SESSIONS) }
            verify { syncProgressTracker.completeStep(SyncStep.SESSIONS) }
            verify { syncProgressTracker.startStep(SyncStep.LOCATIONS) }
            verify { syncProgressTracker.completeStep(SyncStep.LOCATIONS) }
        }
}