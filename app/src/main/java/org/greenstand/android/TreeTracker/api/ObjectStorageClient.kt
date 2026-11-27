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
package org.greenstand.android.TreeTracker.api

import android.annotation.SuppressLint
import android.content.Context
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import org.greenstand.android.TreeTracker.BuildConfig
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import java.util.UUID

@Suppress("ImplicitDefaultLocale")
class ObjectStorageClient private constructor(
    applicationContext: Context,
) {
    private var s3Client: AmazonS3? = null

    init {

        val region = Regions.fromName(BuildConfig.OBJECT_STORAGE_IDENTITY_REGION)

        val credentialsProvider =
            CognitoCachingCredentialsProvider(
                applicationContext,
                BuildConfig.OBJECT_STORAGE_IDENTITY_POOL_ID,
                region,
            )

        if (BuildConfig.USE_AWS_S3) {

            val clientRegion = BuildConfig.OBJECT_STORAGE_ENDPOINT
            try {

                val region = Region.getRegion(Regions.fromName(clientRegion))
                s3Client = AmazonS3Client(credentialsProvider, region, ClientConfiguration())
            } catch (e: AmazonServiceException) {
                Timber.e(e, "S3 service error during client initialization")
                throw(e)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize S3 client")
                throw(e)
            }
        } else {

            s3Client = AmazonS3Client(credentialsProvider)
            s3Client?.setEndpoint(String.format("https://%s/", BuildConfig.OBJECT_STORAGE_ENDPOINT))
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(AmazonClientException::class)
    fun put(
        path: String,
        lat: Double,
        long: Double,
    ): String {
        val acl = AccessControlList()
        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)

        val image = File(path)
        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())

        val dosKey = timeStamp + '_' + lat + '_' + long + '_' + UUID.randomUUID() + '_' + image.name
        val poRequest = PutObjectRequest(BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES, dosKey, image)
        poRequest.withAccessControlList(acl)
        s3Client?.putObject(poRequest)

        if (BuildConfig.USE_AWS_S3) {
            return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES,
                BuildConfig.OBJECT_STORAGE_ENDPOINT,
                dosKey,
            )
        } else {
            return String.format(
                "https://%s.%s/%s",
                BuildConfig.OBJECT_STORAGE_BUCKET_IMAGES,
                BuildConfig.OBJECT_STORAGE_ENDPOINT,
                dosKey,
            )
        }
    }

    fun uploadBundle(
        jsonBundle: String,
        bundleId: String,
    ) {
        val byteArray = jsonBundle.toByteArray(Charsets.UTF_8)
        val inputStream = ByteArrayInputStream(byteArray)

        val acl = AccessControlList()
        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)

        val timeStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())

        val dosKey = timeStamp + '_'.toString() + UUID.randomUUID() + '_'.toString() + bundleId + ".json"

        val objectMetadata =
            ObjectMetadata().apply {
                contentLength = inputStream.available().toLong()
            }

        val putObjectRequest =
            PutObjectRequest(
                BuildConfig.OBJECT_STORAGE_BUCKET_BATCH_UPLOADS,
                dosKey,
                inputStream,
                objectMetadata,
            ).apply {
                withAccessControlList(acl)
            }

        s3Client?.putObject(putObjectRequest)
    }

    companion object {
        private var INSTANCE: ObjectStorageClient? = null

        fun init(context: Context) {
            INSTANCE = ObjectStorageClient(context)
        }

        fun instance(): ObjectStorageClient = INSTANCE!!
    }
}