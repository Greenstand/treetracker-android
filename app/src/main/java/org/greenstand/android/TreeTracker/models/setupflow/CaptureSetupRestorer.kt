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
 * Brings capture setup back when Android restores a setup step after process death (#1418).
 *
 * Navigation 3 restores the back stack, but [CaptureSetupScopeManager] only keeps the setup scope
 * in memory, so the restored step finds it closed. Each step's view model calls [reopenIfClosed]
 * from init and then [loadUser], which also saves the user's id so a later restore can find it.
 * The setup step index needs nothing: `CaptureSetupNavigationController.navForward` re-derives
 * it from the restored back stack.
 */
class CaptureSetupRestorer(
    private val preferences: Preferences,
    private val userRepo: UserRepo,
) {
    /**
     * Reopens a closed setup scope. Returns true when it did, which means the step was restored
     * after process death and the user must be reloaded.
     */
    fun reopenIfClosed(): Boolean {
        if (CaptureSetupScopeManager.isOpen) return false
        CaptureSetupScopeManager.open()
        return true
    }

    /**
     * The setup user: the one in the open scope, or when [restored], the one saved before the
     * process died. If there is none, closes the scope and returns null: setup is lost.
     */
    suspend fun loadUser(restored: Boolean): User? {
        val user =
            if (restored) {
                userRepo
                    .getUser(preferences.getLong(SETUP_USER_ID_KEY))
                    ?.also { CaptureSetupScopeManager.getData().user = it }
            } else {
                CaptureSetupScopeManager.getData().user
            }
        if (user == null) {
            CaptureSetupScopeManager.close()
            return null
        }
        preferences.edit().putLong(SETUP_USER_ID_KEY, user.id).apply()
        return user
    }

    companion object {
        /** The capture setup user, kept on disk so a setup step can be restored after process death. */
        val SETUP_USER_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-user-id")
    }
}