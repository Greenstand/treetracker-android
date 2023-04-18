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
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeAttributeEntity
import org.greenstand.android.TreeTracker.database.legacy.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.models.Tree
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import timber.log.Timber

data class CreateLegacyTreeParams(
    val planterCheckInId: Long,
    val tree: Tree,
)

class CreateLegacyTreeUseCase(
    private val dao: TreeTrackerDAO,
    private val timeProvider: TimeProvider,
) : UseCase<CreateLegacyTreeParams, Long>() {

    override suspend fun execute(params: CreateLegacyTreeParams): Long = withContext(Dispatchers.IO) {
        val entity = TreeCaptureEntity(
            uuid = params.tree.treeUuid.toString(),
            planterCheckInId = params.planterCheckInId,
            localPhotoPath = params.tree.photoPath,
            photoUrl = null,
            noteContent = params.tree.content,
            longitude = params.tree.meanLongitude,
            latitude = params.tree.meanLatitude,
            accuracy = 0.0, // accuracy is a legacy remnant and not used. Pending table cleanup
            createAt = timeProvider.currentTime().epochSeconds, // legacy bulk pack uses seconds, not milliseconds
        )
        val attributeEntitites = params.tree.treeCaptureAttributes().map {
            TreeAttributeEntity(it.key, it.value, -1)
        }.toList()
        Timber.d("Inserting TreeCapture entity $entity")
        dao.insertTreeWithAttributes(entity, attributeEntitites)

    }
}