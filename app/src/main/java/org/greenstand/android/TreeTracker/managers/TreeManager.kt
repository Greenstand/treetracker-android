package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.data.TreeAttributes
import org.greenstand.android.TreeTracker.database.entity.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

object TreeManager {

    private val db = TreeTrackerApplication.getAppDatabase()

    suspend fun addAttributes(treeId: Long, attributes: TreeAttributes): Long? {

        val attributesEntity = TreeAttributesEntity(
            treeId = treeId,
            heightColor = attributes.heightColor.name,
            appBuild = attributes.appBuild,
            appVersion = attributes.appVersion,
            flavorId = attributes.appFlavor
        )

        return db.treeAttributesDao().insert(attributesEntity)
            .also { Timber.d("Inserted $attributes into Attributes Table") }
    }

    suspend fun addTree(photoPath: String,
                        minAccuracy: Int,
                        timeToNextUpdate: Int,
                        content: String,
                        userId: Long,
                        planterIdentifierId: Long): Long {

        val locationId = insertLocation(userId)

        val photoId = insertPhoto(locationId, photoPath)

        val settingsId = insertSettings(timeToNextUpdate, minAccuracy)

        val noteId = insertNote(userId, content)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val calendar = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_MONTH, timeToNextUpdate)
        }

        val dateString = dateFormat.format(calendar.time)

        val treeEntity = TreeEntity(
            userId = userId,
            locationId = locationId.toInt(),
            settingId = settingsId,
            planterId = planterIdentifierId,
            timeCreated = dateString,
            timeForUpdate = dateString,
            timeUpdated = dateString,
            causeOfDeath = null,
            isSynced = false,
            isMissing = false,
            isPriority = false,
            settingsOverrideId = null,
            mainDbId = 0,
            threeDigitNumber = null
        )

        val treeId = db.treeDao().insert(treeEntity)
        Timber.d("treeId $treeId")

        insertTreePhoto(treeId, photoId)

        insertTreeNote(treeId, noteId)

        return treeId
    }

    fun insertLocation(userId: Long): Long {
        val locationEntity = LocationEntity(
            MainActivity.mCurrentLocation!!.accuracy.toInt(),
            MainActivity.mCurrentLocation!!.latitude,
            MainActivity.mCurrentLocation!!.longitude,
            userId
        )

        return db.locationDao().insert(locationEntity)
            .also { Timber.d("locationId $it") }
    }

    fun insertTreeNote(treeId: Long, noteId: Long): Long {
        return db.noteDao().insert(TreeNoteEntity(treeId, noteId))
            .also { Timber.d("treeNoteId $it") }
    }

    fun insertTreePhoto(treeId: Long, photoId: Long): Long {
        return db.photoDao().insert(TreePhotoEntity(treeId, photoId))
            .also { Timber.d("treePhotoId $it") }
    }

    fun insertPhoto(locationId: Long, photoPath: String): Long {
        val photoEntity = PhotoEntity(
            locationId = locationId.toInt(),
            name = photoPath,
            timeTaken = "",
            userId = 0
        )
        return db.photoDao().insertPhoto(photoEntity)
            .also { Timber.d("photoId $it") }
    }

    fun insertSettings(timeToNextUpdate: Int, minAccuracy: Int): Long {
        val settingsEntity = SettingsEntity(timeToNextUpdate = timeToNextUpdate,
                                            minAccuracy = minAccuracy)

        return db.settingsDao().insert(settingsEntity)
            .also { Timber.d("settingsId $it") }
    }

    fun insertNote(userId: Long, content: String): Long {
        val noteEntity = NoteEntity(userId = userId,
                                    content = content,
                                    timeCreated = "")

        return db.noteDao().insert(noteEntity)
            .also { Timber.d("noteId $it") }
    }
}