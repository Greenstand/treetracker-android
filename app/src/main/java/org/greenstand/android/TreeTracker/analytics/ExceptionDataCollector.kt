package org.greenstand.android.TreeTracker.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

class ExceptionDataCollector(
    private val firebaseCrashlytics: FirebaseCrashlytics
) {

    private var currentRoute: String? = null
    private var lastRoute: String? = null

    fun setScreen(route: String) {
        if (route == currentRoute) {
            return
        }

        if (currentRoute == null) {
            currentRoute = route
        } else {
            lastRoute = currentRoute
            currentRoute = route
        }
        set(ROUTE, route)
        set(LAST_ROUTE, lastRoute)
    }

    fun set(key: String, value: String?) {
        value ?: return

        if (key == USER_WALLET || key == POWER_USER_WALLET) {
            firebaseCrashlytics.setUserId(value)
            firebaseCrashlytics.setCustomKey(key, value)
        } else {
            firebaseCrashlytics.setCustomKey(key, value)
        }
    }

    fun set(key: String, value: Boolean) {
        firebaseCrashlytics.setCustomKey(key, value)
    }

    fun clear(key: String) {
        if (key == USER_WALLET || key == POWER_USER_WALLET) {
            firebaseCrashlytics.setUserId("")
        }
        firebaseCrashlytics.setCustomKey(key, "")
    }

    companion object {
        const val IS_SYNCING = "is_syncing"
        const val USER_WALLET = "user_wallet"
        const val POWER_USER_WALLET = "power_user_wallet"
        const val DESTINATION_WALLET = "destination_wallet"
        const val SESSION_NOTE = "session_note"
        const val ORG_NAME = "organization_name"
        const val IS_IN_SESSION = "is_in_session"
        private const val LAST_ROUTE = "last_route"
        private const val ROUTE = "route"
    }

}