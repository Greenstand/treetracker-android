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
package org.greenstand.android.TreeTracker.capture

import org.greenstand.android.TreeTracker.navigation.AddOrgRoute
import org.greenstand.android.TreeTracker.navigation.AddWalletRoute
import org.greenstand.android.TreeTracker.navigation.OrgRoute
import org.greenstand.android.TreeTracker.navigation.SessionNoteRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.TreeHeightScreenRoute
import org.greenstand.android.TreeTracker.navigation.TreeImageReviewRoute
import org.greenstand.android.TreeTracker.navigation.UserSelectRoute
import org.greenstand.android.TreeTracker.navigation.WalletSelectRoute
import org.greenstand.android.TreeTracker.orgpicker.AddOrgScreen
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerScreen
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteScreen
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.treeheight.TreeHeightScreen
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectScreen
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletScreen
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// Tree capture: org and wallet setup, session note, capture, image review and tree height.
@OptIn(KoinExperimentalAPI::class, ExperimentalPermissionsApi::class)
val captureModule =
    module {
        viewModel { OrgPickerViewModel(get()) }
        viewModel { AddOrgViewModel(get(), get()) }
        viewModel { WalletSelectViewModel(get()) }
        viewModel { AddWalletViewModel() }
        viewModel { SessionNoteViewModel() }
        viewModel { TreeImageReviewViewModel(get(), get(), get()) }
        viewModel { TreeHeightSelectionViewModel(get()) }
        viewModel { PermissionViewModel(get()) }

        navigation<OrgRoute> { _ -> OrgPickerScreen() }
        navigation<AddOrgRoute> { _ -> AddOrgScreen() }
        navigation<UserSelectRoute> { _ -> UserSelectScreen() }
        navigation<WalletSelectRoute> { _ -> WalletSelectScreen() }
        navigation<AddWalletRoute> { _ -> AddWalletScreen() }
        navigation<SessionNoteRoute> { _ -> SessionNoteScreen() }
        navigation<TreeHeightScreenRoute> { _ -> TreeHeightScreen() }
        navigation<TreeCaptureRoute> { route -> TreeCaptureScreen(route.profilePicUrl) }
        navigation<TreeImageReviewRoute> { _ -> TreeImageReviewScreen() }
    }