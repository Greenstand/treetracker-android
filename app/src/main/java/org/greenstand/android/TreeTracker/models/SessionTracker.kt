/*
 * Copyright 2023 Treetracker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greenstand.android.TreeTracker.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.CrashKey
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.SessionEntity
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.utilities.TimeProvider
import java.util.UUID

class SessionTracker(
    private val dao: TreeTrackerDAO,
    private val treesToSyncHelper: TreesToSyncHelper,
    private val preferences: Preferences,
    private val timeProvider: TimeProvider,
    private val orgRepo: OrgRepo,
    private val exceptionDataCollector: ExceptionDataCollector,
) {
    private var _currentSessionId: Long? = null
    val currentSessionId: Long?
        get() = _currentSessionId

    suspend fun startSession() {
        val captureSetupData = CaptureSetupScopeManager.getData()
        endSession()

        withContext(Dispatchers.IO) {
            val sessionEntity =
                createSession(
                    userId = captureSetupData.user!!.id,
                    organizationName = captureSetupData.organizationName,
                    sessionNote = captureSetupData.sessionNote,
                )

            _currentSessionId = sessionEntity.id

            preferences.edit().putLong(SESSION_ID_KEY, _currentSessionId ?: -1).commit()
            exceptionDataCollector.apply {
                set(CrashKey.ORG_NAME, sessionEntity.organization)
                set(CrashKey.DESTINATION_WALLET, sessionEntity.destinationWallet)
                set(CrashKey.USER_WALLET, sessionEntity.originWallet)
                set(CrashKey.SESSION_NOTE, sessionEntity.note)
                set(CrashKey.IS_IN_SESSION, true)
                set(CrashKey.CAPTURE_SESSION_UUID, sessionEntity.uuid)
            }
        }
    }

    /**
     * Saves a new session for [userId] without making it the current one, so a capture flow in
     * progress is unaffected. [organizationName] falls back to the current org when null. End it
     * with [closeSession].
     */
    suspend fun createSession(
        userId: Long,
        organizationName: String?,
        sessionNote: String?,
    ): SessionEntity =
        withContext(Dispatchers.IO) {
            val userEntity = dao.getUserById(userId) ?: error("Could not find user of id $userId")

            SessionEntity(
                uuid = UUID.randomUUID().toString(),
                originUserId = userEntity.uuid,
                originWallet = userEntity.wallet,
                destinationWallet = userEntity.wallet ?: orgRepo.currentOrg().walletId,
                startTime = timeProvider.currentTime(),
                isUploaded = false,
                organization = organizationName ?: orgRepo.currentOrg().name,
                deviceConfigId = dao.getLatestDeviceConfig()!!.id,
                note = sessionNote,
            ).also { it.id = dao.insertSession(it) }
        }

    /** Sets the end time of session [sessionId], leaving the current session alone. */
    suspend fun closeSession(sessionId: Long) {
        val session = dao.getSessionById(sessionId)
        session.endTime = timeProvider.currentTime()
        dao.updateSession(session)
    }

    suspend fun endSession() {
        _currentSessionId?.let { id ->
            closeSession(id)

            _currentSessionId = null

            withContext(Dispatchers.IO) {
                treesToSyncHelper.refreshTreeCountToSync()
                preferences.edit().putLong(SESSION_ID_KEY, -1).commit()
            }
        }

        exceptionDataCollector.apply {
            clear(CrashKey.ORG_NAME)
            clear(CrashKey.DESTINATION_WALLET)
            clear(CrashKey.SESSION_NOTE)
            clear(CrashKey.CAPTURE_SESSION_UUID)
            clear(CrashKey.PENDING_UPLOAD_COUNT)
            set(CrashKey.IS_IN_SESSION, false)
        }
    }

    fun wasSessionInterrupted(): Boolean =
        _currentSessionId == null &&
            preferences.getLong(SESSION_ID_KEY) != -1L

    companion object {
        // Session key is used to keep track of session ID after app is killed. If the app is killed
        // while a session is in progress, this lets the system know
        val SESSION_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("session-active")
    }
}