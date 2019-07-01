package org.greenstand.android.TreeTracker.api

import android.annotation.SuppressLint
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.SdkClientException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.BucketAccelerateConfiguration
import com.amazonaws.services.s3.model.BucketAccelerateStatus
import com.amazonaws.services.s3.model.GetBucketAccelerateConfigurationRequest
import com.amazonaws.services.s3.model.SetBucketAccelerateConfigurationRequest

import com.amazonaws.services.s3.model.AccessControlList
import com.amazonaws.services.s3.model.GroupGrantee
import com.amazonaws.services.s3.model.Permission
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult

import org.greenstand.android.TreeTracker.BuildConfig

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlin.String.Companion

/**
 * Created by lei on 12/11/17.
 */

class DOSpaces private constructor() {

    private var s3Client: AmazonS3? = null

    init {
        val basicAWSCredentials = BasicAWSCredentials(BuildConfig.DOS_ACCESS_KEY, BuildConfig.DOS_SECRET_KEY)
        val bucketName = BuildConfig.DO_SPACES_BUCKET
        val credentialsProvider = StaticCredentialsProvider(basicAWSCredentials)

        if(BuildConfig.ENABLE_TRANSFER_ACCELERATION) {

            val clientRegion = BuildConfig.DO_SPACES_ENDPOINT

            try {
                // Create an Amazon S3 client that is configured to use the accelerate endpoint.
                s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(credentialsProvider)
                    .enableAccelerateMode()
                    .build();

                // Enable Transfer Acceleration for the specified bucket.
                s3Client?.setBucketAccelerateConfiguration(
                    SetBucketAccelerateConfigurationRequest (bucketName,
                    BucketAccelerateConfiguration (
                            BucketAccelerateStatus.Enabled
                    )
                ));

                // Verify that transfer acceleration is enabled for the bucket.
                val accelerateStatus = s3Client?.getBucketAccelerateConfiguration (
                         GetBucketAccelerateConfigurationRequest (bucketName))
                    ?.status
                System.out.println("Bucket accelerate status: " + accelerateStatus);

                // Upload a new object using the accelerate endpoint.
                //s3Client?.putObject(bucketName, keyName, "Test object for transfer acceleration");
                //System.out.println("Object \"" + keyName + "\" uploaded with transfer acceleration.");
            } catch (e : AmazonServiceException) {
                // The call was transmitted successfully, but Amazon S3 couldn't process
                // it, so it returned an error response.
                e.printStackTrace();
            } catch (e : SdkClientException) {
                // Amazon S3 couldn't be contacted for a response, or the client
                // couldn't parse the response from Amazon S3.
                e.printStackTrace();
            }

        } else {

            s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .build()
            s3Client?.setEndpoint(String.format("https://%s.digitaloceanspaces.com/", BuildConfig.DO_SPACES_ENDPOINT))

        }




    }

    @SuppressLint("SimpleDateFormat")
    @Throws(AmazonClientException::class)
    fun put(path: String): String {
        val acl = AccessControlList()
        acl.grantPermission(GroupGrantee.AllUsers, Permission.Read)

        val image = File(path)
        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())

        val dosKey = timeStamp + '_'.toString() + UUID.randomUUID() + '_'.toString() + image.name
        val poRequest = PutObjectRequest(BuildConfig.DO_SPACES_BUCKET, dosKey, image)
        poRequest.withAccessControlList(acl)
        val poResult = s3Client?.putObject(poRequest)
        return String.format("https://%s.%s.digitaloceanspaces.com/%s",  BuildConfig.DO_SPACES_BUCKET,
            BuildConfig.DO_SPACES_ENDPOINT, dosKey)
    }

    companion object {


        private var sInstance: DOSpaces? = null

        fun instance(): DOSpaces {
            if (sInstance == null) {
                sInstance = DOSpaces()
            }
            return sInstance!!
        }
    }

}
