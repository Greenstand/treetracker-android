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
package org.greenstand.android.TreeTracker.uitest.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.mockk.mockk
import io.mockk.verify
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavEntryContentKey
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.MapRoute
import org.greenstand.android.TreeTracker.navigation.SettingsRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.rememberScreenTrackingNavEntryDecorator
import org.greenstand.android.TreeTracker.uitest.ComposeUiTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ScreenTrackingNavEntryDecoratorUiTest : ComposeUiTest() {
    private val exceptionDataCollector = mockk<ExceptionDataCollector>(relaxed = true)

    @Before
    fun startKoinWithCollector() {
        stopKoin()
        startKoin {
            modules(module { single { exceptionDataCollector } })
        }
    }

    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** A screen that shows its content key and double-fires a throttled navigation on click. */
    @Composable
    private fun ProbeScreen(name: String) {
        val navigator = LocalNavigator.current
        val contentKey = LocalNavEntryContentKey.current
        Column {
            BasicText("$name key=$contentKey")
            BasicText(
                text = "$name double tap",
                modifier =
                    Modifier.clickable {
                        navigator.throttledNavigate(SettingsRoute)
                        navigator.throttledNavigate(MapRoute)
                    },
            )
        }
    }

    private fun setNavDisplay() {
        setContent(inspectionMode = false) {
            NavDisplay(
                backStack = backStack,
                onBack = { navigator.popBackStack() },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberScreenTrackingNavEntryDecorator(),
                    ),
                entryProvider =
                    entryProvider {
                        entry<DashboardRoute> { ProbeScreen("Dashboard") }
                        entry<SettingsRoute> { ProbeScreen("Settings") }
                        entry<MapRoute> { ProbeScreen("Map") }
                        entry<TreeCaptureRoute> { ProbeScreen("Capture") }
                    },
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `reports the top entry as the current screen`() {
        setNavDisplay()

        verify { exceptionDataCollector.setScreen("DashboardRoute") }
    }

    @Test
    fun `reports arg routes by class name without their arguments`() {
        backStack.clear()
        backStack.add(TreeCaptureRoute(profilePicUrl = "pic"))

        setNavDisplay()

        verify { exceptionDataCollector.setScreen("TreeCaptureRoute") }
    }

    @Test
    fun `provides the entry content key to the screen`() {
        setNavDisplay()

        composeTestRule.onNodeWithText("Dashboard key=DashboardRoute").assertExists()
    }

    @Test
    fun `provides the full content key for arg routes`() {
        backStack.clear()
        backStack.add(TreeCaptureRoute(profilePicUrl = "pic"))

        setNavDisplay()

        composeTestRule.onNodeWithText("Capture key=TreeCaptureRoute(profilePicUrl=pic)").assertExists()
    }

    @Test
    fun `reports the new screen after navigating`() {
        setNavDisplay()

        composeTestRule.runOnIdle { navigator.navigate(SettingsRoute) }
        composeTestRule.waitForIdle()

        verify { exceptionDataCollector.setScreen("SettingsRoute") }
        composeTestRule.onNodeWithText("Settings key=SettingsRoute").assertExists()
    }

    @Test
    fun `screen navigator is scoped so a double tap navigates only once`() {
        setNavDisplay()

        composeTestRule.onNodeWithText("Dashboard double tap").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf(DashboardRoute, SettingsRoute), backStack.toList())
    }
}