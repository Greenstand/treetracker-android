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
package org.greenstand.android.TreeTracker.uitest.profile

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.messages.MessagesRepo
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupData
import org.greenstand.android.TreeTracker.models.setupflow.CaptureSetupScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.preferences.Preferences
import org.greenstand.android.TreeTracker.uitest.support.RootScreenUiTest
import org.greenstand.android.TreeTracker.userselect.UserSelectViewModel
import org.junit.After
import org.junit.Before
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shared setup for the profile root screens, which all run on a [UserSelectViewModel]
 * built from mocked repositories.
 */
abstract class UserSelectRootUiTest : RootScreenUiTest() {
    val userRepo = mockk<UserRepo>(relaxed = true)
    val messagesRepo = mockk<MessagesRepo>(relaxed = true)
    val locationDataCapturer = mockk<LocationDataCapturer>(relaxed = true)
    val preferences = mockk<Preferences>(relaxed = true)
    val captureSetupData = mockk<CaptureSetupData>(relaxed = true)

    val planter =
        User(
            id = 3L,
            wallet = "0712345678",
            numberOfTrees = 12,
            firstName = "Jane",
            lastName = "Doe",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false,
        )

    @Before
    fun setUpUserSelect() {
        mockkObject(CaptureSetupScopeManager)
        every { CaptureSetupScopeManager.open() } just Runs
        every { CaptureSetupScopeManager.getData() } returns captureSetupData
        every { userRepo.users() } returns flowOf(listOf(planter))
        coEvery { userRepo.getUser(planter.id) } returns planter
    }

    @After
    fun tearDownUserSelect() {
        unmockkObject(CaptureSetupScopeManager)
    }

    /** Binds the repositories `UserSelectViewModelFactory` resolves from Koin. */
    fun repositoriesModule(): Module =
        module {
            single { userRepo }
            single { messagesRepo }
            single { locationDataCapturer }
            single { preferences }
        }

    /** Binds a [UserSelectViewModel] for screens that resolve it through `LocalViewModelFactory`. */
    fun userSelectViewModelModule(): Module {
        val viewModel =
            UserSelectViewModel(
                userId = null,
                userRepo = userRepo,
                messageRepo = messagesRepo,
                locationDataCapturer = locationDataCapturer,
                prefs = preferences,
            )
        return module { single { viewModel } }
    }
}