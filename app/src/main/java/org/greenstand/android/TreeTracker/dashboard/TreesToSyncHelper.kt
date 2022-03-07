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