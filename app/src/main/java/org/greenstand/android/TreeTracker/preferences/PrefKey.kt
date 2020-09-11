package org.greenstand.android.TreeTracker.preferences

open class PrefKey(val path: String) {

    operator fun plus(prefKey: PrefKey): PrefKey {
        return when (prefKey) {
            is UserPrefKey -> UserPrefKey("$path/${prefKey.path}")
            else -> PrefKey("$path/${prefKey.path}")
        }
    }

    fun asUserPref(): UserPrefKey {
        return UserPrefKey(path)
    }
}
