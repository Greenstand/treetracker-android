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
package org.greenstand.android.TreeTracker.camera

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.user.User
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SelfieViewModelTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val userRepo = mockk<UserRepo>(relaxed = true)

    private val admin =
        User(
            id = 1,
            wallet = "admin",
            numberOfTrees = 0,
            firstName = "Ada",
            lastName = "Admin",
            photoPath = "",
            isPowerUser = true,
            unreadMessagesAvailable = false,
        )

    @Test
    fun `first sign-up on the device shows the tutorial`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null
            val viewModel = SelfieViewModel(userRepo)
            assertEquals(true, viewModel.state.value.showSelfieTutorial)
        }

    @Test
    fun `later sign-ups don't show the tutorial`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns admin
            val viewModel = SelfieViewModel(userRepo)
            assertEquals(false, viewModel.state.value.showSelfieTutorial)
        }

    @Test
    fun `update selfie tutorial dialog state = false, returns false`() =
        runTest {
            val viewModel = SelfieViewModel(userRepo)
            viewModel.handleAction(SelfieAction.UpdateSelfieTutorialDialog(false))
            assertEquals(false, viewModel.state.value.showSelfieTutorial)
        }

    @Test
    fun `update selfie tutorial dialog state = true, returns true`() =
        runTest {
            val viewModel = SelfieViewModel(userRepo)
            viewModel.handleAction(SelfieAction.UpdateSelfieTutorialDialog(true))
            assertEquals(true, viewModel.state.value.showSelfieTutorial)
        }
}