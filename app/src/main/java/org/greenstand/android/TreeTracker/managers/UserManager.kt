package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.io.IOException

object UserManager {


    val isLoggedIn: Boolean
        get() = Api.authToken != null

    val userId: Long
        get() = TreeTrackerApplication.appContext.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE).getLong(ValueHelper.MAIN_USER_ID, -1)

    suspend fun authenticateDevice(): Boolean {

        try {
            Api.authenticate(DeviceUtils.deviceId)
        } catch (e: IOException) {
            return false
        }

        try {
            Api.updateDevice()
        } catch (e: IOException) {
            return false
        }

        return true
    }

}