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
package org.greenstand.android.TreeTracker.models.organization

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class OrgConfigProvider(
    private val remoteConfig: FirebaseRemoteConfig,
) {
    /**
     * Attempts to fetch org configuration from Firebase Remote Config.
     * Returns the JSON string if found, or null if:
     * - Device is offline
     * - Fetch times out
     * - Key does not exist in Remote Config
     * - Any error occurs
     */
    suspend fun fetchOrgConfig(orgId: String): String? {
        val key = "$KEY_PREFIX$orgId"
        Timber.tag(TAG).d("Fetching Remote Config for key: $key")
        val startTime = System.currentTimeMillis()
        return try {
                val fetched = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                remoteConfig.fetchAndActivate().await()
            }
            val elapsed = System.currentTimeMillis() - startTime
            if (fetched == null) {
                Timber.tag(TAG).w("Remote Config fetch timed out after ${elapsed}ms for org $orgId")
                return null
            }

            Timber.tag(TAG).d("Remote Config fetchAndActivate completed in ${elapsed}ms (changed=$fetched)")
            val configValue = remoteConfig.getString(key)

            if (configValue.isBlank()) {
                Timber.tag(TAG).w("No Remote Config value for key: $key")
                null
            } else {
                Timber.tag(TAG).d("Remote Config value found for key: $key (${configValue.length} chars)")
                configValue
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Timber.tag(TAG).e(e, "Remote Config fetch failed after ${elapsed}ms for org $orgId")
            null
        }
    }

    companion object {
        private const val TAG = "OrgLink"
        private const val KEY_PREFIX = "org_"
        private const val FETCH_TIMEOUT_MS = 10_000L
    }
}
