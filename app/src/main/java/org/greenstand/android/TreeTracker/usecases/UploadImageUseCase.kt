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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.analytics.ExceptionDataCollector
import org.greenstand.android.TreeTracker.api.ObjectStorageClient
import java.io.IOException

data class UploadImageParams(
    val imagePath: String,
    val lat: Double,
    val long: Double,
)

class UploadImageUseCase(
    private val doSpaces: ObjectStorageClient,
    private val exceptionDataCollector: ExceptionDataCollector,
) : UseCase<UploadImageParams, String?>() {
    override suspend fun execute(params: UploadImageParams): String? =
        try {
            withContext(Dispatchers.IO) {
                doSpaces.put(params.imagePath, params.lat, params.long)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_NETWORK, e, "Network failure during image upload")
            null
        } catch (ace: AmazonClientException) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_SERVER, ace, "Storage server failure during image upload")
            null
        } catch (e: Exception) {
            exceptionDataCollector.recordFailure(ExceptionDataCollector.TYPE_UNKNOWN, e, "Unexpected failure during image upload")
            null
        }
}