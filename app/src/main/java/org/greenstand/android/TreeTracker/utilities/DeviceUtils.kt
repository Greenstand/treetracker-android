package org.greenstand.android.TreeTracker.utilities

import android.app.Application
import android.content.Context
import android.provider.Settings
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication

object DeviceUtils {

    val deviceId: String
        get() = Settings.Secure.getString(TreeTrackerApplication.appContext.contentResolver, Settings.Secure.ANDROID_ID)

}