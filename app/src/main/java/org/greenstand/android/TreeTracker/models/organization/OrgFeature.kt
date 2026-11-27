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

import timber.log.Timber

enum class OrgFeature(
    val key: String,
) {
    FORCE_NOTE("forceNote"),
    ;

    companion object {
        private val keyMap = entries.associateBy { it.key }

        fun fromKey(key: String): OrgFeature? {
            val feature = keyMap[key]
            if (feature == null) {
                Timber.tag("OrgFeature").w("Unknown feature flag: '$key'")
            }
            return feature
        }
    }
}

class FeatureResolver(
    private val orgRepo: OrgRepo,
) {
    fun isCaptureFlowFeatureEnabled(
        routeId: String,
        feature: OrgFeature,
    ): Boolean =
        orgRepo
            .currentOrg()
            .captureFlow
            .find { it.route == routeId }
            ?.features
            ?.any { OrgFeature.fromKey(it) == feature }
            ?: false

    fun isSetupFlowFeatureEnabled(
        routeId: String,
        feature: OrgFeature,
    ): Boolean =
        orgRepo
            .currentOrg()
            .captureSetupFlow
            .find { it.route == routeId }
            ?.features
            ?.any { OrgFeature.fromKey(it) == feature }
            ?: false
}