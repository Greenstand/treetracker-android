package org.greenstand.android.TreeTracker.analytics

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class ExceptionLogger : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }
        val error = throwable ?: Exception(message)

        Firebase.crashlytics.recordException(error)
    }
}