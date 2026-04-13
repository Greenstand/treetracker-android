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
import org.greenstand.android.TreeTracker.dashboard.TreesToSyncHelper
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.viewmodel.Action
import org.greenstand.android.TreeTracker.viewmodel.BaseViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class TreeDetailState(
    val tree: TreeEntity? = null,
    val editedNote: String = "",
    val showDeleteConfirmation: Boolean = false,
    val isDeleted: Boolean = false,
    val noteSaved: Boolean = false,
)

sealed class TreeDetailAction : Action {
    data class UpdateNote(val note: String) : TreeDetailAction()
    object SaveNote : TreeDetailAction()
    object DeleteTree : TreeDetailAction()
    data class SetDeleteDialogVisibility(val show: Boolean) : TreeDetailAction()
    object NoteSavedShown : TreeDetailAction()
}

class TreeDetailViewModel(
    private val treeId: Long,
    private val dao: TreeTrackerDAO,
    private val treesToSyncHelper: TreesToSyncHelper,
) : BaseViewModel<TreeDetailState, TreeDetailAction>(TreeDetailState()) {

    init {
        viewModelScope.launch {
            val trees = dao.getTreesByIds(listOf(treeId))
            trees.firstOrNull()?.let { tree ->
                updateState { copy(tree = tree, editedNote = tree.note) }
            }
        }
    }

    override fun handleAction(action: TreeDetailAction) {
        when (action) {
            is TreeDetailAction.UpdateNote -> {
                updateState { copy(editedNote = action.note) }
            }
            is TreeDetailAction.SaveNote -> {
                viewModelScope.launch {
                    currentState.tree?.let { tree ->
                        tree.note = currentState.editedNote
                        dao.updateTree(tree)
                        updateState { copy(tree = tree, noteSaved = true) }
                    }
                }
            }
            is TreeDetailAction.NoteSavedShown -> {
                updateState { copy(noteSaved = false) }
            }
            is TreeDetailAction.DeleteTree -> {
                viewModelScope.launch {
                    currentState.tree?.photoPath?.let { path ->
                        val file = java.io.File(path)
                        if (file.exists()) file.delete()
                    }
                    dao.deleteTreeById(treeId)
                    treesToSyncHelper.refreshTreeCountToSync()
                    updateState { copy(isDeleted = true) }
                }
            }
            is TreeDetailAction.SetDeleteDialogVisibility -> {
                updateState { copy(showDeleteConfirmation = action.show) }
            }
        }
    }
}

class TreeDetailViewModelFactory(
    private val treeId: Long,
) : ViewModelProvider.Factory, KoinComponent {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TreeDetailViewModel(treeId, get(), get()) as T
}
