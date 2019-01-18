package org.greenstand.android.TreeTracker.application

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

import com.crashlytics.android.Crashlytics

import io.fabric.sdk.android.Fabric
import timber.log.Timber

import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.database.DatabaseManager
import org.greenstand.android.TreeTracker.database.DbHelper
import java.io.IOException


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


    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)

    }

    fun getDatabaseManager(): DatabaseManager{
        val dbHelper = DbHelper.getDbHelper(this)

        try {
            dbHelper!!.createDataBase()
        } catch (e: IOException) {
            // This should be a fatal error
            e.printStackTrace()
        }

        return DatabaseManager(dbHelper)
    }

    companion object {

        private var application: TreeTrackerApplication? = null
        private var databaseManager: DatabaseManager? = null

        fun getDatabaseManager() : DatabaseManager{
            if(databaseManager == null){
                databaseManager = application!!.getDatabaseManager()
                databaseManager!!.openDatabase()
            }
            return databaseManager!!
        }




    }
}
