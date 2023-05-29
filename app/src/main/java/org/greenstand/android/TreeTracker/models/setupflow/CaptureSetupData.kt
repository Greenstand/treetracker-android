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

class CaptureSetupData(private val exceptionDataCollector: ExceptionDataCollector) {

    var user: User? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.USER_WALLET, value?.wallet)
            field = value
        }

    var destinationWallet: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.DESTINATION_WALLET, value)
            field = value
        }

    var sessionNote: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.SESSION_NOTE, value)
            field = value
        }

    var organizationName: String? = null
        set(value) {
            exceptionDataCollector.set(ExceptionDataCollector.ORG_NAME, value)
            field = value
        }
}