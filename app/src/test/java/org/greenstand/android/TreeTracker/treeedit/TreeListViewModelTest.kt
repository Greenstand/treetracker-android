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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.greenstand.android.TreeTracker.MainCoroutineRule
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TreeListViewModelTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @MockK(relaxed = true)
    private lateinit var dao: TreeTrackerDAO

    private val tree1 =
        TreeEntity(
            uuid = "abc-123",
            sessionId = 1,
            photoPath = null,
            photoUrl = null,
            note = "First tree",
            latitude = 12.345,
            longitude = -67.890,
            uploaded = false,
            createdAt = Instant.parse("2026-04-01T10:00:00Z"),
        ).apply { id = 1 }

    private val tree2 =
        TreeEntity(
            uuid = "def-456",
            sessionId = 1,
            photoPath = null,
            photoUrl = null,
            note = "Second tree",
            latitude = 12.346,
            longitude = -67.891,
            uploaded = true,
            createdAt = Instant.parse("2026-04-02T14:30:00Z"),
        ).apply { id = 2 }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `init loads trees from DAO and sets isLoading to false`() =
        runTest {
            every { dao.getTreesByUserWallet("wallet-1") } returns flowOf(listOf(tree1, tree2))
            val vm = TreeListViewModel(userWallet = "wallet-1", dao = dao)
            vm.state.first { !it.isLoading }
            assertEquals(2, vm.state.value.trees.size)
            assertFalse(vm.state.value.isLoading)
        }

    @Test
    fun `init with no trees sets empty list`() =
        runTest {
            every { dao.getTreesByUserWallet("wallet-1") } returns flowOf(emptyList())
            val vm = TreeListViewModel(userWallet = "wallet-1", dao = dao)
            vm.state.first { !it.isLoading }
            assertEquals(0, vm.state.value.trees.size)
            assertFalse(vm.state.value.isLoading)
        }

    @Test
    fun `SelectTree updates selectedTree in state`() =
        runTest {
            every { dao.getTreesByUserWallet("wallet-1") } returns flowOf(listOf(tree1, tree2))
            val vm = TreeListViewModel(userWallet = "wallet-1", dao = dao)
            vm.state.first { !it.isLoading }
            vm.handleAction(TreeListAction.SelectTree(tree1))
            assertEquals(tree1, vm.state.value.selectedTree)
        }

    @Test
    fun `selectedTree is cleared when it disappears from tree list`() =
        runTest {
            val treeFlow = MutableSharedFlow<List<TreeEntity>>(replay = 1)
            every { dao.getTreesByUserWallet("wallet-1") } returns treeFlow
            treeFlow.emit(listOf(tree1, tree2))
            val vm = TreeListViewModel(userWallet = "wallet-1", dao = dao)
            vm.state.first { !it.isLoading }
            vm.handleAction(TreeListAction.SelectTree(tree1))
            assertEquals(tree1, vm.state.value.selectedTree)
            // Emit updated list without tree1 (simulating deletion)
            treeFlow.emit(listOf(tree2))
            vm.state.first { it.selectedTree == null }
            assertNull(vm.state.value.selectedTree)
        }

    @Test
    fun `selectedTree persists when it remains in updated tree list`() =
        runTest {
            val treeFlow = MutableSharedFlow<List<TreeEntity>>(replay = 1)
            every { dao.getTreesByUserWallet("wallet-1") } returns treeFlow
            treeFlow.emit(listOf(tree1, tree2))
            val vm = TreeListViewModel(userWallet = "wallet-1", dao = dao)
            vm.state.first { !it.isLoading }
            vm.handleAction(TreeListAction.SelectTree(tree1))
            assertEquals(tree1, vm.state.value.selectedTree)
            // Emit updated list that still includes tree1
            treeFlow.emit(listOf(tree1, tree2))
            vm.state.first { it.trees.size == 2 }
            assertEquals(tree1.id, vm.state.value.selectedTree?.id)
        }
}
