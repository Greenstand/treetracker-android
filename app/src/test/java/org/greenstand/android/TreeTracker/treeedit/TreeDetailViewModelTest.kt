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
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TreeDetailViewModelTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

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
    fun `SaveNote updates tree in DAO and sets noteSaved flag`() =
        runTest {
            coEvery { dao.updateTree(any()) } just Runs
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.UpdateNote("Updated note"))
            vm.handleAction(TreeDetailAction.SaveNote)
            vm.state.first { it.noteSaved }
            assertTrue(vm.state.value.noteSaved)
            coVerify { dao.updateTree(match { it.note == "Updated note" }) }
        }

    @Test
    fun `NoteSavedShown clears noteSaved flag`() =
        runTest {
            coEvery { dao.updateTree(any()) } just Runs
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.SaveNote)
            vm.state.first { it.noteSaved }
            vm.handleAction(TreeDetailAction.NoteSavedShown)
            assertFalse(vm.state.value.noteSaved)
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
    fun `DeleteTree calls DAO delete and refreshes sync helper`() =
        runTest {
            coEvery { dao.deleteTreeById(42) } just Runs
            coEvery { treesToSyncHelper.refreshTreeCountToSync() } just Runs
            val vm = createViewModel()
            vm.state.first { it.tree != null }
            vm.handleAction(TreeDetailAction.DeleteTree)
            vm.state.first { it.isDeleted }
            assertTrue(vm.state.value.isDeleted)
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
            vm.state.first { it.isDeleted }
            assertTrue(vm.state.value.isDeleted)
            coVerify { dao.deleteTreeById(42) }
        }
}
