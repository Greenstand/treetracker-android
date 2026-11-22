/*
 * Copyright 2026 Treetracker
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
package org.greenstand.android.TreeTracker.models.setupflow

import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

/**
 * Keeps a copy of [CaptureSetupData] on disk so a capture setup step can come back after process
 * death.
 *
 * Navigation 3 restores the step's screen, but [CaptureSetupScopeManager] only keeps the setup
 * scope in memory. A setup step's ViewModel calls [reopenIfClosed] first thing in `init`, then
 * [reloadUser] if that returned true, and [save] once the scope has its user and again whenever
 * the step changes the setup data.
 */
class CaptureSetupRestorer(
    private val preferences: Preferences,
    private val userRepo: UserRepo,
) {
    /**
     * Reopens the setup scope if it is closed, with the session note, destination wallet and
     * organization name from the last [save]. The user is not back yet: call [reloadUser].
     *
     * @return true if the scope was closed, which means the screen was restored after process death.
     */
    fun reopenIfClosed(): Boolean {
        if (CaptureSetupScopeManager.isOpen) return false
        CaptureSetupScopeManager.open()
        CaptureSetupScopeManager.getData().apply {
            sessionNote = preferences.getString(SESSION_NOTE_KEY)
            destinationWallet = preferences.getString(DESTINATION_WALLET_KEY)
            organizationName = preferences.getString(ORGANIZATION_NAME_KEY)
        }
        return true
    }

    /**
     * Loads the user from the last [save] into the scope that [reopenIfClosed] reopened. If that
     * user no longer exists, closes the scope again.
     *
     * @return false if the user is gone; setup can't continue and the screen should leave.
     */
    suspend fun reloadUser(): Boolean {
        val user = userRepo.getUser(preferences.getLong(USER_ID_KEY))
        if (user == null) {
            CaptureSetupScopeManager.close()
            return false
        }
        // User select does both of these when the user is picked.
        preferences.setUserId(user.id)
        CaptureSetupScopeManager.getData().user = user
        return true
    }

    /** Writes the open scope's setup data to disk. */
    fun save() {
        val data = CaptureSetupScopeManager.getData()
        preferences
            .edit()
            .putLong(USER_ID_KEY, data.user?.id ?: NO_USER_ID)
            .putString(SESSION_NOTE_KEY, data.sessionNote)
            .putString(DESTINATION_WALLET_KEY, data.destinationWallet)
            .putString(ORGANIZATION_NAME_KEY, data.organizationName)
            .apply()
    }

    companion object {
        private const val NO_USER_ID = -1L

        internal val USER_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-user-id")
        internal val SESSION_NOTE_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-session-note")
        internal val DESTINATION_WALLET_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-destination-wallet")
        internal val ORGANIZATION_NAME_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-organization-name")
    }
}
