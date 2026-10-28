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
package org.greenstand.android.TreeTracker.uitest

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.LocalNavigator
import org.greenstand.android.TreeTracker.navigation.Navigator
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.greenstand.android.TreeTracker.viewmodel.LocalSnackbarController
import org.greenstand.android.TreeTracker.viewmodel.SnackbarController
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Base class for Robolectric Compose UI tests.
 *
 * Renders content inside the same theme and CompositionLocals the app provides, with a
 * real [Navigator] over [backStack] so tests can assert where a click navigated to.
 * The navigator's clock moves forward on every read, so the unscoped time throttle never
 * drops a navigation in tests.
 *
 * These tests only run in `testDebugUnitTest` (see app/build.gradle).
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    qualifiers = "w400dp-h800dp-xxhdpi",
    application = android.app.Application::class,
    sdk = [35],
)
abstract class ComposeUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var clockMs = 0L

    /** The back stack behind [navigator]. Starts with [DashboardRoute]; reset it in a test if needed. */
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(DashboardRoute)

    val navigator: Navigator =
        Navigator(
            backStack = backStack,
            clock = {
                clockMs += CLOCK_STEP_MS
                clockMs
            },
        )

    val snackbarController = SnackbarController()

    /**
     * Sets [content] inside the app theme, [LocalNavigator] and [LocalSnackbarController].
     *
     * [inspectionMode] defaults to true, like the screenshot tests, so shared components
     * that would otherwise resolve a ViewModel (e.g. `LanguageButton`) render statically.
     * Pass false when testing a root screen with a ViewModel passed in explicitly.
     */
    fun setContent(
        inspectionMode: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalSnackbarController provides snackbarController,
                LocalInspectionMode provides inspectionMode,
            ) {
                CustomTheme {
                    TreeTrackerTheme {
                        content()
                    }
                }
            }
        }
    }

    /** Resolves a string resource the same way the UI does. */
    fun string(
        @StringRes id: Int,
        vararg args: Any,
    ): String {
        val context = RuntimeEnvironment.getApplication()
        return if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
    }

    private companion object {
        const val CLOCK_STEP_MS = 1_000L
    }
}