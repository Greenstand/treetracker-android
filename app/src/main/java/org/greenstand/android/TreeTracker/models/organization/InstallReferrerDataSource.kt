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
package org.greenstand.android.TreeTracker.models.organization

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

sealed interface InstallReferrerResult {
    data class Available(
        val referrer: String?,
    ) : InstallReferrerResult

    /** Play Store too old, or absent. Nothing to wait for on a later launch. */
    data object Unsupported : InstallReferrerResult

    /** Transient, so the caller leaves the referrer unconsumed and retries. */
    data object Unavailable : InstallReferrerResult
}

fun interface InstallReferrerDataSource {
    suspend fun fetch(): InstallReferrerResult
}

/** Wraps [InstallReferrerClient] so callers see a result instead of a connection. */
class PlayInstallReferrerDataSource(
    private val context: Context,
) : InstallReferrerDataSource {
    override suspend fun fetch(): InstallReferrerResult {
        val client = InstallReferrerClient.newBuilder(context).build()
        return try {
            awaitSetup(client)
        } finally {
            runCatching { client.endConnection() }
        }
    }

    private suspend fun awaitSetup(client: InstallReferrerClient): InstallReferrerResult =
        suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)

            fun finish(result: InstallReferrerResult) {
                if (resumed.compareAndSet(false, true)) continuation.resume(result)
            }
            client.startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        finish(
                            when (responseCode) {
                                InstallReferrerClient.InstallReferrerResponse.OK ->
                                    InstallReferrerResult.Available(
                                        runCatching { client.installReferrer.installReferrer }
                                            .onFailure { Timber.tag(TAG).w(it, "Could not read install referrer") }
                                            .getOrNull(),
                                    )
                                InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                                    InstallReferrerResult.Unsupported
                                else -> InstallReferrerResult.Unavailable
                            },
                        )
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        finish(InstallReferrerResult.Unavailable)
                    }
                },
            )
        }

    private companion object {
        const val TAG = "OrgLink"
    }
}