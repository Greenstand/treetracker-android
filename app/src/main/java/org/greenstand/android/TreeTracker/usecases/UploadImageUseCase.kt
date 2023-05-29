/*
 * Copyright 2023 Treetracker
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
package org.greenstand.android.TreeTracker.usecases

import com.amazonaws.AmazonClientException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import timber.log.Timber

data class UploadImageParams(
    val imagePath: String,
    val lat: Double,
    val long: Double
)

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