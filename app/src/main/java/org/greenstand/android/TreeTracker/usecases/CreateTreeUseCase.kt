package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


data class CreateTreeParams(val photoPath: String,
                            val content: String,
                            val userId: Long,
                            val planterIdentifierId: Long)


class CreateTreeUseCase(private val treeManager: TreeManager,
                        private val sharedPreferences: SharedPreferences,
                        private val userLocationManager: UserLocationManager) : UseCase<CreateTreeParams, Long>() {

    override suspend fun execute(params: CreateTreeParams): Long = withContext(Dispatchers.IO) {
        val uuid = UUID.randomUUID()

        val locationId = treeManager.insertLocation(params.userId)

        val photoId = treeManager.insertPhoto(locationId, params.photoPath)

        val minAccuracy: Int = sharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)

        val settingsId = treeManager.insertSettings(0, minAccuracy)

        val noteId = treeManager.insertNote(params.userId, params.content)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date(userLocationManager.currentLocation?.time!!)
        val treeCreationDate = dateFormat.format(date)

        Timber.d("Creation Time Stamp: $treeCreationDate")

        val treeId = treeManager.addNewTree(uuid,
                                            params.userId,
                                            locationId,
                                            settingsId,
                                            params.planterIdentifierId,
                                            treeCreationDate,
                                            treeCreationDate,
                                            treeCreationDate)

        Timber.d("treeId $treeId")

        treeManager.insertTreePhoto(treeId, photoId)

        treeManager.insertTreeNote(treeId, noteId)

        treeId
    }

}