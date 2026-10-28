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
package org.greenstand.android.TreeTracker.treeedit

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.navigation.DashboardRoute
import org.greenstand.android.TreeTracker.navigation.TreeDetailRoute
import org.greenstand.android.TreeTracker.navigation.TreeListRoute
import org.greenstand.android.TreeTracker.utils.fakeNavigator
import org.greenstand.android.TreeTracker.viewmodel.PopBackStackEvent
import org.greenstand.android.TreeTracker.viewmodel.ShowSnackbar
import org.greenstand.android.TreeTracker.viewmodel.TextRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@ExperimentalCoroutinesApi
class TreeDetailViewModelTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var tempFolder = TemporaryFolder()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    @MockK(relaxed = true)
    private lateinit var treesToSyncHelper: TreesToSyncHelper

    private val fakeTree =
        TreeEntity(
            uuid = "abc-123",
            sessionId = 1,
            photoPath = "/fake/photo.jpg",
            photoUrl = null,
            note = "Original note",
            latitude = 12.345,
            longitude = -67.890,
            uploaded = false,
            createdAt = Instant.parse("2026-04-01T10:00:00Z"),
        ).apply { id = 42 }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    private fun createViewModel(tree: TreeEntity? = fakeTree): TreeDetailViewModel {
        coEvery { dao.getTreesByIds(listOf(42)) } returns listOfNotNull(tree)
        return TreeDetailViewModel(treeId = 42, dao = dao, treesToSyncHelper = treesToSyncHelper)
    }

    @Test
    fun `init loads tree and sets state`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            assertEquals(fakeTree, vm.state.value.tree)
            assertEquals("Original note", vm.state.value.editedNote)
        }

    @Test
    fun `init with nonexistent tree leaves state empty`() =
        runTest {
            coEvery { dao.getTreesByIds(listOf(42)) } returns emptyList()
            val vm = TreeDetailViewModel(treeId = 42, dao = dao, treesToSyncHelper = treesToSyncHelper)
            // Give time for the init coroutine to complete
            vm.state.first { true }
            assertEquals(null, vm.state.value.tree)
        }

    @Test
    fun `UpdateNote updates editedNote in state`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.UpdateNote("New note text"))
            assertEquals("New note text", vm.state.value.editedNote)
        }

    @Test
    fun `SaveNote updates tree in DAO and emits noteSaved snackbar`() =
        runTest {
            coEvery { dao.updateTree(any()) } just Runs
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.UpdateNote("Updated note"))
            vm.handleAction(TreeDetailAction.SaveNote)

            val event = vm.events.first().getContentIfNotConsumed()
            assertTrue(event is ShowSnackbar)
            assertEquals(TextRef.Res(R.string.tree_note_saved), (event as ShowSnackbar).message)
            coVerify { dao.updateTree(match { it.note == "Updated note" }) }
        }

    @Test
    fun `SetDeleteDialogVisibility toggles showDeleteConfirmation`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.SetDeleteDialogVisibility(true))
            assertTrue(vm.state.value.showDeleteConfirmation)
            vm.handleAction(TreeDetailAction.SetDeleteDialogVisibility(false))
            assertFalse(vm.state.value.showDeleteConfirmation)
        }

    @Test
    fun `DeleteTree calls DAO delete, refreshes sync helper, and emits pop-back event`() =
        runTest {
            coEvery { dao.deleteTreeById(42) } just Runs
            coEvery { treesToSyncHelper.refreshTreeCountToSync() } just Runs
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.DeleteTree)

            val event = vm.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
            coVerify { dao.deleteTreeById(42) }
            coVerify { treesToSyncHelper.refreshTreeCountToSync() }
        }

    @Test
    fun `DeleteTree for uploaded tree with null photoPath still deletes`() =
        runTest {
            val uploadedTree = fakeTree.copy(photoPath = null, uploaded = true).apply { id = 42 }
            coEvery { dao.deleteTreeById(42) } just Runs
            coEvery { treesToSyncHelper.refreshTreeCountToSync() } just Runs
            val vm = createViewModel(tree = uploadedTree)
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.DeleteTree)

            val event = vm.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
            coVerify { dao.deleteTreeById(42) }
        }

    @Test
    fun `NavigateBack emits pop-back event`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.NavigateBack)

            val event = vm.events.first().getContentIfNotConsumed()
            assertEquals(PopBackStackEvent, event)
        }

    @Test
    fun `before the tree is loaded state is empty`() =
        runTest {
            val gate = CompletableDeferred<List<TreeEntity>>()
            coEvery { dao.getTreesByIds(listOf(42)) } coAnswers { gate.await() }

            val vm = TreeDetailViewModel(treeId = 42, dao = dao, treesToSyncHelper = treesToSyncHelper)

            assertEquals(TreeDetailState(tree = null, editedNote = "", showDeleteConfirmation = false), vm.state.value)
            gate.complete(listOf(fakeTree))
            vm.state.first { it.tree != null }
            coVerify(exactly = 1) { dao.getTreesByIds(listOf(42L)) }
        }

    @Test
    fun `SaveNote stores the edited note in state tree`() =
        runTest {
            coEvery { dao.updateTree(any()) } just Runs
            val vm = createViewModel()
            vm.state.first { it.tree != null }

            vm.handleAction(TreeDetailAction.UpdateNote("Updated note"))
            vm.handleAction(TreeDetailAction.SaveNote)
            vm.events.first()

            assertEquals(
                "Updated note",
                vm.state.value.tree
                    ?.note,
            )
            assertEquals("Updated note", vm.state.value.editedNote)
        }

    @Test
    fun `SaveNote without a loaded tree does nothing`() =
        runTest {
            val vm = createViewModel(tree = null)

            vm.handleAction(TreeDetailAction.UpdateNote("Note for nothing"))
            vm.handleAction(TreeDetailAction.SaveNote)

            coVerify(exactly = 0) { dao.updateTree(any()) }
            assertNull(vm.state.value.tree)
            assertTrue(vm.events.replayCache.isEmpty())
        }

    @Test
    fun `DeleteTree removes the local photo file`() =
        runTest {
            val photo = tempFolder.newFile("tree.jpg")
            val treeWithPhoto = fakeTree.copy(photoPath = photo.absolutePath).apply { id = 42 }
            val vm = createViewModel(tree = treeWithPhoto)
            vm.state.first { it.tree != null }

            vm.handleAction(TreeDetailAction.DeleteTree)
            vm.events.first()

            assertFalse(photo.exists())
            coVerify { dao.deleteTreeById(42) }
        }

    @Test
    fun `DeleteTree with a missing photo file still deletes the tree`() =
        runTest {
            val missing = tempFolder.root.resolve("missing.jpg")
            val treeWithoutFile = fakeTree.copy(photoPath = missing.absolutePath).apply { id = 42 }
            val vm = createViewModel(tree = treeWithoutFile)
            vm.state.first { it.tree != null }

            vm.handleAction(TreeDetailAction.DeleteTree)

            assertEquals(PopBackStackEvent, vm.events.first().getContentIfNotConsumed())
            coVerify { dao.deleteTreeById(42) }
        }

    @Test
    fun `DeleteTree before the tree is loaded deletes by id`() =
        runTest {
            val vm = createViewModel(tree = null)

            vm.handleAction(TreeDetailAction.DeleteTree)

            assertEquals(PopBackStackEvent, vm.events.first().getContentIfNotConsumed())
            coVerify { dao.deleteTreeById(42) }
            coVerify { treesToSyncHelper.refreshTreeCountToSync() }
        }

    @Test
    fun `DeleteTree deletes before refreshing the sync count`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.tree != null }

            vm.handleAction(TreeDetailAction.DeleteTree)
            vm.events.first()

            coVerifyOrder {
                dao.deleteTreeById(42)
                treesToSyncHelper.refreshTreeCountToSync()
            }
        }

    @Test
    fun `DeleteTree pop-back event returns to the tree list`() =
        runTest {
            val navigator =
                fakeNavigator(DashboardRoute, TreeListRoute(userWallet = "wallet", userName = "Jane"), TreeDetailRoute(treeId = 42))
            val vm = createViewModel()
            vm.state.first { it.tree != null }

            vm.handleAction(TreeDetailAction.DeleteTree)
            val event = vm.events.first().getContentIfNotConsumed()

            // HandleUIEvents dispatches PopBackStackEvent as a throttled pop.
            assertEquals(PopBackStackEvent, event)
            navigator.throttledPopBackStack()
            assertEquals(
                listOf(DashboardRoute, TreeListRoute(userWallet = "wallet", userName = "Jane")),
                navigator.backStack,
            )
        }

    @Test
    fun `dialog visibility and note edits emit no events`() =
        runTest {
            val vm = createViewModel()
            vm.state.first { it.tree != null }

            vm.handleAction(TreeDetailAction.SetDeleteDialogVisibility(true))
            vm.handleAction(TreeDetailAction.UpdateNote("draft"))

            assertTrue(vm.events.replayCache.isEmpty())
            assertEquals(
                "Original note",
                vm.state.value.tree
                    ?.note,
            )
        }
}