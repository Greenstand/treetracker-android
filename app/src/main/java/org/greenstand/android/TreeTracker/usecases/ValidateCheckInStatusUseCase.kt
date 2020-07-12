package org.greenstand.android.TreeTracker.usecases

import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.utilities.ValueHelper

class ValidateCheckInStatusUseCase constructor(private val sharedPreferences: SharedPreferences): UseCase<Unit, Boolean>() {

    override suspend fun execute(params: Unit): Boolean {
        val currentTimestampSeconds = System.currentTimeMillis() / 1000

        val lastTimeStamp = sharedPreferences.getLong(ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS, 0)

        val timeSinceLastCheckIn = currentTimestampSeconds - lastTimeStamp

        if (FeatureFlags.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED && timeSinceLastCheckIn > CHECK_IN_TIMEOUT) {
            return false
        }
        return true
    }

    companion object {
        private const val CHECK_IN_TIMEOUT = 60 * 60 * 24 * 14 // 2 weeks
    }

}