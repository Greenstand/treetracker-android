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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

/** What the user has chosen so far in a capture setup: the contents of [CaptureSetupData]. */
@Serializable
data class SavedCaptureSetup(
    val user: SavedUser? = null,
    val destinationWallet: String? = null,
    val sessionNote: String? = null,
    val organizationName: String? = null,
)

@Serializable
data class SavedUser(
    val id: Long,
    val wallet: String,
    val numberOfTrees: Int,
    val firstName: String,
    val lastName: String?,
    val photoPath: String,
    val isPowerUser: Boolean,
    val unreadMessagesAvailable: Boolean,
) {
    fun toUser() =
        User(
            id = id,
            wallet = wallet,
            numberOfTrees = numberOfTrees,
            firstName = firstName,
            lastName = lastName,
            photoPath = photoPath,
            isPowerUser = isPowerUser,
            unreadMessagesAvailable = unreadMessagesAvailable,
        )

    companion object {
        fun from(user: User) =
            SavedUser(
                id = user.id,
                wallet = user.wallet,
                numberOfTrees = user.numberOfTrees,
                firstName = user.firstName,
                lastName = user.lastName,
                photoPath = user.photoPath,
                isPowerUser = user.isPowerUser,
                unreadMessagesAvailable = user.unreadMessagesAvailable,
            )
    }
}

/**
 * Keeps the capture setup in progress on disk, so it survives process death.
 *
 * Navigation 3 restores the setup screens (Add org, wallet select, session note, ...) after
 * Android kills the app in the background, but the Koin scope that holds [CaptureSetupData] only
 * lives in memory. [CaptureSetupScopeManager] writes the data here while a setup is open and
 * reads it back when a restored screen asks for a scope that is gone (#1418).
 *
 * A saved setup exists from [CaptureSetupScopeManager.open] until
 * [CaptureSetupScopeManager.close], so [load] returns null when no setup was in progress.
 */
class CaptureSetupStore(
    private val preferences: Preferences,
) {
    fun save(setup: SavedCaptureSetup) {
        preferences.edit().putString(KEY, json.encodeToString(SavedCaptureSetup.serializer(), setup)).apply()
    }

    fun load(): SavedCaptureSetup? {
        val saved = preferences.getString(KEY) ?: return null
        return try {
            json.decodeFromString(SavedCaptureSetup.serializer(), saved)
        } catch (e: Exception) {
            Timber.tag("CaptureSetupStore").e(e, "Could not read the saved capture setup")
            null
        }
    }

    fun clear() {
        if (preferences.getString(KEY) != null) {
            preferences.edit().remove(KEY).apply()
        }
    }

    companion object {
        private val KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("capture-setup-in-progress")
        private val json = Json { ignoreUnknownKeys = true }
    }
}
