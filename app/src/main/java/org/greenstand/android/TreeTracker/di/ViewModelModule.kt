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
package org.greenstand.android.TreeTracker.di

import org.greenstand.android.TreeTracker.capture.TreeImageReviewViewModel
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.map.MapViewModel
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.messages.announcementmessage.AnnouncementViewModel
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.settings.SettingsViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {

        viewModel { SessionNoteViewModel() }

        viewModel { AddWalletViewModel() }

        viewModel { AddOrgViewModel(get(), get()) }

        viewModel { LanguagePickerViewModel(get()) }

        viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }

        viewModel { OrgPickerViewModel(get()) }

        viewModel { UserSelectViewModel(null, get(), get(), get(), get()) }

        viewModel { DevOptionsViewModel(get(), get()) }

        viewModel { TreeHeightSelectionViewModel(get()) }

        viewModel { SignupViewModel(get(), get()) }

        viewModel { WalletSelectViewModel(get()) }

        viewModel { IndividualMessageListViewModel(get(), get(), get()) }

        viewModel { ChatViewModel(get(), get(), get(), get()) }

        viewModel { AnnouncementViewModel(get(), get()) }

        viewModel { TreeImageReviewViewModel(get(), get(), get()) }

        viewModel { PermissionViewModel(get()) }

        viewModel { SettingsViewModel(get()) }

        viewModel { MapViewModel(get()) }
    }