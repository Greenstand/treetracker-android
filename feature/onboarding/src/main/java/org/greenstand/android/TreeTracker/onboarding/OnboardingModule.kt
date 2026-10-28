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
package org.greenstand.android.TreeTracker.onboarding

import org.greenstand.android.TreeTracker.languagepicker.LanguageSelectScreen
import org.greenstand.android.TreeTracker.navigation.LanguageRoute
import org.greenstand.android.TreeTracker.navigation.SignupFlowRoute
import org.greenstand.android.TreeTracker.navigation.SplashRoute
import org.greenstand.android.TreeTracker.signup.SignUpScreen
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.splash.SplashScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// Onboarding: splash, language picker and sign-up.
@OptIn(KoinExperimentalAPI::class)
val onboardingModule =
    module {
        viewModel { SignupViewModel(get(), get()) }

        navigation<SplashRoute> { route -> SplashScreen(orgId = route.orgId, orgName = route.orgName) }
        navigation<LanguageRoute> { route -> LanguageSelectScreen(route.isFromTopBar) }
        navigation<SignupFlowRoute> { _ -> SignUpScreen() }
    }