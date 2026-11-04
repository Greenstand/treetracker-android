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
package org.greenstand.android.TreeTracker.e2e

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.TreeTrackerActivity
import org.greenstand.android.TreeTracker.models.Language
import org.greenstand.android.TreeTracker.view.ARROW_FORWARD_BUTTON_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * End-to-end check of the real app on a device: a fresh install, with its permissions granted,
 * boots through the splash screen to the language picker, and picking English leads to the
 * sign-up screen. Runs in CI on an emulator (.github/workflows/android-e2e.yml).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class FirstLaunchE2eTest {
    private val permissions = GrantPermissionRule.grant(*requiredPermissions())

    // A StandardTestDispatcher queues effects for the test clock, which runs them on the main
    // thread as in production. The rule's default unconfined dispatcher resumes a coroutine on
    // whatever thread finished the work (a Room thread here), so the splash screen would start
    // GPS updates off the main thread and crash.
    private val composeRule = createAndroidComposeRule<TreeTrackerActivity>(effectContext = StandardTestDispatcher())

    // Permissions first, so the splash screen's request comes back granted at once.
    @get:Rule
    val rules: TestRule = RuleChain.outerRule(permissions).around(composeRule)

    @Test
    fun freshInstallReachesSignUpAfterPickingALanguage() {
        val english = Language.ENGLISH.toString()
        val email = composeRule.activity.getString(R.string.email_placeholder)

        // The splash screen bootstraps the app, then opens the language picker.
        composeRule.waitUntilAtLeastOneExists(hasText(english), BOOT_TIMEOUT_MS)

        composeRule.onNodeWithText(english).performClick()
        composeRule.onNodeWithTag(ARROW_FORWARD_BUTTON_TAG).performClick()

        // Setting the language can recreate the activity; the back stack survives it.
        composeRule.waitUntilAtLeastOneExists(hasText(email, ignoreCase = true), SCREEN_TIMEOUT_MS)
    }

    private companion object {
        const val BOOT_TIMEOUT_MS = 30_000L
        const val SCREEN_TIMEOUT_MS = 15_000L

        fun requiredPermissions(): Array<String> {
            val permissions =
                mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            return permissions.toTypedArray()
        }
    }
}