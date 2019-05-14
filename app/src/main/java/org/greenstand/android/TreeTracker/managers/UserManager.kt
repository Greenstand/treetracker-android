package org.greenstand.android.TreeTracker.managers

import android.content.Context
import org.greenstand.android.TreeTracker.api.RetrofitApi
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.DeviceUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import java.io.IOException

class UserManager(private val context: Context) {

    var authToken: String? = null

    val isLoggedIn: Boolean
        get() = authToken != null

    val userId: Long
        get() = context.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE).getLong(ValueHelper.MAIN_USER_ID, -1)

}