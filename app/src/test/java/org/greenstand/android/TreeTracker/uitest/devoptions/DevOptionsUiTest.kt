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
package org.greenstand.android.TreeTracker.uitest.devoptions

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.DevOptionsAction
import org.greenstand.android.TreeTracker.devoptions.DevOptionsScreen
import org.greenstand.android.TreeTracker.devoptions.DevOptionsState
import org.greenstand.android.TreeTracker.devoptions.OrgLinkOverridesState
import org.greenstand.android.TreeTracker.devoptions.OrgSettingRow
import org.greenstand.android.TreeTracker.models.organization.OrgFlow
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.overlay.DebugOverlayManager
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.greenstand.android.TreeTracker.view.ARROW_BACK_BUTTON_TAG
import org.greenstand.android.TreeTracker.view.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevOptionsUiTest : ComposeUiTest() {
    private val actions = mutableListOf<DevOptionsAction>()

    private fun setDevOptions(
        state: DevOptionsState,
        overlayManager: DebugOverlayManager? = null,
    ) {
        setContent {
            DevOptionsScreen(
                state = state,
                overlayManager = overlayManager,
                onHandleAction = { actions += it },
            )
        }
    }

    private fun switches() = composeTestRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))

    private fun checkbox() = composeTestRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))

    @Test
    fun `shows the overlay toggles`() {
        setDevOptions(DevOptionsState())

        composeTestRule.onNodeWithText("Debug Overlays").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Sync Overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Sensor Overlay").assertIsDisplayed()
    }

    @Test
    fun `shows every config param`() {
        setDevOptions(DevOptionsState(params = ConfigKeys.configList))

        ConfigKeys.configList.forEach { config ->
            composeTestRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(config.name))
            composeTestRule.onNodeWithText(config.name).assertIsDisplayed()
        }
    }

    @Test
    fun `overlay switches are off without an overlay manager`() {
        setDevOptions(DevOptionsState())

        switches()[0].performClick()

        switches()[0].assertIsOff()
        switches()[1].assertIsOff()
    }

    @Test
    fun `sync overlay switch turns on the sync overlay`() {
        val overlayManager = DebugOverlayManager()
        setDevOptions(DevOptionsState(), overlayManager)

        switches()[0].performClick()

        assertTrue(overlayManager.showSyncOverlay.value)
        assertFalse(overlayManager.showSensorOverlay.value)
        switches()[0].assertIsOn()
    }

    @Test
    fun `sensor overlay switch turns on the sensor overlay`() {
        val overlayManager = DebugOverlayManager()
        setDevOptions(DevOptionsState(), overlayManager)

        switches()[1].performClick()

        assertTrue(overlayManager.showSensorOverlay.value)
        assertFalse(overlayManager.showSyncOverlay.value)
        switches()[1].assertIsOn()
    }

    @Test
    fun `overlay switches reflect the overlay manager`() {
        val overlayManager = DebugOverlayManager()
        overlayManager.setSyncOverlay(true)
        setDevOptions(DevOptionsState(), overlayManager)

        switches()[0].assertIsOn()
        switches()[1].assertIsOff()
    }

    @Test
    fun `turning off an active overlay switch hides the overlay`() {
        val overlayManager = DebugOverlayManager()
        overlayManager.setSensorOverlay(true)
        setDevOptions(DevOptionsState(), overlayManager)

        switches()[1].performClick()

        assertFalse(overlayManager.showSensorOverlay.value)
    }

    @Test
    fun `boolean param checkbox reflects its value`() {
        setDevOptions(DevOptionsState(params = listOf(ConfigKeys.FORCE_IMAGE_SIZE.copy(defaultValue = true))))

        checkbox().assertIsOn()
    }

    @Test
    fun `checking a boolean param sends UpdateParam`() {
        setDevOptions(DevOptionsState(params = listOf(ConfigKeys.FORCE_IMAGE_SIZE)))
        checkbox().assertIsOff()

        checkbox().performClick()

        assertEquals(listOf<DevOptionsAction>(DevOptionsAction.UpdateParam(ConfigKeys.FORCE_IMAGE_SIZE, true)), actions)
    }

    @Test
    fun `int param shows its value and sends valid numbers`() {
        setDevOptions(DevOptionsState(params = listOf(ConfigKeys.IMAGE_CAPTURE_HEIGHT)))

        composeTestRule.onNode(hasSetTextAction()).assertExists()
        composeTestRule.onNodeWithText("1920").assertExists()
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("100")

        assertEquals(listOf<DevOptionsAction>(DevOptionsAction.UpdateParam(ConfigKeys.IMAGE_CAPTURE_HEIGHT, 100)), actions)
    }

    @Test
    fun `int param ignores text that is not a number`() {
        setDevOptions(DevOptionsState(params = listOf(ConfigKeys.IMAGE_CAPTURE_HEIGHT)))

        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("abc")

        assertTrue(actions.isEmpty())
        composeTestRule.onNodeWithText("abc").assertExists()
    }

    @Test
    fun `float param sends valid numbers`() {
        setDevOptions(DevOptionsState(params = listOf(ConfigKeys.LAT_STD_DEV_THRESHOLD)))

        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("0.5")

        assertEquals(listOf<DevOptionsAction>(DevOptionsAction.UpdateParam(ConfigKeys.LAT_STD_DEV_THRESHOLD, 0.5f)), actions)
    }

    @Test
    fun `float param ignores text that is not a number`() {
        setDevOptions(DevOptionsState(params = listOf(ConfigKeys.LAT_STD_DEV_THRESHOLD)))

        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("x")

        assertTrue(actions.isEmpty())
    }

    @Test
    fun `back sends NavigateBack`() {
        setDevOptions(DevOptionsState())

        composeTestRule.onNodeWithTag(ARROW_BACK_BUTTON_TAG).performClick()

        assertEquals(listOf<DevOptionsAction>(DevOptionsAction.NavigateBack), actions)
    }

    private val orgLinkState =
        OrgLinkOverridesState(
            orgId = "42",
            orgName = "Kasiki Hai",
            enabled = true,
            setupSteps =
                listOf(
                    OrgSettingRow(RouteRegistry.ROUTE_USER_SELECT, "User select", orgDefault = true, required = true),
                    OrgSettingRow(RouteRegistry.ROUTE_SESSION_NOTE, "Session note", orgDefault = false),
                ),
            captureSteps =
                listOf(
                    OrgSettingRow(RouteRegistry.ROUTE_TREE_HEIGHT, "Tree height", orgDefault = true, override = false),
                ),
            features = emptyList(),
            orgWalletId = "org-wallet",
            walletIdOverride = null,
            effectiveSetupFlow = listOf("User select"),
            effectiveCaptureFlow = listOf("Tree capture"),
            hasOverrides = true,
        )

    private fun scrollTo(tag: String) {
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun `org link overrides are hidden without an org`() {
        setDevOptions(DevOptionsState())

        composeTestRule.onNodeWithText("Org Link Overrides").assertDoesNotExist()
    }

    @Test
    fun `org link overrides show the org value and the override`() {
        setDevOptions(DevOptionsState(orgLink = orgLinkState))

        composeTestRule.onNodeWithText("Current org: Kasiki Hai (42)").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.ORG_OVERRIDES_SWITCH).assertIsOn()
        scrollTo(TestTags.orgOverride(RouteRegistry.ROUTE_USER_SELECT))
        composeTestRule.onNodeWithTag(TestTags.orgOverride(RouteRegistry.ROUTE_USER_SELECT)).assertIsOn().assertIsNotEnabled()
        scrollTo(TestTags.orgOverride(RouteRegistry.ROUTE_TREE_HEIGHT))
        composeTestRule.onNodeWithTag(TestTags.orgOverride(RouteRegistry.ROUTE_TREE_HEIGHT)).assertIsOff()
        composeTestRule.onNodeWithText("Org: On · Overridden: Off").assertIsDisplayed()
    }

    @Test
    fun `org link override switches and reset send their actions`() {
        setDevOptions(DevOptionsState(orgLink = orgLinkState))

        composeTestRule.onNodeWithTag(TestTags.ORG_OVERRIDES_SWITCH).performClick()
        scrollTo(TestTags.orgOverride(RouteRegistry.ROUTE_SESSION_NOTE))
        composeTestRule.onNodeWithTag(TestTags.orgOverride(RouteRegistry.ROUTE_SESSION_NOTE)).performClick()
        scrollTo(TestTags.orgOverrideReset(RouteRegistry.ROUTE_TREE_HEIGHT))
        composeTestRule.onNodeWithTag(TestTags.orgOverrideReset(RouteRegistry.ROUTE_TREE_HEIGHT)).performClick()
        scrollTo(TestTags.ORG_OVERRIDES_RESET)
        composeTestRule.onNodeWithTag(TestTags.ORG_OVERRIDES_RESET).performClick()

        assertEquals(
            listOf(
                DevOptionsAction.SetOrgOverridesEnabled(false),
                DevOptionsAction.SetStepOverride(OrgFlow.SETUP, RouteRegistry.ROUTE_SESSION_NOTE, true),
                DevOptionsAction.SetStepOverride(OrgFlow.CAPTURE, RouteRegistry.ROUTE_TREE_HEIGHT, null),
                DevOptionsAction.ResetOrgOverrides,
            ),
            actions,
        )
    }
}