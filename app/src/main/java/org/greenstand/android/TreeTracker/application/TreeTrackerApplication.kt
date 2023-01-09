package org.greenstand.android.TreeTracker.application

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.analytics.ExceptionLogger
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.di.appModule
import org.greenstand.android.TreeTracker.di.networkModule
import org.greenstand.android.TreeTracker.di.roomModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class TreeTrackerApplication : Application() {

    override fun onCreate() {
        appContext = applicationContext

        super.onCreate()

        ObjectStorageClient.init(applicationContext)

        startKoin {
            androidLogger()
            androidContext(applicationContext)
            modules(
                appModule,
                roomModule,
                networkModule,
            )
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            Timber.tag("DebugDB").d("To forward DebugDB from emulator to browser " +
                    "use the command 'adb forward tcp:8080 tcp:8080' from terminal")
            Timber.tag("DebugDB").d("For more information visit: " +
                    "https://github.com/amitshekhariitbhu/Android-Debug-Database")
        } else {
            Timber.plant(ExceptionLogger())
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    companion object {
        lateinit var appContext: Context
    }
}
