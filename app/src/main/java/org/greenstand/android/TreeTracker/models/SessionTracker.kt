package org.greenstand.android.TreeTracker.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import java.util.*

class SessionTracker(
    private val dao: TreeTrackerDAO,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val preferences: Preferences,
    private val timeProvider: TimeProvider,
) {

    private var _currentSessionId: Long? = null
    val currentSessionId: Long?
        get() = _currentSessionId //?: throw IllegalStateException("Session ID cannot be null when accessed")

    suspend fun startSession(userId: Long, destinationWallet: String, organization: String) {
        endSession()

        withContext(Dispatchers.IO) {
            val userEntity = dao.getUserById(userId) ?: throw IllegalStateException("Could not find user of id $userId")

            val sessionEntity = SessionEntity(
                uuid = UUID.randomUUID().toString(),
                originUserId = userEntity.uuid,
                originWallet = userEntity.wallet,
                destinationWallet = destinationWallet,
                startTime = timeProvider.currentTime(),
                isUploaded = false,
                organization = organization,
                deviceConfigId = dao.getLatestDeviceConfig()!!.id,
            )

            _currentSessionId = dao.insertSession(sessionEntity)

            preferences.edit().putLong(SESSION_ID_KEY, _currentSessionId ?: -1).commit()
        }
    }

    suspend fun endSession() {
        _currentSessionId?.let { id ->
            val session = dao.getSessionById(id)
            session.endTime = timeProvider.currentTime()
            dao.updateSession(session)

            _currentSessionId = null

            withContext(Dispatchers.IO) {
                treesToSyncHelper.refreshTreeCountToSync()
                preferences.edit().putLong(SESSION_ID_KEY, -1).commit()
            }
        }
    }

    fun wasSessionInterrupted(): Boolean {
        return _currentSessionId == null
                && preferences.getLong(SESSION_ID_KEY) != -1L
    }

    companion object {
        // Session key is used to keep track of session ID after app is killed. If the app is killed
        // while a session is in progress, this lets the system know
        val SESSION_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("session-active")
    }
}
