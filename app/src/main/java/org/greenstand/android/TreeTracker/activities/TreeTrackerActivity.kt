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
package org.greenstand.android.TreeTracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.ExperimentalComposeApi
import org.greenstand.android.TreeTracker.models.LanguageSwitcher
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.greenstand.android.TreeTracker.utilities.GpsUtils
import org.greenstand.android.TreeTracker.view.NoGPSDeviceDialog
import org.koin.android.ext.android.inject

class TreeTrackerActivity : ComponentActivity() {

    private val languageSwitcher: LanguageSwitcher by inject()
    private val viewModelFactory: TreeTrackerViewModelFactory by inject()
    private val gpsUtils: GpsUtils by inject()

    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.argb(128, 0, 0, 0) // 50% black scrim
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.argb(128, 0, 0, 0)
            )
        )

        languageSwitcher.applyCurrentLanguage(this)

        setContent {
            CustomTheme {
                if (gpsUtils.hasGPSDevice()) {
                    Root(viewModelFactory)
                } else {
                    NoGPSDeviceDialog(onPositiveClick = { finishAndRemoveTask() })
                }
            }
        }
    }
}