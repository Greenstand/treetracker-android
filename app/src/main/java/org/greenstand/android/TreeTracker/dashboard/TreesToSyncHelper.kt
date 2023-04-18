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

import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

class TreesToSyncHelper(
    private val preferences: Preferences,
    private val dao: TreeTrackerDAO,
) {

    suspend fun refreshTreeCountToSync() {
        val treesToSync = dao.getNonUploadedLegacyTreeCaptureImageCount() + dao.getNonUploadedTreeImageCount()
        preferences.edit().putInt(TREES_TO_SYNC_KEY, treesToSync).commit()
    }

    fun getTreeCountToSync(): Int = preferences.getInt(TREES_TO_SYNC_KEY, -1)

    companion object {
        val TREES_TO_SYNC_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("tree-count-to-sync")
    }

}