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

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

data class OrgLink(
    val id: String,
    val name: String,
)

/**
 * The org a planter was invited to, from the Play install referrer.
 *
 * The app:// deep link cannot carry it: it never reaches a device without the app.
 * Read once, so a later org switch is not overwritten.
 */
class InstallReferrerOrgProvider(
    private val prefs: Preferences,
    private val dataSource: InstallReferrerDataSource,
) {
    suspend fun consume(): OrgLink? =
        withContext(Dispatchers.IO) {
            if (prefs.getBoolean(CONSUMED_KEY, false)) return@withContext null

            val result =
                withTimeoutOrNull(REFERRER_TIMEOUT_MS) { dataSource.fetch() }
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
        private const val ORG_ID_PARAM = "org_id"
        private const val ORG_NAME_PARAM = "org_name"
        private const val ORG_LINK_TAG = "OrgLink"
        private const val REFERRER_TIMEOUT_MS = 3_000L
        private val CONSUMED_KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("install-referrer-consumed")

        /** Parses `org_id=42&org_name=Acme%20Trees`, the shape the admin panel puts on the Play url. */
        internal fun parseOrgLink(referrer: String?): OrgLink? {
            if (referrer.isNullOrBlank()) return null
            val uri = Uri.parse("https://referrer/?$referrer")
            val id = uri.getQueryParameter(ORG_ID_PARAM)?.takeIf { it.isNotBlank() } ?: return null
            return OrgLink(id, uri.getQueryParameter(ORG_NAME_PARAM).orEmpty())
        }
    }
}