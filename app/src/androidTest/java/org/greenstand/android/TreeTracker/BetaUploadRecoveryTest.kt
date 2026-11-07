/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.models.TreeUploader
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.usecases.CreateTreeRequestUseCase
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.usecases.SyncDataUseCase
import org.greenstand.android.TreeTracker.usecases.UploadImageUseCase
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.UUID

/** Opt-in test: writes synthetic QA data to beta storage. Run on a fresh beta install only. */
@RunWith(AndroidJUnit4::class)
class BetaUploadRecoveryTest {
    @Test
    fun photoCheckpointSurvivesCancellationAndRetry() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("betaUploadRecovery") == "true")
        check(BuildConfig.API_GATEWAY == "https://test-k8s.treetracker.org")
        check(BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES == "treetracker-test-images")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val koin = GlobalContext.get()
        runBlocking(Dispatchers.IO) {
            withTimeout(180_000) {
                val dao = koin.get<TreeTrackerDAO>()
                val users = koin.get<UserRepo>()
                check(users.getUserList().isEmpty()) { "Use a fresh beta install; existing accounts must not be synced by this test" }
                val marker = UUID.randomUUID().toString()
                val wallet = "sdk36-qa-$marker@example.invalid"
                val userId =
                    users.createUser(
                        firstName = "SDK QA",
                        lastName = "Recovery",
                        phone = null,
                        email = wallet,
                        wallet = wallet,
                        photoPath = ImageUtils.createTestImageFile(context).absolutePath,
                        isPowerUser = true,
                    )
                koin.get<DeviceConfigUpdater>().saveLatestConfig()
                koin.get<OrgRepo>().ensureInitialized()
                val sessions = koin.get<SessionTracker>()
                val sessionId = sessions.createSession(userId, null, null).id
                sessions.closeSession(sessionId)
                check(koin.get<SyncDataUseCase>().execute(Unit)) { "Beta prerequisite metadata upload failed" }
                val photo = ImageUtils.createTestImageFile(context)
                val treeId =
                    koin.get<CreateTreeUseCase>().execute(
                        Tree(UUID.randomUUID(), sessionId, "SDK36 interrupted-upload QA $marker", photo.absolutePath, 0.0, 0.0),
                    )
                val checkpoint = CompletableDeferred<Unit>()
                val stoppedDao =
                    object : TreeTrackerDAO by dao {
                        override suspend fun updateTreesBundleIds(
                            ids: List<Long>,
                            bundleId: String,
                        ) {
                            checkpoint.complete(Unit)
                            awaitCancellation()
                        }
                    }

                fun uploader(database: TreeTrackerDAO) =
                    TreeUploader(
                        koin.get<UploadImageUseCase>(),
                        koin.get<ObjectStorageClient>(),
                        koin.get<CreateTreeRequestUseCase>(),
                        database,
                        koin.get<Json>(),
                    )
                val job = launch { uploader(stoppedDao).uploadTrees(listOf(treeId)) }
                try {
                    select<Unit> {
                        checkpoint.onAwait { }
                        job.onJoin { error("Beta photo upload failed before reaching the persisted checkpoint") }
                    }
                } finally {
                    job.cancelAndJoin()
                }
                val interrupted = dao.getTreesByIds(listOf(treeId)).single()
                assertNotNull(interrupted.photoUrl)
                assertFalse(interrupted.uploaded)
                assertTrue(File(interrupted.photoPath!!).exists())
                uploader(dao).uploadTrees(listOf(treeId))
                val resumed = dao.getTreesByIds(listOf(treeId)).single()
                assertTrue("Beta metadata bundle upload must complete", resumed.uploaded)
                assertEquals(interrupted.photoUrl, resumed.photoUrl)
                assertNull(resumed.photoPath)
                assertFalse(photo.exists())
            }
        }
    }
}