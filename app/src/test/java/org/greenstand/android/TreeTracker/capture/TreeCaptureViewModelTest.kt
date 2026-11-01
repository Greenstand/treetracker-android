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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.devoptions.ConfigKeys
import org.greenstand.android.TreeTracker.devoptions.Configurator
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.user.User
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.TreeImageReviewRoute
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesParams
import org.greenstand.android.TreeTracker.usecases.CreateFakeTreesUseCase
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.NavigationEvent
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    private lateinit var createFakeTreesUseCase: CreateFakeTreesUseCase

    @MockK(relaxed = true)
    private lateinit var configurator: Configurator

    private lateinit var treeCaptureViewModel: TreeCaptureViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser
        mockkObject(CaptureFlowScopeManager)
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureFlowScopeManager)
    }

    private fun createViewModel(): TreeCaptureViewModel =
        TreeCaptureViewModel(
            profilePicUrl = profilePicUrl,
            userRepo = userRepo,
            treeCapturer = treeCapturer,
            createFakeTreesUseCase = createFakeTreesUseCase,
            configurator = configurator,
        )

    @Test
    fun `WHEN numberOfTrees greater than 0 THEN showCaptureTutorial is false`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns
                User(
                    id = 5,
                    wallet = "",
                    numberOfTrees = 5,
                    firstName = "",
                    lastName = "",
                    photoPath = "",
                    isPowerUser = false,
                    unreadMessagesAvailable = false,
                )

            treeCaptureViewModel = createViewModel()
            assertFalse(treeCaptureViewModel.state.value.showCaptureTutorial ?: true)
        }

    @Test
    fun `WHEN numberOfTrees less than 1 THEN showCaptureTutorial is true`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns
                User(
                    id = 5,
                    wallet = "",
                    numberOfTrees = 0,
                    firstName = "",
                    lastName = "",
                    photoPath = "",
                    isPowerUser = false,
                    unreadMessagesAvailable = false,
                )

            treeCaptureViewModel = createViewModel()
            assertTrue(treeCaptureViewModel.state.value.showCaptureTutorial ?: false)
        }

    @Test
    fun `WHEN location coordinate is available THEN isLocationAvailable state true AND isGettingLocation state always false `() =
        runTest {
            coEvery { treeCapturer.pinLocation() } returns true

            treeCaptureViewModel = createViewModel()
            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)

            assertTrue(
                treeCaptureViewModel.state.value.isLocationAvailable ?: false,
            )
            assertFalse(treeCaptureViewModel.state.value.isGettingLocation)
        }

    @Test
    fun `WHEN updateBadGpsDialogState true THEN isLocationAvailable state true`() =
        runTest {
            treeCaptureViewModel = createViewModel()
            treeCaptureViewModel.handleAction(TreeCaptureAction.UpdateBadGpsDialogState(true))
            assertTrue(treeCaptureViewModel.state.value.isLocationAvailable ?: false)
        }

    @Test
    fun `WHEN updateCaptureTutorialDialog true THEN showCaptureTutorial state true`() =
        runTest {
            treeCaptureViewModel = createViewModel()
            treeCaptureViewModel.handleAction(TreeCaptureAction.UpdateCaptureTutorialDialog(true))
            assertTrue(treeCaptureViewModel.state.value.showCaptureTutorial ?: false)
        }

    @Test
    fun `WHEN create fake trees THEN createFakeTreesUseCase is called 1 time AND isCreatingFakeTrees state always false`() =
        runTest {
            treeCaptureViewModel = createViewModel()
            treeCaptureViewModel.handleAction(TreeCaptureAction.CreateFakeTrees)

            coVerify(exactly = 1) { createFakeTreesUseCase.execute(CreateFakeTreesParams(50)) }
            assertFalse(treeCaptureViewModel.state.value.isCreatingFakeTrees)
        }

    @Test
    fun `WHEN created THEN state starts from profilePicUrl and configurator values`() =
        runTest {
            every { configurator.getBoolean(ConfigKeys.FORCE_IMAGE_SIZE) } returns true
            every { configurator.getInt(ConfigKeys.IMAGE_CAPTURE_HEIGHT) } returns 1280

            treeCaptureViewModel =
                TreeCaptureViewModel(
                    profilePicUrl = "/profile.jpg",
                    userRepo = userRepo,
                    treeCapturer = treeCapturer,
                    createFakeTreesUseCase = createFakeTreesUseCase,
                    configurator = configurator,
                )

            val state = treeCaptureViewModel.state.value
            assertEquals("/profile.jpg", state.profilePicUrl)
            assertTrue(state.forceImageScaling)
            assertEquals(1280, state.imageScalingHeight)
            assertFalse(state.isGettingLocation)
            assertFalse(state.isCreatingFakeTrees)
            assertNull(state.isLocationAvailable)
            assertEquals(0f, state.convergencePercentage)
            assertTrue(treeCaptureViewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN force image size disabled THEN forceImageScaling is false`() =
        runTest {
            every { configurator.getBoolean(ConfigKeys.FORCE_IMAGE_SIZE) } returns false
            every { configurator.getInt(ConfigKeys.IMAGE_CAPTURE_HEIGHT) } returns 1920

            treeCaptureViewModel = createViewModel()

            assertFalse(treeCaptureViewModel.state.value.forceImageScaling)
            assertEquals(1920, treeCaptureViewModel.state.value.imageScalingHeight)
        }

    @Test
    fun `WHEN location is pending THEN isGettingLocation is true`() =
        runTest {
            val gate = CompletableDeferred<Boolean>()
            coEvery { treeCapturer.pinLocation() } coAnswers { gate.await() }
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)

            assertTrue(treeCaptureViewModel.state.value.isGettingLocation)
            assertNull(treeCaptureViewModel.state.value.isLocationAvailable)

            gate.complete(true)
            advanceUntilIdle()

            assertFalse(treeCaptureViewModel.state.value.isGettingLocation)
            assertEquals(true, treeCaptureViewModel.state.value.isLocationAvailable)
        }

    @Test
    fun `WHEN location is unavailable THEN isLocationAvailable is false`() =
        runTest {
            coEvery { treeCapturer.pinLocation() } returns false
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)

            assertEquals(false, treeCaptureViewModel.state.value.isLocationAvailable)
            assertFalse(treeCaptureViewModel.state.value.isGettingLocation)
        }

    @Test
    fun `WHEN CaptureLocation repeats before image is captured THEN location is pinned once`() =
        runTest {
            coEvery { treeCapturer.pinLocation() } returns true
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)

            coVerify(exactly = 1) { treeCapturer.pinLocation() }
        }

    @Test
    fun `WHEN CaptureLocation repeats while pin is pending THEN location is pinned once`() =
        runTest {
            val gate = CompletableDeferred<Boolean>()
            coEvery { treeCapturer.pinLocation() } coAnswers { gate.await() }
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            gate.complete(true)
            advanceUntilIdle()

            coVerify(exactly = 1) { treeCapturer.pinLocation() }
        }

    @Test
    fun `WHEN image captured with location THEN sets image and navigates to image review`() =
        runTest {
            val imageFile = File("/photos/captured.jpg")
            coEvery { treeCapturer.pinLocation() } returns true
            every { treeCapturer.currentTree } returns CaptureFlowTestFixtures.tree()
            every { CaptureFlowScopeManager.nav } returns CaptureFlowTestFixtures.controller(treeCapturer)
            val navigator = fakeNavigator(DashboardRoute, TreeCaptureRoute(profilePicUrl = ""))
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.OnImageCaptured(imageFile))
            advanceUntilIdle()

            verify(exactly = 1) { treeCapturer.setImage(imageFile) }
            val event = treeCaptureViewModel.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)
            assertEquals(
                listOf(
                    DashboardRoute,
                    TreeCaptureRoute(profilePicUrl = ""),
                    TreeImageReviewRoute(photoPath = CaptureFlowTestFixtures.PHOTO_PATH),
                ),
                navigator.backStack,
            )
        }

    @Test
    fun `WHEN image captured while location is pending THEN waits for the location before navigating`() =
        runTest {
            val imageFile = File("/photos/captured.jpg")
            val gate = CompletableDeferred<Boolean>()
            coEvery { treeCapturer.pinLocation() } coAnswers { gate.await() }
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.OnImageCaptured(imageFile))

            verify(exactly = 0) { treeCapturer.setImage(any()) }
            assertTrue(treeCaptureViewModel.events.replayCache.isEmpty())

            gate.complete(true)
            advanceUntilIdle()

            verify(exactly = 1) { treeCapturer.setImage(imageFile) }
            assertEquals(1, treeCaptureViewModel.events.replayCache.size)
            assertTrue(treeCaptureViewModel.events.first().getContentIfNotConsumed() is NavigationEvent)
        }

    @Test
    fun `WHEN image captured without location THEN shows bad gps state and does not navigate`() =
        runTest {
            coEvery { treeCapturer.pinLocation() } returns false
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.UpdateBadGpsDialogState(null))
            treeCaptureViewModel.handleAction(TreeCaptureAction.OnImageCaptured(File("/photos/captured.jpg")))
            advanceUntilIdle()

            assertEquals(false, treeCaptureViewModel.state.value.isLocationAvailable)
            verify(exactly = 0) { treeCapturer.setImage(any()) }
            assertTrue(treeCaptureViewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN image captured without requesting a location THEN shows bad gps state and does not navigate`() =
        runTest {
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.OnImageCaptured(File("/photos/captured.jpg")))
            advanceUntilIdle()

            assertEquals(false, treeCaptureViewModel.state.value.isLocationAvailable)
            verify(exactly = 0) { treeCapturer.setImage(any()) }
            coVerify(exactly = 0) { treeCapturer.pinLocation() }
            assertTrue(treeCaptureViewModel.events.replayCache.isEmpty())
        }

    @Test
    fun `WHEN location does not arrive within 30 seconds THEN image capture times out`() =
        runTest {
            val gate = CompletableDeferred<Boolean>()
            coEvery { treeCapturer.pinLocation() } coAnswers { gate.await() }
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.OnImageCaptured(File("/photos/captured.jpg")))

            advanceTimeBy(29_999L)
            assertNull(treeCaptureViewModel.state.value.isLocationAvailable)

            advanceTimeBy(2L)
            assertEquals(false, treeCaptureViewModel.state.value.isLocationAvailable)
            verify(exactly = 0) { treeCapturer.setImage(any()) }
            assertTrue(treeCaptureViewModel.events.replayCache.isEmpty())

            gate.complete(false)
            advanceUntilIdle()
        }

    @Test
    fun `WHEN a capture completes THEN the next CaptureLocation pins a new location`() =
        runTest {
            coEvery { treeCapturer.pinLocation() } returns true
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)
            treeCaptureViewModel.handleAction(TreeCaptureAction.OnImageCaptured(File("/photos/first.jpg")))
            advanceUntilIdle()
            treeCaptureViewModel.handleAction(TreeCaptureAction.CaptureLocation)

            coVerify(exactly = 2) { treeCapturer.pinLocation() }
        }

    @Test
    fun `WHEN updateBadGpsDialogState false or null THEN isLocationAvailable follows`() =
        runTest {
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.UpdateBadGpsDialogState(false))
            assertEquals(false, treeCaptureViewModel.state.value.isLocationAvailable)

            treeCaptureViewModel.handleAction(TreeCaptureAction.UpdateBadGpsDialogState(null))
            assertNull(treeCaptureViewModel.state.value.isLocationAvailable)
        }

    @Test
    fun `WHEN updateCaptureTutorialDialog false THEN showCaptureTutorial state false`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser.copy(numberOfTrees = 0)
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.UpdateCaptureTutorialDialog(false))

            assertEquals(false, treeCaptureViewModel.state.value.showCaptureTutorial)
        }

    @Test
    fun `WHEN fake trees are being created THEN isCreatingFakeTrees is true until done`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            coEvery { createFakeTreesUseCase.execute(any()) } coAnswers { gate.await() }
            treeCaptureViewModel = createViewModel()

            treeCaptureViewModel.handleAction(TreeCaptureAction.CreateFakeTrees)
            assertTrue(treeCaptureViewModel.state.value.isCreatingFakeTrees)

            gate.complete(Unit)
            advanceUntilIdle()
            assertFalse(treeCaptureViewModel.state.value.isCreatingFakeTrees)
        }
}