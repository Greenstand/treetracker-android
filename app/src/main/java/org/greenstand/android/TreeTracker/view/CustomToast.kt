package org.greenstand.android.TreeTracker.view

import android.widget.Toast
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication.Companion.appContext

object CustomToast {

    fun showToast(stringRes: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(appContext, stringRes, duration).show()
    }

    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(appContext, message, duration).show()
    }

}