package org.greenstand.android.TreeTracker.api

import com.amazonaws.AmazonClientException
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.*
import io.reactivex.Observable
import kotlinx.coroutines.experimental.async

import org.greenstand.android.TreeTracker.BuildConfig

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import timber.log.Timber


/**
 * Created by lei on 12/11/17.
 */

class DOSpaces private constructor() {
    private val s3Client: AmazonS3

    init {
        val basicAWSCredentials = BasicAWSCredentials(BuildConfig.DOS_ACCESS_KEY, BuildConfig.DOS_SECRET_KEY)
        s3Client = AmazonS3Client(basicAWSCredentials)
        s3Client.setEndpoint(BuildConfig.DO_SPACES_ENDPOINT)
    }

    @Throws(AmazonClientException::class)
    fun put(path: String): String {
        val accessControlList = AccessControlList()
        accessControlList.grantPermission(GroupGrantee.AllUsers, Permission.Read)

        val image = File(path)
        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())
        val dosKey = timeStamp + '_'.toString() + UUID.randomUUID() + '_'.toString() + image.name

        val poRequest = PutObjectRequest(BuildConfig.DO_SPACES_BUCKET, dosKey, image)
        poRequest.withAccessControlList(accessControlList)

        val poResult = s3Client.putObject(poRequest)

        return String.format("https://%s.nyc3.digitaloceanspaces.com/%s", BuildConfig.DO_SPACES_BUCKET, dosKey)
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
