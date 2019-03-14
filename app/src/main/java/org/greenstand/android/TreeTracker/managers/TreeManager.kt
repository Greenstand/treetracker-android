package org.greenstand.android.TreeTracker.managers

import android.content.ContentValues
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import timber.log.Timber

object TreeManager {

    private object TreeHeightTable {
        const val NAME = "tree_height"

        const val COL_HEIGHT = "tree_height"
        const val COL_TREE_ID = "tree_id"
    }

    fun addTreeHeight(treeId: String, height: Int) {

        val contentValues = ContentValues().apply {
            put(TreeHeightTable.COL_TREE_ID, treeId)
            put(TreeHeightTable.COL_HEIGHT, height)
        }

        TreeTrackerApplication.getDatabaseManager().insert(table = TreeHeightTable.NAME,
                                                           contentValues = contentValues)

        Timber.d("Inserted height $height for tree id $treeId")
    }

}