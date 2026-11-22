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
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

/**
 * Brings the capture setup scope back for a setup step restored after process death.
 *
 * Navigation 3 restores the step's screen, but [CaptureSetupScopeManager] keeps the scope only in
 * memory, so the step's view model would read a closed scope (#1447). A step's view model calls
 * [reopenScopeIfClosed] first thing in `init`, then [setupUser] from a coroutine before it reads
 * the setup data. The setup step index needs nothing: `CaptureSetupNavigationController.navForward`
 * re-derives it from the restored back stack.
 */
class CaptureSetupRestorer(
    private val preferences: Preferences,
    private val userRepo: UserRepo,
) {
    /**
     * Opens the capture setup scope if nothing has (the step was restored after process death).
     * Returns true in that case; the scope then has no user until [setupUser] reloads it.
     */
    fun reopenScopeIfClosed(): Boolean {
        if (CaptureSetupScopeManager.isOpen) return false
        CaptureSetupScopeManager.open()
        return true
    }

    /**
     * The user capture setup is for. With an open scope that is the scope's user, whose id is
     * saved for a later restore. When [restoring], the saved user is reloaded into the scope; if
     * it can't be found, the reopened scope is closed again and setup can't continue.
     */
    suspend fun setupUser(restoring: Boolean): User? {
        val data = CaptureSetupScopeManager.getData()
        val user =
            if (restoring) {
                userRepo.getUser(preferences.getLong(SETUP_USER_ID_KEY))?.also { data.user = it }
            } else {
                data.user
            }
        if (user != null) {
            preferences.edit().putLong(SETUP_USER_ID_KEY, user.id).apply()
        } else if (restoring) {
            CaptureSetupScopeManager.close()
        }
        return user
    }

    companion object {
        /** The capture setup user, kept on disk so a setup step can be restored after process death. */
        internal val SETUP_USER_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-user-id")
    }
}
