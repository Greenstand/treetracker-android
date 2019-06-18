package org.greenstand.android.TreeTracker.managers

import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.database.entity.*
import timber.log.Timber
import java.util.*

class TreeManager(private val db: AppDatabase,
                  private val userLocationManager: UserLocationManager) {

    companion object {
        const val TREE_COLOR_ATTR_KEY = "height_color"
        const val APP_BUILD_ATTR_KEY = "app_build"
        const val APP_FLAVOR_ATTR_KEY = "app_flavor"
        const val APP_VERSION_ATTR_KEY = "app_version"
    }


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

    fun getTreeAttributes(treeId: Long): List<TreeAttributesEntity> {

        return db.treeAttributesDao().getTreeAttributesByTree(treeId);
    }

    fun addNewTree(uuid: UUID,
                   userId: Long,
                   locationId: Long,
                   settingsId: Long,
                   planterId: Long,
                   timeCreated: String,
                   timeForUpdate: String,
                   timeUpdated: String): Long {

        val treeEntity = TreeEntity(
            uuid = uuid.toString(),
            userId = userId,
            locationId = locationId.toInt(),
            settingId = settingsId,
            planterId = planterId,
            timeCreated = timeCreated,
            timeForUpdate = timeForUpdate,
            timeUpdated = timeUpdated,
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

        return treeId
    }

    fun insertLocation(userId: Long): Long {
        val locationEntity = LocationEntity(
            userLocationManager.currentLocation!!.accuracy.toInt(),
            userLocationManager.currentLocation!!.latitude,
            userLocationManager.currentLocation!!.longitude,
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