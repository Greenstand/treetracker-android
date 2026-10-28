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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class TreeListState(
    val trees: List<TreeEntity> = emptyList(),
    val selectedTree: TreeEntity? = null,
    val isLoading: Boolean = true,
)

sealed class TreeListAction : Action {
    data class SelectTree(
        val tree: TreeEntity,
    ) : TreeListAction()
}

class TreeListViewModel(
    private val userWallet: String,
    private val dao: TreeTrackerDAO,
) : BaseViewModel<TreeListState, TreeListAction>(TreeListState()) {
    init {
        viewModelScope.launch {
            dao.getTreesByUserWallet(userWallet).collect { trees ->
                updateState {
                    copy(
                        trees = trees,
                        isLoading = false,
                        selectedTree =
                            selectedTree?.let { selected ->
                                trees.find { it.id == selected.id }
                            },
                    )
                }
            }
        }
    }

    override fun handleAction(action: TreeListAction) {
        when (action) {
            is TreeListAction.SelectTree -> {
                updateState { copy(selectedTree = action.tree) }
            }
        }
    }
}

class TreeListViewModelFactory(
    private val userWallet: String,
) : ViewModelProvider.Factory,
    KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TreeListViewModel(userWallet, get()) as T
}