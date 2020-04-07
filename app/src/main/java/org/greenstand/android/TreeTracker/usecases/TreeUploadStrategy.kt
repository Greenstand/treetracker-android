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
        Timber.tag(tag).d("Uploading ${treeIds.size} trees")

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

    companion object {
        val tag: String = BundleTreeUploadStrategy::class.java.simpleName
    }
}