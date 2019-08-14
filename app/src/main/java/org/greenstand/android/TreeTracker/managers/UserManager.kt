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
        get() = sharedPreferences.getLong(ValueHelper.PLANTER_INFO_ID, -1) != -1L

    val userId: Long
        get() = context.getSharedPreferences(ValueHelper.NAME_SPACE, Context.MODE_PRIVATE).getLong("", -1)

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
            putLong(ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS, 0)
            putString(ValueHelper.PLANTER_PHOTO, null)
            putLong(ValueHelper.PLANTER_CHECK_IN_ID, -1)
            putLong(ValueHelper.PLANTER_INFO_ID, -1)
        }.apply()
    }

    suspend fun login(identifier: String,
                      photoPath: String,
                      location: Location?) {


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