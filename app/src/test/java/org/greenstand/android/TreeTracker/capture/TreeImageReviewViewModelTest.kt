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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.models.TreeCapturer
import org.greenstand.android.TreeTracker.models.UserRepo
import org.greenstand.android.TreeTracker.models.captureflowdata.CaptureFlowScopeManager
import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.FeatureResolver
import org.greenstand.android.TreeTracker.models.organization.OrgFeature
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.Navigator
import org.greenstand.android.TreeTracker.navigation.RouteRegistry
import org.greenstand.android.TreeTracker.navigation.TreeCaptureRoute
import org.greenstand.android.TreeTracker.navigation.TreeHeightScreenRoute
import org.greenstand.android.TreeTracker.navigation.TreeImageReviewRoute
import org.greenstand.android.TreeTracker.utils.FakeClock
import org.greenstand.android.TreeTracker.utils.FakeFileGenerator
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.NavigationEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TreeImageReviewViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val treeCapturer = mockk<TreeCapturer>(relaxed = true)
    private val userRepo = mockk<UserRepo>(relaxed = true)
    private val featureResolver = mockk<FeatureResolver>(relaxed = true)

    private lateinit var testSubject: TreeImageReviewViewModel

    @Before
    fun setUp() {
        mockkObject(CaptureFlowScopeManager)
        testSubject = TreeImageReviewViewModel(treeCapturer = treeCapturer, userRepo = userRepo, featureResolver = featureResolver)
    }

    @After
    fun tearDown() {
        unmockkObject(CaptureFlowScopeManager)
    }

    private fun createViewModel(): TreeImageReviewViewModel = TreeImageReviewViewModel(treeCapturer = treeCapturer, userRepo = userRepo, featureResolver = featureResolver)

    /**
     * Opens a real capture flow and moves it from TreeCapture to TreeImageReview, the state the
     * review screen is shown in. The clock is advanced past the navigation throttle afterwards.
     */
    private suspend fun navigatorOnImageReview(captureFlow: List<Destination>): Navigator {
        every { treeCapturer.currentTree } returns CaptureFlowTestFixtures.tree()
        coEvery { treeCapturer.saveTree() } returns true
        val clock = FakeClock()
        val navigator = fakeNavigator(DashboardRoute, TreeCaptureRoute(profilePicUrl = ""), clock = clock)
        val controller = CaptureFlowTestFixtures.controller(treeCapturer, captureFlow)
        every { CaptureFlowScopeManager.nav } returns controller
        controller.navForward(navigator)
        clock.advance(1_000L)
        return navigator
    }

    @Test
    fun `updateNote should update the note in the view model state`() =
        runTest {
            val note = "Note"
            testSubject.handleAction(TreeImageReviewAction.UpdateNote(note))
            val expected = testSubject.state.value.note
            assertEquals(expected, note)
        }

    @Test
    fun `updateReviewTutorialDialog should update showReviewTutorial flag state `() =
        runTest {
            val showTutorial = true
            testSubject.handleAction(TreeImageReviewAction.UpdateReviewTutorialDialog(showTutorial))
            val expected = testSubject.state.value.showReviewTutorial!!
            assertTrue(expected)
        }

    @Test
    fun `addNote should set the note in the tree capturer and close the dialog`() =
        runTest {
            val note = "Some note"
            testSubject.handleAction(TreeImageReviewAction.UpdateNote(note))
            testSubject.handleAction(TreeImageReviewAction.AddNote)
            verify { treeCapturer.setNote(note) }
            val expected = testSubject.state.value.isDialogOpen
            assertFalse(expected)
        }

    @Test
    fun `setDialogState should update the dialog open state in the view model`() =
        runTest {
            val isDialogOpen = true
            testSubject.handleAction(TreeImageReviewAction.SetDialogState(isDialogOpen))
            val expected = testSubject.state.value.isDialogOpen
            assertTrue(expected)
        }

    @Test
    fun `CheckIfCanNavigateForward without forceNote emits NavigationEvent`() =
        runTest {
            // featureResolver defaults to false (relaxed mock) → forceNote = false
            testSubject.handleAction(TreeImageReviewAction.CheckIfCanNavigateForward)

            val event = testSubject.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent)
        }

    @Test
    fun `CheckIfCanNavigateForward with forceNote and blank note opens dialog and emits no event`() =
        runTest {
            every {
                featureResolver.isCaptureFlowFeatureEnabled(any(), any())
            } returns true
            val vm = TreeImageReviewViewModel(treeCapturer, userRepo, featureResolver)

            vm.handleAction(TreeImageReviewAction.CheckIfCanNavigateForward)

            assertTrue(vm.state.value.isDialogOpen)
        }

    @Test
    fun `CheckIfCanNavigateForward with forceNote and non-blank note emits NavigationEvent`() =
        runTest {
            every {
                featureResolver.isCaptureFlowFeatureEnabled(any(), any())
            } returns true
            val vm = TreeImageReviewViewModel(treeCapturer, userRepo, featureResolver)

            vm.handleAction(TreeImageReviewAction.UpdateNote("a note"))
            vm.handleAction(TreeImageReviewAction.CheckIfCanNavigateForward)

            val event = vm.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent)
            assertFalse(vm.state.value.isDialogOpen)
        }

    @Test
    fun `NavigateBack emits NavigationEvent`() =
        runTest {
            testSubject.handleAction(TreeImageReviewAction.NavigateBack)

            val event = testSubject.events.first().getContentIfNotConsumed()
            assertTrue(event is NavigationEvent)
        }

    @Test
    fun `init with a first-time planter shows the review tutorial and the captured photo`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser.copy(numberOfTrees = 0)
            every { treeCapturer.currentTree } returns CaptureFlowTestFixtures.tree()

            val vm = createViewModel()

            assertEquals(true, vm.state.value.showReviewTutorial)
            assertEquals(CaptureFlowTestFixtures.PHOTO_PATH, vm.state.value.treeImagePath)
            assertEquals("", vm.state.value.note)
            assertFalse(vm.state.value.isDialogOpen)
            assertTrue(vm.events.replayCache.isEmpty())
        }

    @Test
    fun `init with an experienced planter hides the review tutorial`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns FakeFileGenerator.emptyUser.copy(numberOfTrees = 5)

            val vm = createViewModel()

            assertEquals(false, vm.state.value.showReviewTutorial)
        }

    @Test
    fun `init without a power user shows the review tutorial`() =
        runTest {
            coEvery { userRepo.getPowerUser() } returns null

            val vm = createViewModel()

            assertEquals(true, vm.state.value.showReviewTutorial)
        }

    @Test
    fun `init without a current tree leaves the image path empty`() =
        runTest {
            every { treeCapturer.currentTree } returns null

            val vm = createViewModel()

            assertNull(vm.state.value.treeImagePath)
        }

    @Test
    fun `forceNote is read from the image review destination of the capture flow`() {
        verify {
            featureResolver.isCaptureFlowFeatureEnabled(RouteRegistry.ROUTE_TREE_IMAGE_REVIEW, OrgFeature.FORCE_NOTE)
        }
    }

    @Test
    fun `CheckIfCanNavigateForward with forceNote and whitespace note opens dialog and emits no event`() =
        runTest {
            every { featureResolver.isCaptureFlowFeatureEnabled(any(), any()) } returns true
            val vm = createViewModel()

            vm.handleAction(TreeImageReviewAction.UpdateNote("   "))
            vm.handleAction(TreeImageReviewAction.CheckIfCanNavigateForward)

            assertTrue(vm.state.value.isDialogOpen)
            assertTrue(vm.events.replayCache.isEmpty())
        }

    @Test
    fun `CheckIfCanNavigateForward navigates to the next capture flow screen`() =
        runTest {
            val navigator = navigatorOnImageReview(CaptureFlowTestFixtures.defaultCaptureFlow)

            testSubject.handleAction(TreeImageReviewAction.CheckIfCanNavigateForward)
            val event = testSubject.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)

            assertEquals(
                listOf(
                    DashboardRoute,
                    TreeCaptureRoute(profilePicUrl = ""),
                    TreeImageReviewRoute(photoPath = CaptureFlowTestFixtures.PHOTO_PATH),
                    TreeHeightScreenRoute,
                ),
                navigator.backStack,
            )
            coVerify(exactly = 0) { treeCapturer.saveTree() }
        }

    @Test
    fun `CheckIfCanNavigateForward on the last flow screen saves the tree and returns to capture`() =
        runTest {
            val navigator = navigatorOnImageReview(CaptureFlowTestFixtures.shortCaptureFlow)

            testSubject.handleAction(TreeImageReviewAction.CheckIfCanNavigateForward)
            val event = testSubject.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)

            coVerify(exactly = 1) { treeCapturer.saveTree() }
            assertEquals(
                listOf(DashboardRoute, TreeCaptureRoute(profilePicUrl = "")),
                navigator.backStack,
            )
        }

    @Test
    fun `NavigateBack pops back to tree capture`() =
        runTest {
            val navigator = navigatorOnImageReview(CaptureFlowTestFixtures.defaultCaptureFlow)

            testSubject.handleAction(TreeImageReviewAction.NavigateBack)
            val event = testSubject.events.first().getContentIfNotConsumed() as NavigationEvent
            event.navigate(navigator)

            assertEquals(
                listOf(DashboardRoute, TreeCaptureRoute(profilePicUrl = "")),
                navigator.backStack,
            )
        }

    @Test
    fun `addNote closes an open dialog`() =
        runTest {
            testSubject.handleAction(TreeImageReviewAction.SetDialogState(true))
            testSubject.handleAction(TreeImageReviewAction.UpdateNote("note"))

            testSubject.handleAction(TreeImageReviewAction.AddNote)

            verify(exactly = 1) { treeCapturer.setNote("note") }
            assertFalse(testSubject.state.value.isDialogOpen)
        }

    @Test
    fun `setDialogState false closes the dialog`() =
        runTest {
            testSubject.handleAction(TreeImageReviewAction.SetDialogState(true))
            testSubject.handleAction(TreeImageReviewAction.SetDialogState(false))

            assertFalse(testSubject.state.value.isDialogOpen)
        }

    @Test
    fun `updateReviewTutorialDialog false hides the tutorial`() =
        runTest {
            testSubject.handleAction(TreeImageReviewAction.UpdateReviewTutorialDialog(false))

            assertEquals(false, testSubject.state.value.showReviewTutorial)
        }
}