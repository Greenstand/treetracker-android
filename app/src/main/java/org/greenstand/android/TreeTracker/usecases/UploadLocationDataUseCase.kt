package org.greenstand.android.TreeTracker.usecases

import com.amazonaws.AmazonClientException
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber

class UploadLocationDataUseCase(
    private val dao: TreeTrackerDAO
) : UseCase<Unit, Boolean>() {

    val storageClient = ObjectStorageClient.instance()

    override suspend fun execute(params: Unit): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val gson = Gson()
                val locations = dao.getTreeLocationData().map { gson.toJson(it) }
                val treeLocJsonArray = gson.toJson(locations)
                storageClient.uploadBundle(treeLocJsonArray, treeLocJsonArray.md5())
                dao.purgeUploadedTreeLocations()
                Timber.d("Completed purging uploaded tree locations")
            }
        } catch (ace: AmazonClientException) {
            Timber.e(
                "Caught an AmazonClientException, which " +
                        "means the client encountered " +
                        "an internal error while trying to " +
                        "communicate with S3, " +
                        "such as not being able to access the network."
            )
            Timber.e("Error Message: ${ace.message}")
            return false
        }
        return true
    }
}