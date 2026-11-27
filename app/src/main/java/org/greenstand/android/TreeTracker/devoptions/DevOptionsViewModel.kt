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
package org.greenstand.android.TreeTracker.devoptions

import org.greenstand.android.TreeTracker.models.ConvergenceConfiguration
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org
import org.greenstand.android.TreeTracker.models.organization.OrgFeature
import org.greenstand.android.TreeTracker.models.organization.OrgFlow
import org.greenstand.android.TreeTracker.models.organization.OrgOverrideCatalog
import org.greenstand.android.TreeTracker.models.organization.OrgOverrideStore
import org.greenstand.android.TreeTracker.models.organization.OrgOverrides
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import timber.log.Timber
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel

data class DevOptionsState(
    val params: List<Config> = emptyList(),
    val orgLink: OrgLinkOverridesState? = null,
)

data class OrgSettingRow(
    val id: String,
    val label: String,
    val orgDefault: Boolean,
    val override: Boolean? = null,
    val required: Boolean = false,
) {
    val value: Boolean get() = if (required) orgDefault else override ?: orgDefault
    val isOverridden: Boolean get() = !required && override != null
}

data class OrgLinkOverridesState(
    val orgId: String,
    val orgName: String,
    val enabled: Boolean,
    val setupSteps: List<OrgSettingRow>,
    val captureSteps: List<OrgSettingRow>,
    val features: List<OrgSettingRow>,
    val orgWalletId: String,
    val walletIdOverride: String?,
    val effectiveSetupFlow: List<String>,
    val effectiveCaptureFlow: List<String>,
    val hasOverrides: Boolean,
)

sealed class DevOptionsAction : Action {
    data class UpdateParam(
        val param: Config,
        val newValue: Any,
    ) : DevOptionsAction()

    object NavigateBack : DevOptionsAction()

    data class SetOrgOverridesEnabled(
        val enabled: Boolean,
    ) : DevOptionsAction()

    data class SetStepOverride(
        val flow: OrgFlow,
        val route: String,
        val enabled: Boolean?,
    ) : DevOptionsAction()

    data class SetFeatureOverride(
        val key: String,
        val enabled: Boolean?,
    ) : DevOptionsAction()

    data class SetWalletIdOverride(
        val walletId: String?,
    ) : DevOptionsAction()

    object ResetOrgOverrides : DevOptionsAction()
}

