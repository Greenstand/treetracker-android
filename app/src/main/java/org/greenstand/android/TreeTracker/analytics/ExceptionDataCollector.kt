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

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import timber.log.Timber
import java.io.IOException
import java.util.Collections
import java.util.WeakHashMap

class ExceptionDataCollector(
    private val firebaseCrashlytics: FirebaseCrashlytics,
    private val analytics: Analytics,
) {
    private var currentRoute: String? = null
    private var lastRoute: String? = null

    private val recordedFailures: MutableSet<Throwable> =
        Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<Throwable, Boolean>()))

    fun setScreen(route: String) {
        if (route == currentRoute) {
            return
        }

        if (currentRoute == null) {
            currentRoute = route
        } else {
            lastRoute = currentRoute
            currentRoute = route
        }
        set(ROUTE, route)
        set(LAST_ROUTE, lastRoute)
    }

    fun set(
        key: String,
        value: String?,
    ) {
        value ?: return

        if (key == USER_WALLET || key == POWER_USER_WALLET) {
            firebaseCrashlytics.setUserId(value)
            firebaseCrashlytics.setCustomKey(key, value)
        } else {
            firebaseCrashlytics.setCustomKey(key, value)
        }
    }

    fun set(
        key: String,
        value: Boolean,
    ) {
        firebaseCrashlytics.setCustomKey(key, value)
    }

    fun recordFailure(
        throwable: Throwable,
        message: String,
        tag: String? = null,
    ) {
        // A cancelled sync is not an upload failure.
        if (throwable is CancellationException) return

        val failureType = classify(throwable)
        recordedFailures.add(throwable)

        // Crashlytics reads custom keys when it writes the event on a background thread, not when
        // recordException is called, so clearing the key here would record an empty value. The
        // key therefore holds the most recent upload failure type until the next failure replaces it.
        set(FAILURE_TYPE, failureType)

        if (tag != null) {
            Timber.tag(tag).e(throwable, "[$failureType] $message")
        } else {
            Timber.e(throwable, "[$failureType] $message")
        }
        // Crashlytics groups issues by stack trace, so this event is the reliable per-type count.
        analytics.uploadFailure(failureType)
    }

    fun wasRecorded(throwable: Throwable): Boolean = throwable in recordedFailures

    fun clear(key: String) {
        if (key == USER_WALLET || key == POWER_USER_WALLET) {
            firebaseCrashlytics.setUserId("")
        }
        firebaseCrashlytics.setCustomKey(key, "")
    }

    companion object {
        const val IS_SYNCING = "is_syncing"
        const val USER_WALLET = "user_wallet"
        const val POWER_USER_WALLET = "power_user_wallet"
        const val DESTINATION_WALLET = "destination_wallet"
        const val SESSION_NOTE = "session_note"
        const val ORG_NAME = "organization_name"
        const val IS_IN_SESSION = "is_in_session"
        const val FAILURE_TYPE = "failure_type"
        private const val LAST_ROUTE = "last_route"
        private const val ROUTE = "route"

        // Failure Types
        const val TYPE_NETWORK = "network_failure"
        const val TYPE_PARSING = "parsing_failure"
        const val TYPE_SERVER = "server_failure"
        const val TYPE_UNKNOWN = "unknown_failure"

        fun classify(throwable: Throwable): String =
            when {
                throwable is SerializationException -> TYPE_PARSING
                throwable is AmazonServiceException -> TYPE_SERVER
                // The AWS SDK wraps connection failures (UnknownHostException, ...) in AmazonClientException.
                throwable is IOException || (throwable is AmazonClientException && throwable.cause is IOException) -> TYPE_NETWORK
                else -> TYPE_UNKNOWN
            }
    }
}