package org.greenstand.android.TreeTracker.managers

import android.content.ContentValues
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.data.TreeAttributes
import org.greenstand.android.TreeTracker.database.AttributesTable
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

object TreeManager {

    suspend fun addAttributes(treeId: Long, attributes: TreeAttributes): Long? {

        val contentValues = ContentValues().apply {
            put(AttributesTable.TREE_ID, treeId)
            put(AttributesTable.HEIGHT_COLOR, attributes.heightColor.name)
            put(AttributesTable.APP_BUILD, attributes.appBuild)
            put(AttributesTable.APP_FLAVOR, attributes.appFlavor)
        }

        return TreeTrackerApplication.getDatabaseManager().insert(table = AttributesTable.NAME,
                                                                  contentValues = contentValues)
            .also { Timber.d("Inserted $attributes into Attributes Table") }
    }

    suspend fun addTree(photoPath: String,
                        minAccuracy: Int,
                        timeToNextUpdate: Int,
                        content: String,
                        userId: Long,
                        planterIdentifierId: Long): Long? {

        val locationId = insertLocation()!!

        val photoId = insertPhoto(locationId, photoPath)!!

        val settingsId = insertSettings(timeToNextUpdate, minAccuracy)!!

        val noteId = insertNote(userId, content)!!

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val calendar = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_MONTH, timeToNextUpdate)
        }
        val date: Date = calendar.time

        // tree
        val treeContentValues = ContentValues().apply {
            put("user_id", userId)
            put("location_id", locationId)
            put("settings_id", settingsId)
            put("planter_identification_id", planterIdentifierId)
            put("time_created", dateFormat.format(Date()))
            put("time_updated", dateFormat.format(Date()))
            put("time_for_update", dateFormat.format(date))
        }

        val treeId = TreeTrackerApplication.getDatabaseManager().insert("tree", contentValues = treeContentValues)
        Timber.d("treeId $treeId")

        insertTreePhoto(treeId, photoId)

        insertTreeNote(treeId, noteId)

        return treeId
    }

    fun insertLocation(): Long? {
        val locationContentValues = ContentValues().apply {
            put("accuracy", MainActivity.mCurrentLocation!!.accuracy.toString())
            put("lat", MainActivity.mCurrentLocation!!.latitude.toString())
            put("long", MainActivity.mCurrentLocation!!.longitude.toString())
        }

        return TreeTrackerApplication.getDatabaseManager().insert("location", contentValues = locationContentValues)
            .also { Timber.d("locationId $it") }
    }

    fun insertTreeNote(treeId: Long, noteId: Long): Long? {
        val treeNoteContentValues = ContentValues().apply {
            put("tree_id", treeId)
            put("note_id", noteId)
        }

        return TreeTrackerApplication.getDatabaseManager().insert("tree_note", null, treeNoteContentValues)
            .also { Timber.d("treeNoteId $it") }
    }

    fun insertTreePhoto(treeId: Long, photoId: Long): Long? {
        val treePhotoContentValues = ContentValues().apply {
            put("tree_id", treeId)
            put("photo_id", photoId)
        }
        return TreeTrackerApplication.getDatabaseManager().insert("tree_photo", contentValues = treePhotoContentValues)
            .also { Timber.d("treePhotoId $it") }
    }

    fun insertPhoto(locationId: Long, photoPath: String): Long? {
        val photoContentValues = ContentValues().apply {
            put("location_id", locationId)
            put("name", photoPath)
        }

        return TreeTrackerApplication.getDatabaseManager().insert("photo", contentValues = photoContentValues)
            .also { Timber.d("photoId $it") }
    }

    fun insertSettings(timeToNextUpdate: Int, minAccuracy: Int): Long? {
        val settingsContentValues = ContentValues().apply {
            put("time_to_next_update", timeToNextUpdate)
            put("min_accuracy", minAccuracy)
        }

        return TreeTrackerApplication.getDatabaseManager().insert("settings", contentValues = settingsContentValues)
            .also { Timber.d("settingsId $it") }
    }

    fun insertNote(userId: Long, content: String): Long? {
        val noteContentValues = ContentValues().apply {
            put("user_id", userId)
            put("content", content)
        }

        return TreeTrackerApplication.getDatabaseManager().insert("note", contentValues = noteContentValues)
            .also { Timber.d("noteId $it") }
    }
}