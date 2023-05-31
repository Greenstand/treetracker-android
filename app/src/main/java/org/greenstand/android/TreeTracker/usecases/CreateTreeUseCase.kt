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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.database.app.dao.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.app.entity.TreeEntity
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import timber.log.Timber

class CreateTreeUseCase(
    private val dao: TreeTrackerDAO,
    private val analytics: Analytics,
    private val timeProvider: TimeProvider,
) : UseCase<Tree, Long>() {

    override suspend fun execute(params: Tree): Long = withContext(Dispatchers.IO) {
        val entity = TreeEntity(
            uuid = params.treeUuid.toString(),
            sessionId = params.sessionId,
            photoPath = params.photoPath,
            photoUrl = null,
            note = params.content,
            longitude = params.meanLongitude,
            latitude = params.meanLatitude,
            createdAt = timeProvider.currentTime(),
            extraAttributes = params.treeCaptureAttributes(),
        )

        Timber.d("Inserting TreeCapture entity $entity")
        analytics.treePlanted()
        dao.insertTree(entity)
    }
}