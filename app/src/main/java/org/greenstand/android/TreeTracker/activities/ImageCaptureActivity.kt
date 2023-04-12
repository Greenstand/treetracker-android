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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.greenstand.android.TreeTracker.models.NavRoute
import org.greenstand.android.TreeTracker.root.LocalNavHostController
import org.greenstand.android.TreeTracker.root.addNavRoute
import org.greenstand.android.TreeTracker.view.TreeTrackerTheme


class CaptureImageContract : ActivityResultContract<Boolean, String?>() {

    companion object {
        const val SELFIE_MODE = "SELFIE_MODE"
        const val TAKEN_IMAGE_PATH = "TAKEN_IMAGE_PATH"
    }

    override fun createIntent(context: Context, selfieMode: Boolean): Intent {
        return Intent(context, ImageCaptureActivity::class.java).apply {
            putExtra(SELFIE_MODE, selfieMode)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode == Activity.RESULT_OK) {
            return intent?.getStringExtra(TAKEN_IMAGE_PATH)
        }
        return null
    }

}

class ImageCaptureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalNavHostController provides navController
            ) {
                TreeTrackerTheme {
                    NavHost(navController, startDestination = NavRoute.Selfie.route) {
                        listOf(
                            NavRoute.Selfie,
                            NavRoute.ImageReview,
                        ).forEach { addNavRoute(it) }
                    }
                }
            }
        }
    }
}
