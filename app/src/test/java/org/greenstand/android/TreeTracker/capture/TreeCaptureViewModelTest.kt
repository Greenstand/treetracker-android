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
package org.greenstand.android.TreeTracker.capture

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.SessionTracker
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.location.LocationDataCapturer
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.getOrAwaitValueTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class TreeCaptureViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val profilePicUrl = ""

    @MockK(relaxed = true)
    private lateinit var userRepo: UserRepo

    @MockK(relaxed = true)
    private lateinit var treeCapturer: TreeCapturer

    @MockK(relaxed = true)
    private lateinit var sessionTracker: SessionTracker

    @MockK(relaxed = true)
    private lateinit var createFakeTreesUseCase: CreateFakeTreesUseCase

    @MockK(relaxed = true)
    private lateinit var locationDataCapturer: LocationDataCapturer

    private lateinit var treeCaptureViewModel: TreeCaptureViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser

        treeCaptureViewModel = TreeCaptureViewModel(
            profilePicUrl = profilePicUrl,
            userRepo = userRepo,
            treeCapturer = treeCapturer,
            sessionTracker = sessionTracker,
            createFakeTreesUseCase = createFakeTreesUseCase,
            locationDataCapturer = locationDataCapturer
        )
    }

    @Test
    fun `WHEN numberOfTrees greater than 0 THEN firstTrack is false `() = runBlocking {
        coEvery { userRepo.getPowerUser() } returns User(
            id = 5,
            wallet = "",
            numberOfTrees = 5,
            firstName = "",
            lastName = "",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false
        )

        val result = treeCaptureViewModel.isFirstTrack()
        assertFalse(result)
    }

    @Test
    fun `WHEN numberOfTrees less than 1 THEN firstTrack is true `() = runBlocking {
        coEvery { userRepo.getPowerUser() } returns User(
            id = 5,
            wallet = "",
            numberOfTrees = 0,
            firstName = "",
            lastName = "",
            photoPath = "",
            isPowerUser = false,
            unreadMessagesAvailable = false
        )

        val result = treeCaptureViewModel.isFirstTrack()
        assertTrue(result)
    }

    @Test
    fun `WHEN location coordinate is available THEN isLocationAvailable state true AND isGettingLocation state always false `() =
        runBlocking {
            coEvery { treeCapturer.pinLocation() } returns true

            treeCaptureViewModel.captureLocation()

            assertTrue(
                treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable ?: false
            )
            assertFalse(treeCaptureViewModel.state.getOrAwaitValueTest().isGettingLocation)
        }

    @Test
    fun `WHEN updateBadGpsDialogState true THEN isLocationAvailable state true`() = runBlocking {
        treeCaptureViewModel.updateBadGpsDialogState(true)
        assertTrue(treeCaptureViewModel.state.getOrAwaitValueTest().isLocationAvailable ?: false)
    }

    @Test
    fun `WHEN updateCaptureTutorialDialog true THEN showCaptureTutorial state true`() = runBlocking {
        treeCaptureViewModel.updateCaptureTutorialDialog(true)
        assertTrue(treeCaptureViewModel.state.getOrAwaitValueTest().showCaptureTutorial ?: false)
    }

    @Test
    fun `WHEN create fake trees THEN createFakeTreesUseCase is called 1 time AND isCreatingFakeTrees state always false`() = runBlocking {
        treeCaptureViewModel.createFakeTrees()

        coVerify(exactly = 1) { createFakeTreesUseCase.execute(CreateFakeTreesParams(50)) }
        assertFalse(treeCaptureViewModel.state.getOrAwaitValueTest().isCreatingFakeTrees)
    }
}