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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.appfunctions.AgentAccountCreator
import org.greenstand.android.TreeTracker.appfunctions.AgentTreeCapturer
import org.greenstand.android.TreeTracker.models.DeviceConfigUpdater
import org.greenstand.android.TreeTracker.models.DeviceOrientation
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.StepCounter
import org.greenstand.android.TreeTracker.models.TreeTrackerViewModelFactory
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.organization.OrgRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.DevOptionsRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.root.Root
import org.greenstand.android.TreeTracker.theme.CustomTheme
import org.koin.android.ext.android.get
import timber.log.Timber
import java.io.File

/** Debug-only fixture and screen links for E2E tests. Renders the real app's Host and screens. */
class E2eActivity : AppCompatActivity() {
    @OptIn(ExperimentalComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uri = intent.data
        if (uri?.scheme != "treetracker-e2e" || uri.host !in setOf("setup", "dashboard", "dev-options", "capture")) {
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                if (uri.host == "setup") {
                    // API 35 has no app_function shell command. Reuse its underlying helpers.
                    withContext(Dispatchers.IO) {
                        AgentAccountCreator(get(), get())
                            .createAccount("Ada", "Admin", "5551230001", null)
                        AgentTreeCapturer(get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
                            .captureTrees(2, "5551230001", -3.5, 37.3, "E2E dev tools")
                        File(filesDir, SETUP_RESULT).writeText("ready")
                    }
                    finish()
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    get<OrgRepo>().ensureInitialized()
                    get<DeviceConfigUpdater>().saveLatestConfig()
                    checkNotNull(get<UserRepo>().getPowerUser()) { "Run e2e/setup-device.sh first" }
                }
                val routes =
                    when (uri.host) {
                        "dev-options" -> listOf(DashboardRoute, DevOptionsRoute)
                        "capture" -> captureBackStack()
                        else -> listOf(DashboardRoute)
                    }
                val factory = get<TreeTrackerViewModelFactory>()
                setContent {
                    CustomTheme { Root(factory, initialBackStack = routes) }
                }
                window.decorView.setViewTreeNavigationEventDispatcherOwner(this@E2eActivity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "E2E link failed")
                if (uri.host == "setup") {
                    withContext(Dispatchers.IO) {
                        File(filesDir, SETUP_RESULT).writeText("error: ${e.message}")
                    }
                }
                finish()
            }
        }
    }

    private suspend fun captureBackStack(): List<NavKey> {
        val user = withContext(Dispatchers.IO) { checkNotNull(get<UserRepo>().getPowerUser()) }
        CaptureSetupScopeManager.open()
        try {
            CaptureSetupScopeManager.getData().apply {
                this.user = user
                organizationName = get<OrgRepo>().currentOrg().name
            }
            get<Preferences>().setUserId(user.id)
            get<SessionTracker>().startSession()
            get<StepCounter>().enable()
            get<DeviceOrientation>().enable()
            get<LocationDataCapturer>().startGpsUpdates()
        } finally {
            CaptureSetupScopeManager.close()
        }
        // TreeCaptureViewModelFactory opens the capture scope, as in normal navigation.
        return listOf(DashboardRoute, TreeCaptureRoute(user.photoPath))
    }

    companion object {
        const val SETUP_RESULT = "e2e-setup-result.txt"
    }
}