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
package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.capture.captureModule
import org.greenstand.android.TreeTracker.dashboard.dashboardModule
import org.greenstand.android.TreeTracker.devtools.devToolsModule
import org.greenstand.android.TreeTracker.messages.messagesModule
import org.greenstand.android.TreeTracker.onboarding.onboardingModule
import org.greenstand.android.TreeTracker.settings.settingsModule
import org.greenstand.android.TreeTracker.trees.treesModule
import org.greenstand.android.TreeTracker.ui.uiModule

// Every feature's Koin module. Each one registers its own ViewModels and the screens for its
// routes; Host draws whatever screen is registered for the route on top of the back stack.
val featureModules =
    listOf(
        uiModule,
        onboardingModule,
        dashboardModule,
        captureModule,
        treesModule,
        settingsModule,
        messagesModule,
        devToolsModule,
    )