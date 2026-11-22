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

import kotlinx.coroutines.CancellationException
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

/**
 * Reopens the capture setup scope after process death.
 *
 * Navigation 3 restores the back stack, so the app can come back on a setup screen (Add org,
 * wallet select, session note) or on capture with the setup screens under it, while the setup
 * scope those screens read was only in memory (#1418). Root calls [restoreIfNeeded] once per
 * process before it shows the back stack.
 */
class CaptureSetupRestorer(
    private val store: CaptureSetupStore,
    private val userRepo: UserRepo,
    private val preferences: Preferences,
    private val locationDataCapturer: LocationDataCapturer,
) {
    /**
     * Opens the setup scope with the saved setup, if one was in progress and no scope is open.
     * Call from the main thread. Does nothing when nothing was saved; drops the saved setup
     * when its user no longer exists.
     */
    suspend fun restoreIfNeeded() {
        if (CaptureSetupScopeManager.isOpen) return
        val saved = store.load() ?: return
        val user =
            try {
                userRepo.getUser(saved.userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag("CaptureSetupScope").e(e, "Could not load the user of the saved capture setup")
                null
            }
        if (user == null) {
            store.clear()
            return
        }
        // User select does these two when the user is picked and when setup starts.
        preferences.setUserId(user.id)
        CaptureSetupScopeManager.restore {
            this.user = user
            destinationWallet = saved.destinationWallet
            sessionNote = saved.sessionNote
            organizationName = saved.organizationName
        }
        locationDataCapturer.startGpsUpdates()
    }
}
