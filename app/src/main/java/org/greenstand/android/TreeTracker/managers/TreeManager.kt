package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.entity.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

object TreeManager {


    const val TREE_COLOR_ATTR_KEY = "height_color"
    const val APP_BUILD_ATTR_KEY = "app_build"
    const val APP_FLAVOR_ATTR_KEY = "app_flavor"
    const val APP_VERSION_ATTR_KEY = "app_version"

    private val db = TreeTrackerApplication.getAppDatabase()

    fun addTreeAttribute(treeId: Long,
                         key: String,
                         value: String): Long {

        val attribute = TreeAttributesEntity(key = key,
                                             value = value,
                                             treeId = treeId)

        return db.treeAttributesDao().insert(attribute)
            .also { Timber.d("Inserted $attribute into Attributes Table") }
    }

    fun getTreeAttribute(treeId: Long,
                         key: String): String? {

        return db.treeAttributesDao().getTreeAttributeByTreeAndKey(treeId, key)?.value
    }

    suspend fun addTree(photoPath: String,
                        minAccuracy: Int,
                        timeToNextUpdate: Int,
                        content: String,
                        userId: Long,
                        planterIdentifierId: Long): Long {

        val uuid = UUID.randomUUID()

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
            uuid = uuid.toString(),
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