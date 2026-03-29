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
package org.greenstand.android.TreeTracker.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import com.github.takahirom.roborazzi.captureRoboImage
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    qualifiers = "w400dp-h800dp-xxhdpi",
    application = android.app.Application::class,
    sdk = [35],
)
abstract class ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    fun snapshot(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            ScreenshotTestDependencies {
                content()
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

}

@Composable
private fun ScreenshotTestDependencies(content: @Composable () -> Unit) {
    val navController = rememberNavController()
    CompositionLocalProvider(
        LocalNavHostController provides navController,
        LocalInspectionMode provides true,
    ) {
        CustomTheme {
            TreeTrackerTheme {
                content()
            }
        }
    }
}
