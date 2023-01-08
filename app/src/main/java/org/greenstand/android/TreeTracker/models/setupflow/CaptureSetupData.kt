package org.greenstand.android.TreeTracker.models.setupflow

import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.user.User

class CaptureSetupData(private val exceptionDataCollector: ExceptionDataCollector) {

    var user: User? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.USER_WALLET, value?.wallet)
            field = value
        }

    var destinationWallet: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.DESTINATION_WALLET, value)
            field = value
        }

    var sessionNote: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.SESSION_NOTE, value)
            field = value
        }

    var organizationName: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.ORG_NAME, value)
            field = value
        }
}