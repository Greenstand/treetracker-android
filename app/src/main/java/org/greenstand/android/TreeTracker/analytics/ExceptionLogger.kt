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
package org.greenstand.android.TreeTracker.analytics

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import timber.log.Timber

/**
 * Custom Timber Tree that logs messages to Firebase Crashlytics.
 *
 * Rule table:
 * - Timber.i / Timber.w -> breadcrumb only
 * - Timber.e(message) -> breadcrumb only
 * - Timber.e(throwable, message) -> breadcrumb + recordException(throwable)
 * - Timber.v / Timber.d -> ignored
 *
 * Tips:
 * - Don't log Personally Identifiable Information
 * - Only record and send to Crashlytics through this class
 * - Timber.e("...") without throwable doesn't always mean it's an exception.
 */
class ExceptionLogger : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        throwable: Throwable?,
    ) {
        when (priority) {
            Log.INFO -> Firebase.crashlytics.log("[$tag]: $message")
            Log.ERROR -> {
                Firebase.crashlytics.log("[$tag]: $message")
                if (throwable != null) {
                    Firebase.crashlytics.recordException(throwable)
                }
            }

            Log.ASSERT -> Firebase.crashlytics.log("[$tag]: $message")
            Log.VERBOSE, Log.DEBUG -> return
        }
    }
}