class DevOptionsViewModel(
    private val configurator: Configurator,
    private val convergenceConfiguration: ConvergenceConfiguration,
    private val orgRepo: OrgRepo,
    private val orgOverrideStore: OrgOverrideStore,
) : BaseViewModel<DevOptionsState, DevOptionsAction>(DevOptionsState()) {
    init {
        val updatedParams =
            ConfigKeys.configList.map { param ->
                when (param) {
                    is BooleanConfig ->
                        param.copy(
                            defaultValue = configurator.getBoolean(param),
                        )
                    is IntConfig ->
                        param.copy(
                            defaultValue = configurator.getInt(param),
                        )
                    is FloatConfig ->
                        param.copy(
                            defaultValue = configurator.getFloat(param),
                        )
                }
            }
        updateState { copy(params = updatedParams, orgLink = buildOrgLinkState()) }
    }

    override fun handleAction(action: DevOptionsAction) {
        when (action) {
            is DevOptionsAction.UpdateParam -> updateParam(action.param, action.newValue)
            is DevOptionsAction.NavigateBack -> popBackStack()
            is DevOptionsAction.SetOrgOverridesEnabled ->
                updateOrgOverrides { it.copy(enabled = action.enabled) }
            is DevOptionsAction.SetStepOverride ->
                updateOrgOverrides { it.withStep(action.flow, action.route, action.enabled) }
            is DevOptionsAction.SetFeatureOverride ->
                updateOrgOverrides { it.withFeature(action.key, action.enabled) }
            is DevOptionsAction.SetWalletIdOverride ->
                updateOrgOverrides { it.copy(walletId = action.walletId?.takeIf { id -> id.isNotBlank() }) }
            is DevOptionsAction.ResetOrgOverrides -> {
                orgOverrideStore.clear()
                updateState { copy(orgLink = buildOrgLinkState()) }
            }
        }
    }

    private fun updateOrgOverrides(transform: (OrgOverrides) -> OrgOverrides) {
        orgOverrideStore.update(transform)
        updateState { copy(orgLink = buildOrgLinkState()) }
    }

    private fun buildOrgLinkState(): OrgLinkOverridesState? {
        val org =
            try {
                orgRepo.baseCurrentOrg()
            } catch (e: IllegalStateException) {
                Timber.tag("DevOptions").w(e, "No current org, hiding org link overrides")
                return null
            }
        val overrides = orgOverrideStore.overrides.value
        val effective = overrides.applyTo(org)
        return OrgLinkOverridesState(
            orgId = org.id,
            orgName = org.name,
            enabled = overrides.enabled,
            setupSteps = stepRows(OrgFlow.SETUP, org.captureSetupFlow, overrides),
            captureSteps = stepRows(OrgFlow.CAPTURE, org.captureFlow, overrides),
            features = featureRows(org, overrides),
            orgWalletId = org.walletId,
            walletIdOverride = overrides.walletId,
            effectiveSetupFlow = effective.captureSetupFlow.map(::stepLabel),
            effectiveCaptureFlow = effective.captureFlow.map(::stepLabel),
            hasOverrides = overrides.hasOverrides,
        )
    }

    private fun stepRows(
        flow: OrgFlow,
        orgFlow: List<Destination>,
        overrides: OrgOverrides,
    ): List<OrgSettingRow> {
        val orgRoutes = orgFlow.map { RouteRegistry.normalize(it.route) }.toSet()
        return OrgOverrideCatalog.steps(flow).map { step ->
            OrgSettingRow(
                id = step.route,
                label = step.label,
                orgDefault = step.route in orgRoutes,
                override = overrides.steps(flow)[step.route],
                required = step.required,
            )
        }
    }

    private fun featureRows(
        org: Org,
        overrides: OrgOverrides,
    ): List<OrgSettingRow> =
        OrgOverrideCatalog.features.map { option ->
            val flow =
                when (option.flow) {
                    OrgFlow.SETUP -> org.captureSetupFlow
                    OrgFlow.CAPTURE -> org.captureFlow
                }
            val orgDefault =
                flow
                    .find { RouteRegistry.normalize(it.route) == option.route }
                    ?.features
                    ?.any { OrgFeature.fromKey(it) == option.feature }
                    ?: false
            OrgSettingRow(
                id = option.key,
                label = option.label,
                orgDefault = orgDefault,
                override = overrides.features[option.key],
            )
        }

    private fun stepLabel(destination: Destination): String {
        val route = RouteRegistry.normalize(destination.route)
        val label =
            (OrgOverrideCatalog.setupSteps + OrgOverrideCatalog.captureSteps)
                .find { it.route == route }
                ?.label ?: route
        return if (destination.features.isNullOrEmpty()) label else "$label (${destination.features.joinToString()})"
    }

    private fun updateParam(
        param: Config,
        newValue: Any,
    ) {
        configurator.putValue(param, newValue)
        updateState {
            val updatedParamList =
                params.updateListItem(param) {
                    when (this) {
                        is BooleanConfig -> copy(defaultValue = newValue as Boolean)
                        is IntConfig -> copy(defaultValue = newValue as Int)
                        is FloatConfig -> copy(defaultValue = newValue as Float)
                    }
                }
            copy(params = updatedParamList)
        }
    }

    override fun onCleared() {
        convergenceConfiguration.refreshConfig()
        super.onCleared()
    }
}

fun <T> List<T>.updateListItem(
    item: T,
    onUpdate: T.() -> T,
): List<T> = this.toMutableList().updateMutableItem(item, onUpdate)

fun <T> MutableList<T>.updateMutableItem(
    item: T,
    onUpdate: T.() -> T,
): List<T> {
    val indexToUpdate = this.indexOf(item)
    this[indexToUpdate]?.onUpdate()?.let { updatedItem ->
        this.removeAt(indexToUpdate)
        this.add(indexToUpdate, updatedItem)
    }
    return this
}