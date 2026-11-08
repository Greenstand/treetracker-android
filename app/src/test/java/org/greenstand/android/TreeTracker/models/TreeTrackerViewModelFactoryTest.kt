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
package org.greenstand.android.TreeTracker.models

import androidx.lifecycle.ViewModel
import io.mockk.mockk
import org.greenstand.android.TreeTracker.capture.TreeImageReviewViewModel
import org.greenstand.android.TreeTracker.dashboard.DashboardViewModel
import org.greenstand.android.TreeTracker.devoptions.DevOptionsViewModel
import org.greenstand.android.TreeTracker.languagepicker.LanguagePickerViewModel
import org.greenstand.android.TreeTracker.map.MapViewModel
import org.greenstand.android.TreeTracker.messages.ChatViewModel
import org.greenstand.android.TreeTracker.messages.individualmeassagelist.IndividualMessageListViewModel
import org.greenstand.android.TreeTracker.orgpicker.AddOrgViewModel
import org.greenstand.android.TreeTracker.orgpicker.OrgPickerViewModel
import org.greenstand.android.TreeTracker.permissions.PermissionViewModel
import org.greenstand.android.TreeTracker.sessionnote.SessionNoteViewModel
import org.greenstand.android.TreeTracker.settings.SettingsViewModel
import org.greenstand.android.TreeTracker.signup.SignupViewModel
import org.greenstand.android.TreeTracker.splash.SplashScreenViewModel
import org.greenstand.android.TreeTracker.treeheight.TreeHeightSelectionViewModel
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.WalletSelectViewModel
import org.greenstand.android.TreeTracker.walletselect.addwallet.AddWalletViewModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TreeTrackerViewModelFactoryTest {
    /** One mock per view model class the factory knows how to build, registered in Koin. */
    private val viewModels: Map<KClass<out ViewModel>, ViewModel> =
        mapOf(
            UserSelectViewModel::class to mockk<UserSelectViewModel>(),
            DashboardViewModel::class to mockk<DashboardViewModel>(),
            SignupViewModel::class to mockk<SignupViewModel>(),
            LanguagePickerViewModel::class to mockk<LanguagePickerViewModel>(),
            OrgPickerViewModel::class to mockk<OrgPickerViewModel>(),
            WalletSelectViewModel::class to mockk<WalletSelectViewModel>(),
            AddWalletViewModel::class to mockk<AddWalletViewModel>(),
            SplashScreenViewModel::class to mockk<SplashScreenViewModel>(),
            TreeImageReviewViewModel::class to mockk<TreeImageReviewViewModel>(),
            PermissionViewModel::class to mockk<PermissionViewModel>(),
            IndividualMessageListViewModel::class to mockk<IndividualMessageListViewModel>(),
            TreeHeightSelectionViewModel::class to mockk<TreeHeightSelectionViewModel>(),
            AddOrgViewModel::class to mockk<AddOrgViewModel>(),
            SessionNoteViewModel::class to mockk<SessionNoteViewModel>(),
            DevOptionsViewModel::class to mockk<DevOptionsViewModel>(),
            SettingsViewModel::class to mockk<SettingsViewModel>(),
            MapViewModel::class to mockk<MapViewModel>(),
        )

    private val factory = TreeTrackerViewModelFactory()

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    register<UserSelectViewModel>()
                    register<DashboardViewModel>()
                    register<SignupViewModel>()
                    register<LanguagePickerViewModel>()
                    register<OrgPickerViewModel>()
                    register<WalletSelectViewModel>()
                    register<AddWalletViewModel>()
                    register<SplashScreenViewModel>()
                    register<TreeImageReviewViewModel>()
                    register<PermissionViewModel>()
                    register<IndividualMessageListViewModel>()
                    register<TreeHeightSelectionViewModel>()
                    register<AddOrgViewModel>()
                    register<SessionNoteViewModel>()
                    register<DevOptionsViewModel>()
                    register<SettingsViewModel>()
                    register<MapViewModel>()
                },
            )
        }
    }

    private inline fun <reified T : ViewModel> Module.register() {
        single { viewModels.getValue(T::class) as T }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `create returns the Koin instance for every supported view model`() {
        viewModels.forEach { (type, instance) ->
            assertSame(instance, factory.create(type.java), "Wrong instance for ${type.simpleName}")
        }
    }

    @Test
    fun `create returns an instance of the requested class`() {
        viewModels.keys.forEach { type ->
            assertTrue(type.isInstance(factory.create(type.java)), "Wrong type for ${type.simpleName}")
        }
    }

    @Test
    fun `create throws a helpful error for a view model the factory does not know`() {
        val error =
            assertFailsWith<RuntimeException> {
                factory.create(ChatViewModel::class.java)
            }

        assertTrue(error.message!!.contains("ChatViewModel"))
        assertTrue(error.message!!.contains("TreeTrackerViewModelFactory"))
    }
}