package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.SharedPreferences
import org.greenstand.android.TreeTracker.utilities.ValueHelper

class UserManager(private val context: Context,
                  private val sharedPreferences: SharedPreferences) {

    var authToken: String? = null

    val isLoggedIn: Boolean
        get() = sharedPreferences.getString(ValueHelper.PLANTER_IDENTIFIER, null) != null

    val userId: Long
        get() = context.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE).getLong(ValueHelper.MAIN_USER_ID, -1)

    fun isUserLoggedIn(): Boolean {
        return if (isLoggedIn) {
            true
        } else {
            clearUser()
            false
        }
    }

    fun clearUser() {
        sharedPreferences.edit().apply {
            putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
            putString(ValueHelper.PLANTER_PHOTO, null)
        }.apply()
    }
}