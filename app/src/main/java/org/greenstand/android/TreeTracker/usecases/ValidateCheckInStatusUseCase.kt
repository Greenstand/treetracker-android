package org.greenstand.android.TreeTracker.usecases

import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.User

class ValidateCheckInStatusUseCase constructor(private val user: User) : UseCase<Unit, Boolean>() {

    override suspend fun execute(params: Unit): Boolean {
        val currentTimestampSeconds = System.currentTimeMillis() / 1000

        val lastTimeStamp = user.lastCheckInTimeInSeconds ?: 0

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
