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

/**
 * Type-safe feature flags that can be enabled per-destination in org flow configs.
 * Features are specified as strings in org JSON and mapped to enum values here.
 */
enum class OrgFeature(val key: String) {
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

/**
 * Resolves feature flags for the current org's flow destinations.
 * Inject this via Koin to check features in ViewModels instead of
 * manually searching destination lists.
 */
class FeatureResolver(private val orgRepo: OrgRepo) {

    /**
     * Check if a feature is enabled for a specific route in the capture flow.
     */
    fun isCaptureFlowFeatureEnabled(routeId: String, feature: OrgFeature): Boolean {
        return orgRepo.currentOrg().captureFlow
            .find { it.route == routeId }
            ?.features
            ?.any { OrgFeature.fromKey(it) == feature }
            ?: false
    }

    /**
     * Check if a feature is enabled for a specific route in the setup flow.
     */
    fun isSetupFlowFeatureEnabled(routeId: String, feature: OrgFeature): Boolean {
        return orgRepo.currentOrg().captureSetupFlow
            .find { it.route == routeId }
            ?.features
            ?.any { OrgFeature.fromKey(it) == feature }
            ?: false
    }
}
