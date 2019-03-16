package org.greenstand.android.TreeTracker.managers

import android.content.ContentValues
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import timber.log.Timber

object TreeManager {



    fun addAttributes(treeId: Long, attributes: Attributes) {

        val contentValues = ContentValues().apply {
            put(AttributesTable.TREE_ID, treeId)
            put(AttributesTable.HEIGHT_COLOR, attributes.heightColor)
            put(AttributesTable.APP_BUILD, attributes.appBuild)
            put(AttributesTable.APP_FLAVOR, attributes.appFlavor)
        }

        TreeTrackerApplication.getDatabaseManager().insert(table = AttributesTable.NAME,
                                                           contentValues = contentValues)


        Timber.d("Inserted $attributes into Attributes Table")
    }

}

data class Attributes(val heightColor: String,
                      val appFlavor: String,
                      val appBuild: String)


object AttributesTable {
    const val NAME = "attributes"

    const val ID = "_id"
    const val TREE_ID = "tree_id"
    const val HEIGHT_COLOR = "height_color"
    const val APP_FLAVOR = "app_flavor"
    const val APP_BUILD = "app_build"
}