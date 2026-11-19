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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.preferences.PrefKey
import org.greenstand.android.TreeTracker.preferences.PrefKeys
import org.greenstand.android.TreeTracker.preferences.Preferences
import timber.log.Timber

/** The two org-configured flows. */
enum class OrgFlow { SETUP, CAPTURE }

/** A step of an org flow that the dev options screen lists. */
data class OrgStepOption(
    val flow: OrgFlow,
    val route: String,
    val label: String,
    /** Required steps can't be turned off: the navigation controllers assume them. */
    val required: Boolean = false,
)

/** A feature flag on one step of an org flow that the dev options screen lists. */
data class OrgFeatureOption(
    val flow: OrgFlow,
    val route: String,
    val feature: OrgFeature,
    val label: String,
) {
    val key: String get() = featureKey(flow, route, feature)
}

internal fun featureKey(
    flow: OrgFlow,
    route: String,
    feature: OrgFeature,
): String = "${flow.name}|$route|${feature.key}"

/**
 * Every org link setting the dev options screen can override. Steps are listed in their
 * canonical order, which is also where a step that the org leaves out is inserted.
 */
object OrgOverrideCatalog {
    val setupSteps: List<OrgStepOption> =
        listOf(
            // The dashboard always opens user select, so the setup flow must start there.
            OrgStepOption(OrgFlow.SETUP, RouteRegistry.ROUTE_USER_SELECT, "User select", required = true),
            OrgStepOption(OrgFlow.SETUP, RouteRegistry.ROUTE_WALLET_SELECT, "Wallet select"),
            OrgStepOption(OrgFlow.SETUP, RouteRegistry.ROUTE_ADD_ORG, "Add org"),
            OrgStepOption(OrgFlow.SETUP, RouteRegistry.ROUTE_SESSION_NOTE, "Session note"),
        )

    val captureSteps: List<OrgStepOption> =
        listOf(
            // Setup ends on tree capture and the capture flow loops back to it.
            OrgStepOption(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_CAPTURE, "Tree capture", required = true),
            OrgStepOption(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, "Image review"),
            OrgStepOption(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_HEIGHT, "Tree height"),
        )

    val features: List<OrgFeatureOption> =
        listOf(
            OrgFeatureOption(
                OrgFlow.CAPTURE,
                RouteRegistry.ROUTE_TREE_IMAGE_REVIEW,
                OrgFeature.FORCE_NOTE,
                "Force note on image review",
            ),
        )

    fun steps(flow: OrgFlow): List<OrgStepOption> =
        when (flow) {
            OrgFlow.SETUP -> setupSteps
            OrgFlow.CAPTURE -> captureSteps
        }
}

/**
 * Debug-only overrides of the current org's link settings, set on the dev options screen.
 * Each map holds only the settings that were changed; anything missing follows the org.
 * Overrides apply to whichever org is current, so switching orgs keeps them.
 */
