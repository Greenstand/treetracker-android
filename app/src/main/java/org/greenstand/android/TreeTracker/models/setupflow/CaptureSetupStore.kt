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
package org.greenstand.android.TreeTracker.models.setupflow

import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences

/** What the user has entered so far in a capture setup, in a form that outlives the process. */
data class SavedCaptureSetup(
    val userId: Long,
    val destinationWallet: String? = null,
    val sessionNote: String? = null,
    val organizationName: String? = null,
)

/**
 * Keeps the capture setup in progress on disk.
 *
 * The capture setup scope lives only in memory, but the Navigation 3 back stack survives process
 * death, so Android can restore a setup screen (e.g. Add org) into a process that has no setup
 * scope (#1418). [CaptureSetupData] saves here on every change and [CaptureSetupRestorer] reads
 * it back before the restored back stack is shown.
 */
class CaptureSetupStore(
    private val preferences: Preferences,
) {
    fun save(setup: SavedCaptureSetup) {
        preferences
            .edit()
            .putLong(USER_ID_KEY, setup.userId)
            .putString(DESTINATION_WALLET_KEY, setup.destinationWallet)
            .putString(SESSION_NOTE_KEY, setup.sessionNote)
            .putString(ORG_NAME_KEY, setup.organizationName)
            .apply()
    }

    /** The saved setup, or null when no setup with a selected user was in progress. */
    fun load(): SavedCaptureSetup? {
        val userId = preferences.getLong(USER_ID_KEY, NO_USER)
        if (userId == NO_USER) return null
        return SavedCaptureSetup(
            userId = userId,
            destinationWallet = preferences.getString(DESTINATION_WALLET_KEY),
            sessionNote = preferences.getString(SESSION_NOTE_KEY),
            organizationName = preferences.getString(ORG_NAME_KEY),
        )
    }

    fun clear() {
        preferences
            .edit()
            .remove(USER_ID_KEY)
            .remove(DESTINATION_WALLET_KEY)
            .remove(SESSION_NOTE_KEY)
            .remove(ORG_NAME_KEY)
            .apply()
    }

    private companion object {
        const val NO_USER = -1L
        val ROOT = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup")
        val USER_ID_KEY = ROOT + PrefKey("user-id")
        val DESTINATION_WALLET_KEY = ROOT + PrefKey("destination-wallet")
        val SESSION_NOTE_KEY = ROOT + PrefKey("session-note")
        val ORG_NAME_KEY = ROOT + PrefKey("org-name")
    }
}
