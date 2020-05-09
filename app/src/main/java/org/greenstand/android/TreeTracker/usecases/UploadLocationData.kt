package org.greenstand.android.TreeTracker.usecases

import com.amazonaws.AmazonClientException
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.utilities.md5
import timber.log.Timber

class UploadLocationData(
    private val dao: TreeTrackerDAO
) : UseCase<Unit, Boolean>() {

    val storageClient = ObjectStorageClient.instance()

    override suspend fun execute(params: Unit): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val locations = dao.getTreeLocationData()
                val treeLocations = mutableListOf<String>()
                val gson = Gson()
                for (location in locations) {
                    val locJson = gson.toJson(location)
                    treeLocations.add(locJson)
                }
                val treeLocJsonArray = gson.toJson(treeLocations)
                storageClient.uploadBundle(treeLocJsonArray, treeLocJsonArray.md5())
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