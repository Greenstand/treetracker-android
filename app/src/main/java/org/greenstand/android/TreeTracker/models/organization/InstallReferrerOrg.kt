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
import android.net.Uri
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

private const val ORG_ID_PARAM = "org_id"
private const val ORG_NAME_PARAM = "org_name"
private const val ORG_LINK_TAG = "OrgLink"
private const val REFERRER_TIMEOUT_MS = 3_000L

data class OrgLink(
    val id: String,
    val name: String,
)

sealed interface InstallReferrerResult {
    data class Available(
        val referrer: String?,
    ) : InstallReferrerResult

    /** Play Store too old, or absent. Nothing to wait for on a later launch. */
    data object Unsupported : InstallReferrerResult

    /** Transient, so the caller leaves the referrer unconsumed and retries. */
    data object Unavailable : InstallReferrerResult
}

/** Parses `org_id=42&org_name=Acme%20Trees`, the shape the admin panel puts on the Play url. */
fun parseOrgLink(referrer: String?): OrgLink? {
    if (referrer.isNullOrBlank()) return null
    val uri = Uri.parse("https://referrer/?$referrer")
    val id = uri.getQueryParameter(ORG_ID_PARAM)?.takeIf { it.isNotBlank() } ?: return null
    return OrgLink(id, uri.getQueryParameter(ORG_NAME_PARAM).orEmpty())
}

suspend fun fetchInstallReferrer(context: Context): InstallReferrerResult {
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
                                        .onFailure { Timber.tag(ORG_LINK_TAG).w(it, "Could not read install referrer") }
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

/**
 * The org a planter was invited to, from the Play install referrer.
 *
 * The app:// deep link cannot carry it: it never reaches a device without the app.
 * Read once, so a later org switch is not overwritten.
 */
class InstallReferrerOrgProvider(
    private val context: Context,
    private val prefs: Preferences,
    private val fetch: suspend (Context) -> InstallReferrerResult = ::fetchInstallReferrer,
) {
    suspend fun consume(): OrgLink? =
        withContext(Dispatchers.IO) {
            if (prefs.getBoolean(CONSUMED_KEY, false)) return@withContext null

            val result =
                withTimeoutOrNull(REFERRER_TIMEOUT_MS) { fetch(context) }
                    ?: InstallReferrerResult.Unavailable

            when (result) {
                is InstallReferrerResult.Available -> {
                    markConsumed()
                    val link = parseOrgLink(result.referrer)
                    if (link == null) {
                        Timber.tag(ORG_LINK_TAG).d("Install referrer carried no org")
                    } else {
                        Timber.tag(ORG_LINK_TAG).i("Install referrer org: id=${link.id}, name='${link.name}'")
                    }
                    link
                }
                InstallReferrerResult.Unsupported -> {
                    Timber.tag(ORG_LINK_TAG).w("Install referrer not supported on this device")
                    markConsumed()
                    null
                }
                InstallReferrerResult.Unavailable -> {
                    Timber.tag(ORG_LINK_TAG).w("Install referrer unavailable, will retry next launch")
                    null
                }
            }
        }

    private fun markConsumed() {
        prefs.edit().putBoolean(CONSUMED_KEY, true).commit()
    }

    companion object {
        private val CONSUMED_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("install-referrer-consumed")
    }
}