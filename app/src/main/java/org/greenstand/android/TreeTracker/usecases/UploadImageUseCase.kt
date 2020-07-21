package org.greenstand.android.TreeTracker.usecases

import com.amazonaws.AmazonClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import timber.log.Timber

data class UploadImageParams(val imagePath: String,
                             val lat: Double,
                             val long: Double)

class UploadImageUseCase(private val doSpaces: ObjectStorageClient) : UseCase<UploadImageParams, String?>() {

    override suspend fun execute(params: UploadImageParams): String? {
        return try {
            withContext(Dispatchers.IO) {
                doSpaces.put(params.imagePath, params.lat, params.long)
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
            null
        }
    }
}