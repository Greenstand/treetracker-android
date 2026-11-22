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

import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.models.user.User

/**
 * The choices made in a capture setup. Each change is also written to [store] (when there is
 * one), so [CaptureSetupScopeManager] can bring the setup back after process death.
 */
class CaptureSetupData(
    private val exceptionDataCollector: ExceptionDataCollector,
    private val store: CaptureSetupStore? = null,
) {
    var user: User? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.USER_WALLET, value?.wallet)
            field = value
            save()
        }

    var destinationWallet: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.DESTINATION_WALLET, value)
            field = value
            save()
        }

    var sessionNote: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.SESSION_NOTE, value)
            field = value
            save()
        }

    var organizationName: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.ORG_NAME, value)
            field = value
            save()
        }

    /** Writes the current (for a new setup: empty) choices to [store], marking a setup as in progress. */
    fun save() {
        store?.save(
            SavedCaptureSetup(
                user = user?.let { SavedUser.from(it) },
                destinationWallet = destinationWallet,
                sessionNote = sessionNote,
                organizationName = organizationName,
            ),
        )
    }

    /** Takes over the choices of a setup that was in progress when the process died. */
    fun restore(saved: SavedCaptureSetup) {
        saved.user?.let { user = it.toUser() }
        saved.destinationWallet?.let { destinationWallet = it }
        saved.sessionNote?.let { sessionNote = it }
        saved.organizationName?.let { organizationName = it }
    }
}
