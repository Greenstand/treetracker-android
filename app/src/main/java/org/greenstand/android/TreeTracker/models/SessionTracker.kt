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
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.app.dao.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.app.entity.SessionEntity
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
            val userEntity = dao.getUserById(captureSetupData.user!!.id) ?: throw IllegalStateException("Could not find user of id ${captureSetupData.user!!.id}")

            val sessionEntity = SessionEntity(
                uuid = UUID.randomUUID().toString(),
                originUserId = userEntity.uuid,
                originWallet = userEntity.wallet,
                destinationWallet = captureSetupData.destinationWallet ?: orgRepo.currentOrg().walletId,
                startTime = timeProvider.currentTime(),
                isUploaded = false,
                organization = captureSetupData.organizationName,
                deviceConfigId = dao.getLatestDeviceConfig()!!.id,
                note = captureSetupData.sessionNote,
            )

            _currentSessionId = dao.insertSession(sessionEntity)

            preferences.edit().putLong(SESSION_ID_KEY, _currentSessionId ?: -1).commit()
            exceptionDataCollector.apply {
                set(ExceptionDataCollector.ORG_NAME, sessionEntity.organization)
                set(ExceptionDataCollector.DESTINATION_WALLET, sessionEntity.destinationWallet)
                set(ExceptionDataCollector.USER_WALLET, sessionEntity.originWallet)
                set(ExceptionDataCollector.SESSION_NOTE, sessionEntity.note)
                set(ExceptionDataCollector.IS_IN_SESSION, true)
            }
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

        exceptionDataCollector.clear(ExceptionDataCollector.ORG_NAME)
        exceptionDataCollector.clear(ExceptionDataCollector.DESTINATION_WALLET)
        exceptionDataCollector.clear(ExceptionDataCollector.SESSION_NOTE)
        exceptionDataCollector.set(ExceptionDataCollector.IS_IN_SESSION, false)
    }

    fun wasSessionInterrupted(): Boolean {
        return _currentSessionId == null &&
            preferences.getLong(SESSION_ID_KEY) != -1L
    }

    companion object {
        // Session key is used to keep track of session ID after app is killed. If the app is killed
        // while a session is in progress, this lets the system know
        val SESSION_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("session-active")
    }
}