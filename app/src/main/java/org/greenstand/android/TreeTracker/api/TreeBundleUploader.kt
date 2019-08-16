package org.greenstand.android.TreeTracker.api

import com.amazonaws.AmazonServiceException
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import org.greenstand.android.TreeTracker.BuildConfig
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.*

class TreeBundleUploader {

    private var s3Client: AmazonS3? = null

    init {

        val basicAWSCredentials = BasicAWSCredentials(BuildConfig.OBJECT_STORAGE_ACCESS_KEY, BuildConfig.OBJECT_STORAGE_SECRET_KEY)
        val credentialsProvider = StaticCredentialsProvider(basicAWSCredentials)

        if (BuildConfig.USE_AWS_S3) {

            // Acceleration IS available in android SDK
            // https://github.com/aws-amplify/aws-sdk-android/blob/master/aws-android-sdk-s3/src/main/java/com/amazonaws/services/s3/S3ClientOptions.java#L114
            // https://github.com/aws-amplify/aws-sdk-android/issues/515

            val clientRegion = BuildConfig.OBJECT_STORAGE_ENDPOINT
            try {

                // Create an Amazon S3 client that is configured to use the accelerate endpoint.
                val region = Region.getRegion(Regions.fromName(clientRegion))
                s3Client = AmazonS3Client(credentialsProvider, region, ClientConfiguration())


            } catch (e : AmazonServiceException) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                e.printStackTrace()
                throw(e)

            } catch (e : Exception) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                e.printStackTrace()
                throw(e)
            }

        } else {
            s3Client = AmazonS3Client(credentialsProvider)
            s3Client?.setEndpoint(String.format("https://%s/", BuildConfig.OBJECT_STORAGE_ENDPOINT))

        }
    }

    suspend fun uploadTreeJsonBundle(jsonBundle: String, bundleId: String) {
        val byteArray = jsonBundle.toByteArray(Charsets.UTF_8)
        val inputStream = ByteArrayInputStream(byteArray)

        val acl = AccessControlList()
        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)


        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())

        val dosKey = timeStamp + '_'.toString() + UUID.randomUUID() + '_'.toString() + bundleId

        val objectMetadata = ObjectMetadata().apply {
            //contentMD5 = bundleId
            contentLength = inputStream.available().toLong()
        }

        val putObjectRequest = PutObjectRequest(BuildConfig.OBJECT_STORAGE_BUCKET_BATCH_UPLOADS,
                                                dosKey,
                                                inputStream,
                                                objectMetadata).apply {
            withAccessControlList(acl)
        }

        s3Client?.putObject(putObjectRequest)

    }
}