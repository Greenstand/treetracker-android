package org.greenstand.android.TreeTracker.usecases

import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.coroutines.coroutineContext

interface TreeUploadStrategy {
    suspend fun uploadTrees(treeIds: List<Long>)
}

class BundleTreeUploadStrategy(private val uploadTreeBundleUseCase: UploadTreeBundleUseCase) : TreeUploadStrategy {
    override suspend fun uploadTrees(treeIds: List<Long>) {
        Timber.tag("BundleTreeUpload").d("Uploading ${treeIds.size} trees")

        treeIds.windowed(size = 50, step = 50, partialWindows = true).onEach { treeIdBundle ->
            try {
                if (coroutineContext.isActive) {
                    uploadTreeBundleUseCase.execute(UploadTreeBundleParams(treeIds = treeIdBundle))
                } else {
                    coroutineContext.cancel()
                }
            } catch (e: Exception) {
                Timber.e("NewTree upload failed")
            }
        }    }

}

class ContinuousTreeUploadStrategy(private val syncTreeUseCase: SyncTreeUseCase) : TreeUploadStrategy {
    override suspend fun uploadTrees(treeIds: List<Long>) {
        Timber.tag("ContinuousTreeUpload").d("Uploading ${treeIds.size} trees")

        treeIds.onEach {
            try {
                if (coroutineContext.isActive) {
                    syncTreeUseCase.execute(SyncTreeParams(treeId = it))
                } else {
                    coroutineContext.cancel()
                }
            } catch (e: Exception) {
                Timber.e("NewTree upload failed")
            }
        }
    }

}
