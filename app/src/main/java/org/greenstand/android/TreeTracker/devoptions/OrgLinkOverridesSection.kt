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
package org.greenstand.android.TreeTracker.devoptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.greenstand.android.TreeTracker.models.organization.OrgFlow
import org.greenstand.android.TreeTracker.view.AppColors
import org.greenstand.android.TreeTracker.view.TestTags

/**
 * The "Org Link Overrides" part of the dev options list. Each setting shows what the current
 * org's link (or the default org) sets, and a switch that overrides it. Overrides are saved
 * at once and apply while "Apply overrides" is on.
 */
fun LazyListScope.orgLinkOverridesSection(
    state: OrgLinkOverridesState,
    onHandleAction: (DevOptionsAction) -> Unit,
) {
    item {
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = AppColors.DeepGray,
        )
    }
    item { SectionHeader("Org Link Overrides") }
    item {
        Text(
            text = "Current org: ${state.orgName} (${state.orgId})",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            fontSize = 13.sp,
            color = AppColors.MediumGray,
        )
    }
    item {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Apply overrides", fontWeight = FontWeight.Bold)
                Text(
                    text = "Off uses the org's settings. Overrides are kept either way.",
                    fontSize = 12.sp,
                    color = AppColors.MediumGray,
                )
            }
            Switch(
                checked = state.enabled,
                onCheckedChange = { onHandleAction(DevOptionsAction.SetOrgOverridesEnabled(it)) },
                modifier = Modifier.testTag(TestTags.ORG_OVERRIDES_SWITCH),
                colors = switchColors(),
            )
        }
    }

    item { SubHeader("Setup flow") }
    items(state.setupSteps, key = { "setup-${it.id}" }) { row ->
        OverrideRow(row) { onHandleAction(DevOptionsAction.SetStepOverride(OrgFlow.SETUP, row.id, it)) }
    }

    item { SubHeader("Capture flow") }
    items(state.captureSteps, key = { "capture-${it.id}" }) { row ->
        OverrideRow(row) { onHandleAction(DevOptionsAction.SetStepOverride(OrgFlow.CAPTURE, row.id, it)) }
    }

    item { SubHeader("Features") }
    items(state.features, key = { "feature-${it.id}" }) { row ->
        OverrideRow(row) { onHandleAction(DevOptionsAction.SetFeatureOverride(row.id, it)) }
    }

    item { SubHeader("Wallet") }
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                text = "Org: ${state.orgWalletId.ifBlank { "(none)" }}",
                fontSize = 12.sp,
                color = AppColors.MediumGray,
            )
            TextField(
                value = state.walletIdOverride.orEmpty(),
                onValueChange = { onHandleAction(DevOptionsAction.SetWalletIdOverride(it)) },
                modifier = Modifier.fillMaxWidth().testTag(TestTags.ORG_OVERRIDE_WALLET),
                placeholder = { Text("Override wallet ID (blank follows org)") },
                singleLine = true,
            )
        }
    }

    item { SubHeader("Flows in use") }
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                text = "Setup: " + state.effectiveSetupFlow.joinToString(" → "),
                fontSize = 13.sp,
                modifier = Modifier.testTag(TestTags.ORG_OVERRIDES_SETUP_FLOW),
            )
            Text(
                text = "Capture: " + state.effectiveCaptureFlow.joinToString(" → "),
                fontSize = 13.sp,
                modifier = Modifier.testTag(TestTags.ORG_OVERRIDES_CAPTURE_FLOW),
            )
            Text(
                text = "Changes apply from the next capture session.",
                fontSize = 12.sp,
                color = AppColors.MediumGray,
            )
        }
    }
    item {
        TextButton(
            onClick = { onHandleAction(DevOptionsAction.ResetOrgOverrides) },
            enabled = state.hasOverrides || state.enabled,
            modifier = Modifier.padding(horizontal = 8.dp).testTag(TestTags.ORG_OVERRIDES_RESET),
        ) {
            Text("Reset all org overrides", color = AppColors.Orange)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = AppColors.SkyBlue,
    )
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}

/** [onSet] gets the new override, or null to follow the org again. */
@Composable
private fun OverrideRow(
    row: OrgSettingRow,
    onSet: (Boolean?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.label)
            Text(
                text =
                    when {
                        row.required -> "Required"
                        row.isOverridden -> "Org: ${onOff(row.orgDefault)} · Overridden: ${onOff(row.value)}"
                        else -> "Org: ${onOff(row.orgDefault)}"
                    },
                fontSize = 12.sp,
                color = if (row.isOverridden) AppColors.Orange else AppColors.MediumGray,
            )
        }
        if (row.isOverridden) {
            TextButton(
                onClick = { onSet(null) },
                modifier = Modifier.testTag(TestTags.orgOverrideReset(row.id)),
            ) {
                Text("Reset", fontSize = 12.sp)
            }
        }
        Switch(
            checked = row.value,
            enabled = !row.required,
            onCheckedChange = { onSet(it) },
            modifier = Modifier.testTag(TestTags.orgOverride(row.id)),
            colors = switchColors(),
        )
    }
}

private fun onOff(value: Boolean) = if (value) "On" else "Off"

@Composable
private fun switchColors() =
    SwitchDefaults.colors(
        checkedThumbColor = AppColors.Green,
        checkedTrackColor = AppColors.GreenShadow,
    )
