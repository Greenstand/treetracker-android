package org.greenstand.android.TreeTracker.application

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex

import com.crashlytics.android.Crashlytics

import org.greenstand.android.TreeTracker.BuildConfig

import io.fabric.sdk.android.Fabric
import timber.log.Timber

class TreeTrackerApplication : Application() {
    override fun onCreate() {
        // The following line triggers the initialization of ACRA
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)

    }
}
