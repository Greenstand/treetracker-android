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
 * What the user has chosen so far in the capture setup flow. With a [store], every change is
 * also saved so the setup can be restored after process death (see [CaptureSetupRestorer]).
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

    /** Forgets the saved copy: this setup is over, or a new one is starting. */
    fun clearSaved() {
        store?.clear()
    }

    private fun save() {
        // Nothing to restore before a user is picked: user select itself starts a new setup.
        val userId = user?.id ?: return
        store?.save(
            SavedCaptureSetup(
                userId = userId,
                destinationWallet = destinationWallet,
                sessionNote = sessionNote,
                organizationName = organizationName,
            ),
        )
    }
}
