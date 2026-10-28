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
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.DeviceConfigUploader
import org.greenstand.android.TreeTracker.models.PlanterUploader
import org.greenstand.android.TreeTracker.models.SessionUploader
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.overlay.SyncProgressTracker
import org.greenstand.android.TreeTracker.overlay.StepStatus
import org.greenstand.android.TreeTracker.overlay.SyncStep
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun `WHEN all steps succeed THEN runs the sync steps in the documented order`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returnsMany listOf(listOf(10L), emptyList())
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L), emptyList())

            syncDataUseCase.execute(Unit)

            coVerifyOrder {
                messagesRepo.syncMessages()
                deviceConfigUploader.upload("test-instance-id")
                planterUploader.upload("test-instance-id")
                sessionUploader.upload()
                treeUploader.uploadLegacyTrees(listOf(10L), "test-instance-id")
                treeUploader.uploadTrees(listOf(1L))
                uploadLocationDataUseCase.execute(Unit)
            }
        }

    @Test
    fun `WHEN all steps succeed THEN tracks steps in the documented order`() =
        runTest {
            stubNoTrees()

            syncDataUseCase.execute(Unit)

            verifyOrder {
                syncProgressTracker.startSync()
                syncProgressTracker.startStep(SyncStep.MESSAGES)
                syncProgressTracker.completeStep(SyncStep.MESSAGES)
                syncProgressTracker.startStep(SyncStep.DEVICE_CONFIG)
                syncProgressTracker.completeStep(SyncStep.DEVICE_CONFIG)
                syncProgressTracker.startStep(SyncStep.USERS)
                syncProgressTracker.completeStep(SyncStep.USERS)
                syncProgressTracker.startStep(SyncStep.SESSIONS)
                syncProgressTracker.completeStep(SyncStep.SESSIONS)
                syncProgressTracker.startStep(SyncStep.LEGACY_TREES)
                syncProgressTracker.completeStep(SyncStep.LEGACY_TREES)
                syncProgressTracker.startStep(SyncStep.TREES)
                syncProgressTracker.completeStep(SyncStep.TREES)
                syncProgressTracker.startStep(SyncStep.LOCATIONS)
                syncProgressTracker.completeStep(SyncStep.LOCATIONS)
                syncProgressTracker.endSync()
            }
        }

    @Test
    fun `WHEN FirebaseInstallations fails THEN passes an empty instance id to every uploader`() =
        runTest {
            val mockInstallations = mockk<FirebaseInstallations>()
            every { FirebaseInstallations.getInstance() } returns mockInstallations
            every { mockInstallations.id } returns Tasks.forException(RuntimeException("Firebase unavailable"))
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returnsMany listOf(listOf(10L), emptyList())
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            syncDataUseCase.execute(Unit)

            coVerify { deviceConfigUploader.upload("") }
            coVerify { planterUploader.upload("") }
            coVerify { treeUploader.uploadLegacyTrees(listOf(10L), "") }
        }

    @Test
    fun `WHEN device config upload fails THEN stops before uploading users and reports the error`() =
        runTest {
            coEvery { deviceConfigUploader.upload(any()) } throws RuntimeException("config failed")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.DEVICE_CONFIG, "config failed") }
            verify { syncProgressTracker.endSync(error = "config failed") }
            verify(exactly = 0) { syncProgressTracker.startStep(SyncStep.USERS) }
            coVerify(exactly = 0) { planterUploader.upload(any()) }
        }

    @Test
    fun `WHEN user upload fails THEN sessions and trees are not uploaded`() =
        runTest {
            coEvery { planterUploader.upload(any()) } throws RuntimeException("users failed")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.USERS, "users failed") }
            coVerify(exactly = 0) { sessionUploader.upload() }
            coVerify(exactly = 0) { treeUploader.uploadLegacyTrees(any(), any()) }
            coVerify(exactly = 0) { treeUploader.uploadTrees(any()) }
        }

    @Test
    fun `WHEN session upload fails THEN trees and locations are not uploaded`() =
        runTest {
            coEvery { sessionUploader.upload() } throws RuntimeException("sessions failed")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.SESSIONS, "sessions failed") }
            coVerify(exactly = 0) { dao.getAllTreeCaptureIdsToUpload() }
            coVerify(exactly = 0) { uploadLocationDataUseCase.execute(any()) }
        }

    @Test
    fun `WHEN legacy tree upload fails THEN new trees are not uploaded`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns listOf(10L)
            coEvery { dao.getAllTreeIdsToUpload() } returns listOf(1L)
            coEvery { treeUploader.uploadLegacyTrees(any(), any()) } throws RuntimeException("legacy failed")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.LEGACY_TREES, "legacy failed") }
            coVerify(exactly = 0) { treeUploader.uploadTrees(any()) }
            coVerify(exactly = 0) { uploadLocationDataUseCase.execute(any()) }
        }

    @Test
    fun `WHEN tree upload fails THEN marks the trees step as failed and skips locations`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returns listOf(1L)
            coEvery { treeUploader.uploadTrees(any()) } throws RuntimeException("trees failed")

            syncDataUseCase.execute(Unit)

            verify { syncProgressTracker.failStep(SyncStep.TREES, "trees failed") }
            verify(exactly = 0) { syncProgressTracker.completeStep(SyncStep.TREES) }
            coVerify(exactly = 0) { uploadLocationDataUseCase.execute(any()) }
        }

    @Test
    fun `WHEN location upload throws THEN returns false`() =
        runTest {
            stubNoTrees()
            coEvery { uploadLocationDataUseCase.execute(Unit) } throws RuntimeException("locations failed")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.LOCATIONS, "locations failed") }
        }

    @Test
    fun `WHEN location upload reports failure without throwing THEN sync still succeeds`() =
        runTest {
            stubNoTrees()
            coEvery { uploadLocationDataUseCase.execute(Unit) } returns false

            val result = syncDataUseCase.execute(Unit)

            assertTrue(result)
            verify { syncProgressTracker.completeStep(SyncStep.LOCATIONS) }
        }

    @Test
    fun `WHEN new trees appear after an upload pass THEN uploads them in another pass`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L, 2L, 3L), listOf(4L), emptyList())

            val result = syncDataUseCase.execute(Unit)

            assertTrue(result)
            coVerifyOrder {
                treeUploader.uploadTrees(listOf(1L, 2L, 3L))
                treeUploader.uploadTrees(listOf(4L))
            }
            verifyOrder {
                syncProgressTracker.updateStepProgress(SyncStep.TREES, 0, 3)
                syncProgressTracker.updateStepProgress(SyncStep.TREES, 3, 3)
                syncProgressTracker.updateStepProgress(SyncStep.TREES, 4, 3)
                syncProgressTracker.completeStep(SyncStep.TREES)
            }
        }

    @Test
    fun `WHEN some trees remain after an upload pass THEN stops retrying and still completes`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L, 2L, 3L), listOf(2L))

            val result = syncDataUseCase.execute(Unit)

            assertTrue(result)
            coVerify(exactly = 1) { treeUploader.uploadTrees(any()) }
            verify { syncProgressTracker.completeStep(SyncStep.TREES) }
            coVerify { uploadLocationDataUseCase.execute(Unit) }
        }

    @Test
    fun `WHEN no trees upload at all THEN tries each batch only once`() =
        runTest {
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns listOf(10L, 20L)
            coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()

            val result = syncDataUseCase.execute(Unit)

            assertTrue(result)
            coVerify(exactly = 1) { treeUploader.uploadLegacyTrees(listOf(10L, 20L), any()) }
            verify { syncProgressTracker.updateStepProgress(SyncStep.LEGACY_TREES, 2, 2) }
        }

    @Test
    fun `WHEN there are no trees THEN reports zero progress and never calls the tree uploader`() =
        runTest {
            stubNoTrees()

            syncDataUseCase.execute(Unit)

            verify { syncProgressTracker.updateStepProgress(SyncStep.LEGACY_TREES, 0, 0) }
            verify { syncProgressTracker.updateStepProgress(SyncStep.TREES, 0, 0) }
            coVerify(exactly = 0) { treeUploader.uploadLegacyTrees(any(), any()) }
            coVerify(exactly = 0) { treeUploader.uploadTrees(any()) }
        }

    @Test
    fun `WHEN a step throws CancellationException THEN sync ends with an error`() =
        runTest {
            coEvery { messagesRepo.syncMessages() } throws CancellationException("cancelled")

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            verify { syncProgressTracker.failStep(SyncStep.MESSAGES, "cancelled") }
            verify { syncProgressTracker.endSync(error = "cancelled") }
            coVerify(exactly = 0) { deviceConfigUploader.upload(any()) }
        }

    @Test
    fun `WHEN sync coroutine is cancelled during a step THEN remaining uploads are skipped`() =
        runTest {
            stubNoTrees()
            coEvery { deviceConfigUploader.upload(any()) } coAnswers { currentCoroutineContext().cancel() }

            val result = syncDataUseCase.execute(Unit)

            assertFalse(result)
            coVerify(exactly = 0) { planterUploader.upload(any()) }
            coVerify(exactly = 0) { sessionUploader.upload() }
            coVerify(exactly = 0) { uploadLocationDataUseCase.execute(any()) }
            verify(exactly = 0) { syncProgressTracker.endSync(null) }
        }

    @Test
    fun `WHEN sync succeeds with a real tracker THEN every step ends successful with tree counts`() =
        runTest {
            val tracker = SyncProgressTracker()
            coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
            coEvery { dao.getAllTreeIdsToUpload() } returnsMany listOf(listOf(1L, 2L), emptyList())

            val result = createUseCase(tracker).execute(Unit)

            assertTrue(result)
            val state = tracker.state.value
            assertFalse(state.isActive)
            assertNull(state.currentStep)
            assertNull(state.overallError)
            assertTrue(state.steps.all { it.status == StepStatus.SUCCESS })
            val treesStep = state.steps.first { it.step == SyncStep.TREES }
            assertEquals(2, treesStep.itemsTotal)
            assertEquals(2, treesStep.itemsCompleted)
        }

    @Test
    fun `WHEN sync fails with a real tracker THEN state shows the failed step and pending later steps`() =
        runTest {
            val tracker = SyncProgressTracker()
            coEvery { sessionUploader.upload() } throws RuntimeException("sessions failed")

            val result = createUseCase(tracker).execute(Unit)

            assertFalse(result)
            val state = tracker.state.value
            val statuses = state.steps.associate { it.step to it.status }
            assertFalse(state.isActive)
            assertEquals("sessions failed", state.overallError)
            assertEquals(StepStatus.SUCCESS, statuses[SyncStep.MESSAGES])
            assertEquals(StepStatus.SUCCESS, statuses[SyncStep.DEVICE_CONFIG])
            assertEquals(StepStatus.SUCCESS, statuses[SyncStep.USERS])
            assertEquals(StepStatus.ERROR, statuses[SyncStep.SESSIONS])
            assertEquals(StepStatus.PENDING, statuses[SyncStep.LEGACY_TREES])
            assertEquals(StepStatus.PENDING, statuses[SyncStep.TREES])
            assertEquals(StepStatus.PENDING, statuses[SyncStep.LOCATIONS])
            assertEquals("sessions failed", state.steps.first { it.step == SyncStep.SESSIONS }.errorMessage)
        }

    private fun stubNoTrees() {
        coEvery { dao.getAllTreeCaptureIdsToUpload() } returns emptyList()
        coEvery { dao.getAllTreeIdsToUpload() } returns emptyList()
    }

    private fun createUseCase(tracker: SyncProgressTracker): SyncDataUseCase =
        SyncDataUseCase(
            treeUploader = treeUploader,
            uploadLocationDataUseCase = uploadLocationDataUseCase,
            dao = dao,
            planterUploader = planterUploader,
            sessionUploader = sessionUploader,
            deviceConfigUploader = deviceConfigUploader,
            messagesRepo = messagesRepo,
            syncProgressTracker = tracker,
        )
}