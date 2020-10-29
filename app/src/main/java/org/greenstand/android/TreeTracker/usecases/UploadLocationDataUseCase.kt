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
    private val dao: TreeTrackerDAO,
    private val gson: Gson
) : UseCase<Unit, Boolean>() {

    val storageClient = ObjectStorageClient.instance()

    override suspend fun execute(params: Unit): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val locationEntities = dao.getTreeLocationData()
                val locations = locationEntities.map { it.locationDataJson }
                val treeLocJsonArray = locations.toString()
                storageClient.uploadBundle(
                    treeLocJsonArray,
                    "loc_data_${treeLocJsonArray.md5()}"
                )
                for (locationData in locationEntities) {
                    locationData.uploaded = true
                    dao.updateLocationData(locationData)
                }
                Timber.d("Completed uploading ${locations.size} GPS locations")
                dao.purgeUploadedTreeLocations()
                Timber.d("Completed purging of uploaded GPS locations")
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
