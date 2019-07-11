package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.database.v2.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.v2.entity.TreeCaptureEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


data class CreateTreeParams(val photoPath: String,
                            val content: String,
                            val planterCheckInId: Long)


class CreateTreeUseCase(private val sharedPreferences: SharedPreferences,
                        private val userLocationManager: UserLocationManager,
                        private val dao: TreeTrackerDAO) : UseCase<CreateTreeParams, Long>() {

    override suspend fun execute(params: CreateTreeParams): Long = withContext(Dispatchers.IO) {
        val uuid = UUID.randomUUID()

        val location = userLocationManager.currentLocation
        val time = location?.time ?: System.currentTimeMillis()

        val entity = TreeCaptureEntity(
            uuid = uuid.toString(),
            planterCheckInId = params.planterCheckInId,
            localPhotoPath = params.photoPath,
            photoUrl = null,
            noteContent = params.content,
            longitude = location?.longitude ?: 0.0,
            latitude = location?.latitude ?: 0.0,
            accuracy = sharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING).toDouble(),
            createAt = time
        )

        dao.insertTreeCapture(entity)
    }

}