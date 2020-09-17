package org.greenstand.android.TreeTracker.preferences

object PrefKeys {

    private val ROOT = PrefKey("greenstand")

    /**
     * Anything depending on SESSION will have its value deleted when the user is logged out
     */
    val SESSION = ROOT + PrefKey("session")

    val USER_SETTINGS = ROOT + PrefKey("user-settings")

    val SYSTEM_SETTINGS = ROOT + PrefKey("system-settings")
}
