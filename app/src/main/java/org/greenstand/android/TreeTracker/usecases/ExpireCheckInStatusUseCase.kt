package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.utilities.ValueHelper

class ExpireCheckInStatusUseCase constructor(
    private val sharedPreferences: SharedPreferences
) : UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit) {
        sharedPreferences.edit()
            .putLong(ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS, 0)
            .putLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)
            .putLong(ValueHelper.PLANTER_INFO_ID, -1)
            .putString(ValueHelper.PLANTER_PHOTO, null)
            .apply()
    }

    fun readyForAutomaticSignout(): Boolean {
        val currentTimestamp = System.currentTimeMillis() / 1000
        val timeStampForLastCheckIn = sharedPreferences.getLong(
            ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS,
            0
        )
        return (FeatureFlags.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED &&
                currentTimestamp - timeStampForLastCheckIn > ValueHelper.CHECK_IN_TIMEOUT)
    }
}
