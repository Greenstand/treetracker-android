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
import kotlin.coroutines.cancellation.CancellationException

class CaptureSetupRestorer(
    private val preferences: Preferences,
    private val userRepo: UserRepo,
) {
    fun reopenIfClosed(): Boolean {
        if (CaptureSetupScopeManager.isOpen) return false
        CaptureSetupScopeManager.open()
        return true
    }

    suspend fun loadUser(restored: Boolean): User? {
        val data = CaptureSetupScopeManager.getData()
        val user =
            if (restored) {
                val saved = userRepo.getUser(preferences.getLong(SETUP_USER_ID_KEY))
                if (runCatching { CaptureSetupScopeManager.getData() }.getOrNull() !== data) {
                    throw CancellationException("The setup scope changed while the setup user reloaded")
                }
                saved?.also { data.user = it }
            } else {
                data.user
            }
        if (user == null) {
            CaptureSetupScopeManager.close()
            return null
        }
        preferences.edit().putLong(SETUP_USER_ID_KEY, user.id).apply()
        return user
    }

    companion object {
        val SETUP_USER_ID_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-user-id")
    }
}