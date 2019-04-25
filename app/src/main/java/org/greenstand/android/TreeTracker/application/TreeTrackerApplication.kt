package org.greenstand.android.TreeTracker.application

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import timber.log.Timber


class TreeTrackerApplication : Application() {

    override fun onCreate() {
        application = this
        appContext = applicationContext
        // The following line triggers the initialization of ACRA
        super.onCreate()
        if (FeatureFlags.FABRIC_ENABLED) {
            Fabric.with(this, Crashlytics())
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            Timber.tag("DebugDB").d("To forward DebugDB from emulator to browser use the command 'adb forward tcp:8080 tcp:8080' from terminal")
            Timber.tag("DebugDB").d("For more information visit: https://github.com/amitshekhariitbhu/Android-Debug-Database")
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {

        lateinit var appContext: Context
        private var application: TreeTrackerApplication? = null

        fun getAppDatabase(): AppDatabase {
            return AppDatabase.getInstance(application!!.applicationContext)
        }

    }
}
