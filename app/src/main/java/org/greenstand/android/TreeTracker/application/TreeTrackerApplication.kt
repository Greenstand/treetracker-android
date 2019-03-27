package org.greenstand.android.TreeTracker.application

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.database.AppDatabase
import timber.log.Timber


class TreeTrackerApplication : Application() {

    override fun onCreate() {
        application = this

        // The following line triggers the initialization of ACRA
        super.onCreate()
        if (BuildConfig.ENABLE_FABRIC == "true") {
            Fabric.with(this, Crashlytics())
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        if (BuildConfig.DEBUG) {
            try {
                val debugDB = Class.forName("com.amitshekhar.DebugDB")
                val getAddressLog = debugDB.getMethod("getAddressLog")
                val value = getAddressLog.invoke(null)
                Timber.tag(TreeTrackerApplication::class.toString()).d("you can see DB contents at %s", value)
            } catch (e: Exception) {
                //e.printStackTrace();
            }
        }

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)

    }

    companion object {

        private var application: TreeTrackerApplication? = null

        fun getAppDatabase(): AppDatabase {
            return AppDatabase.getInstance(application!!.applicationContext)
        }

    }
}
