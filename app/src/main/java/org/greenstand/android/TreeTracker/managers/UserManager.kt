package org.greenstand.android.TreeTracker.managers

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import org.greenstand.android.TreeTracker.utilities.ValueHelper

class UserManager(private val context: Context,
                  private val sharedPreferences: SharedPreferences,
                  private val planterManager: PlanterManager) {

    private val userLoginChannel = BroadcastChannel<Unit>(1)
    private val userDetailsChannel = BroadcastChannel<Unit>(1)

    val userLoginReceiveChannel: ReceiveChannel<Unit> = userLoginChannel.openSubscription()
    val userDetailsReceiveChannel: ReceiveChannel<Unit> = userDetailsChannel.openSubscription()

    var authToken: String? = null

    val isLoggedIn: Boolean
        get() = sharedPreferences.getString(ValueHelper.PLANTER_IDENTIFIER, null) != null

    val userId: Long
        get() = context.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE).getLong(ValueHelper.MAIN_USER_ID, -1)

    var firstName: String?
        get() = sharedPreferences.getString(FIRST_NAME_KEY, null)
        set(value) = sharedPreferences.edit().putString(FIRST_NAME_KEY, value).apply()

    var lastName: String?
        get() = sharedPreferences.getString(LAST_NAME_KEY, null)
        set(value) = sharedPreferences.edit().putString(LAST_NAME_KEY, value).apply()

    var organization: String?
        get() = sharedPreferences.getString(ORG_NAME_KEY, null)
        set(value) = sharedPreferences.edit().putString(ORG_NAME_KEY, value).apply()

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
            putString(ValueHelper.PLANTER_IDENTIFIER, null)
        }.apply()
    }

    suspend fun login(identifier: String,
                      photoPath: String,
                      location: Location?) {

        planterManager.addPlanterIdentification(identifier,
                                                photoPath,
                                                location)

        userLoginChannel.send(Unit)
    }

    suspend fun addLoginDetails(identification: String,
                                firstName: String,
                                lastName: String,
                                organization: String?,
                                timeCreated: String,
                                location: Location?) {

        val planterDetailsId = planterManager.addPlanterDetails(identification,
                                                                firstName,
                                                                lastName,
                                                                organization,
                                                                timeCreated,
                                                                location)

        this.firstName = firstName
        this.lastName = lastName
        this.organization = organization

        planterManager.updateIdentifierId(identification, planterDetailsId)

        userDetailsChannel.send(Unit)
    }

    companion object {
        private const val FIRST_NAME_KEY = "FIRST_NAME_KEY"
        private const val LAST_NAME_KEY = "LAST_NAME_KEY"
        private const val ORG_NAME_KEY = "ORG_NAME_KEY"
    }
}