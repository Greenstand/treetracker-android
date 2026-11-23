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
package org.greenstand.android.TreeTracker.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber
import java.util.UUID

class PendingTreeStore(
    private val preferences: Preferences,
) {
    fun save(tree: Tree) {
        preferences.edit().putString(PENDING_TREE_KEY, json.encodeToString(PendingTree.serializer(), tree.toPending())).apply()
    }

    fun load(): Tree? {
        val value = preferences.getString(PENDING_TREE_KEY) ?: return null
        return try {
            json.decodeFromString(PendingTree.serializer(), value).toTree()
        } catch (e: Exception) {
            Timber.tag("PendingTreeStore").e(e, "Could not read the pending tree, dropping it")
            clear()
            null
        }
    }

    fun clear() {
        preferences.edit().remove(PENDING_TREE_KEY).apply()
    }

    @Serializable
    private data class PendingTree(
        val treeUuid: String,
        val sessionId: Long,
        val content: String,
        val photoPath: String,
        val meanLongitude: Double,
        val meanLatitude: Double,
        val attributes: Map<String, String>,
    ) {
        fun toTree(): Tree =
            Tree(
                treeUuid = UUID.fromString(treeUuid),
                sessionId = sessionId,
                content = content,
                photoPath = photoPath,
                meanLongitude = meanLongitude,
                meanLatitude = meanLatitude,
                treeAttributes = attributes.toMutableMap(),
            )
    }

    private fun Tree.toPending(): PendingTree =
        PendingTree(
            treeUuid = treeUuid.toString(),
            sessionId = sessionId,
            content = content,
            photoPath = photoPath,
            meanLongitude = meanLongitude,
            meanLatitude = meanLatitude,
            attributes = treeCaptureAttributes().toMap(),
        )

    companion object {
        val PENDING_TREE_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("pending-capture-tree")
        private val json = Json { ignoreUnknownKeys = true }
    }
}