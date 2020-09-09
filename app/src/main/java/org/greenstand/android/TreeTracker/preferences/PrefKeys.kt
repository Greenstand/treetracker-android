package org.greenstand.android.TreeTracker.preferences

object PrefKeys {

    private val ROOT = PrefKey("greenstand")

    val SESSION = ROOT + PrefKey("user-session")

    val USER_SETTINGS = ROOT + PrefKey("user-settings")

    val SYSTEM_SETTINGS = ROOT + PrefKey("system-settings")
}
