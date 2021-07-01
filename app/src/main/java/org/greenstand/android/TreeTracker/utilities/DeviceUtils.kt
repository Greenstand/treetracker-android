package org.greenstand.android.TreeTracker.utilities

import android.provider.Settings
import java.util.*
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication

object DeviceUtils {

    val deviceId: String
        get() = Settings.Secure.getString(TreeTrackerApplication.appContext.contentResolver, Settings.Secure.ANDROID_ID)

    val language: String
        get() = Locale.getDefault().displayLanguage
}