@Serializable
data class OrgOverrides(
    /** Master switch. Off keeps the stored overrides but applies none of them. */
    val enabled: Boolean = false,
    /** Setup flow route -> on/off. */
    val setupSteps: Map<String, Boolean> = emptyMap(),
    /** Capture flow route -> on/off. */
    val captureSteps: Map<String, Boolean> = emptyMap(),
    /** [OrgFeatureOption.key] -> on/off. */
    val features: Map<String, Boolean> = emptyMap(),
    /** Replaces the org's wallet ID when not null. */
    val walletId: String? = null,
) {
    val hasOverrides: Boolean
        get() = setupSteps.isNotEmpty() || captureSteps.isNotEmpty() || features.isNotEmpty() || walletId != null

    fun steps(flow: OrgFlow): Map<String, Boolean> =
        when (flow) {
            OrgFlow.SETUP -> setupSteps
            OrgFlow.CAPTURE -> captureSteps
        }

    fun withStep(
        flow: OrgFlow,
        route: String,
        enabled: Boolean?,
    ): OrgOverrides {
        val updated = steps(flow).toMutableMap().apply { if (enabled == null) remove(route) else put(route, enabled) }
        return when (flow) {
            OrgFlow.SETUP -> copy(setupSteps = updated)
            OrgFlow.CAPTURE -> copy(captureSteps = updated)
        }
    }

    fun withFeature(
        key: String,
        enabled: Boolean?,
    ): OrgOverrides = copy(features = features.toMutableMap().apply { if (enabled == null) remove(key) else put(key, enabled) })

    /** Returns [org] with these overrides on top, or [org] unchanged when they are switched off. */
    fun applyTo(org: Org): Org {
        if (!enabled) return org
        return org.copy(
            walletId = walletId ?: org.walletId,
            captureSetupFlow = applyFlow(OrgFlow.SETUP, org.captureSetupFlow),
            captureFlow = applyFlow(OrgFlow.CAPTURE, org.captureFlow),
        )
    }

    private fun applyFlow(
        flow: OrgFlow,
        destinations: List<Destination>,
    ): List<Destination> {
        val catalog = OrgOverrideCatalog.steps(flow)
        val required = catalog.filter { it.required }.map { it.route }.toSet()
        val stepOverrides = steps(flow).filterKeys { it !in required }

        val result =
            destinations
                .filter { stepOverrides[RouteRegistry.normalize(it.route)] != false }
                .toMutableList()

        catalog.forEachIndexed { index, step ->
            if (stepOverrides[step.route] != true) return@forEachIndexed
            if (result.any { RouteRegistry.normalize(it.route) == step.route }) return@forEachIndexed
            // Insert after the last step that comes before this one in the canonical order.
            val earlier = catalog.subList(0, index).map { it.route }.toSet()
            val insertAt = result.indexOfLast { RouteRegistry.normalize(it.route) in earlier } + 1
            result.add(insertAt, Destination(step.route))
        }

        return result.map { destination -> applyFeatures(flow, destination) }
    }

    private fun applyFeatures(
        flow: OrgFlow,
        destination: Destination,
    ): Destination {
        val route = RouteRegistry.normalize(destination.route)
        var features = destination.features
        OrgOverrideCatalog.features
            .filter { it.flow == flow && it.route == route }
            .forEach { option ->
                val enabled = this.features[option.key] ?: return@forEach
                val others = features.orEmpty().filter { OrgFeature.fromKey(it) != option.feature }
                features = if (enabled) others + option.feature.key else others
            }
        return if (features == destination.features) destination else destination.copy(features = features)
    }
}

/**
 * Keeps [OrgOverrides] in preferences so they survive app restarts. Release builds always see
 * no overrides, whatever is stored.
 */
class OrgOverrideStore(
    private val prefs: Preferences,
    private val json: Json,
    private val isDebugBuild: Boolean = FeatureFlags.DEBUG_ENABLED,
) {
    private val _overrides = MutableStateFlow(load())
    val overrides: StateFlow<OrgOverrides> = _overrides.asStateFlow()

    /** The overrides to apply now: none unless this is a debug build and the master switch is on. */
    val active: OrgOverrides?
        get() = _overrides.value.takeIf { isDebugBuild && it.enabled }

    fun update(transform: (OrgOverrides) -> OrgOverrides) {
        val updated = transform(_overrides.value)
        _overrides.value = updated
        prefs.edit().putString(KEY, json.encodeToString(updated)).commit()
        Timber.tag(TAG).d("Org link overrides: $updated")
    }

    fun clear() {
        _overrides.value = OrgOverrides()
        prefs.edit().remove(KEY).commit()
        Timber.tag(TAG).d("Org link overrides cleared")
    }

    private fun load(): OrgOverrides {
        if (!isDebugBuild) return OrgOverrides()
        val stored = prefs.getString(KEY, null)
        if (stored.isNullOrBlank()) return OrgOverrides()
        return try {
            json.decodeFromString<OrgOverrides>(stored)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Ignoring unreadable org link overrides")
            OrgOverrides()
        }
    }

    companion object {
        private const val TAG = "OrgLink"
        val KEY = PrefKeys.SYSTEM_SETTINGS + PrefKey("org-link-overrides")
    }
}
