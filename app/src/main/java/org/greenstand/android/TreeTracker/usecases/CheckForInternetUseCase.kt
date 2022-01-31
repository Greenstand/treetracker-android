package org.greenstand.android.TreeTracker.usecases

import java.lang.Exception

class CheckForInternetUseCase {

    /**
     *  checks if the internet is available on the user's device
     */
    fun isInternetServiceAvailable(): Boolean = run {
        try {
            val command = "ping -c 1 google.com"
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}