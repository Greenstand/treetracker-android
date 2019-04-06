package org.greenstand.android.TreeTracker.viewmodels

import android.content.ContentValues
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amazonaws.AmazonClientException
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.api.models.requests.AttributesRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.dao.TreeDto
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.managers.TreeManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.Utils
import timber.log.Timber
import java.io.File
import java.io.IOException

class DataViewModel : CoroutineViewModel() {

    private val totalTreesLiveData = MutableLiveData<Int>()
    private val treesUploadedLiveData = MutableLiveData<Int>()
    private val treesToSyncLiveData = MutableLiveData<Int>()
    private val toastLiveData = MutableLiveData<Int>()

    val totalTrees: LiveData<Int> = totalTreesLiveData
    val treesUploaded: LiveData<Int> = treesUploadedLiveData
    val treesToSync: LiveData<Int> = treesToSyncLiveData
    val toasts: LiveData<Int> = toastLiveData

    var currentJob: Job? = null

    fun sync() {
        launch {
            val treesToSync = TreeTrackerApplication.getAppDatabase().treeDao().getToSyncTreeCount()
            when (treesToSync) {
                0 -> {
                    currentJob?.cancel()
                    toastLiveData.value = R.string.nothing_to_sync
                }
                else -> {
                    toastLiveData.value = R.string.sync_started
                    startDataSynchronization()
                }
            }
        }
    }

    private fun updateData() {
        launch(Dispatchers.IO) {

            val treeCount = TreeTrackerApplication.getAppDatabase().treeDao().getTotalTreeCount()
            val syncedTreeCount = TreeTrackerApplication.getAppDatabase().treeDao().getSyncedTreeCount()
            val notSyncedTreeCount = TreeTrackerApplication.getAppDatabase().treeDao().getToSyncTreeCount()

            withContext(Dispatchers.Main) {
                totalTreesLiveData.value = treeCount
                treesUploadedLiveData.value = syncedTreeCount
                treesToSyncLiveData.value = notSyncedTreeCount
            }
        }
    }

    private fun startDataSynchronization() {
        //syncBtn?.setText(R.string.stop)
        //userId = mSharedPreferences!!.getLong(ValueHelper.MAIN_USER_ID, -1)
        currentJob = launch(Dispatchers.IO) {

            val isAuthenticated = UserManager.authenticateDevice().await()

            if (!isAuthenticated) {
                withContext(Dispatchers.Main) {
                    toastLiveData.value = R.string.sync_failed
                }
            } else {

                uploadUserIdentifications()

                uploadPlanterIdentifications()

                uploadNewTrees()

                withContext(Dispatchers.Main) {
                    //syncBtn?.setText(R.string.sync)
                    if (success) {
                        toastLiveData.value = R.string.sync_successful
                    } else {
                        toastLiveData.value = R.string.sync_failed
                    }
                }
            }
        }
    }

    private suspend fun uploadUserIdentifications() {
        // Upload all user registration data that hasn't been uploaded yet
        val registrations = TreeTrackerApplication.getAppDatabase().planterDao().getPlanterRegistrationsToUpload()
        registrations.forEach {
            uploadPlanterRegistration(it).await()
        }
    }

    private suspend fun uploadPlanterIdentifications() {
        // Get all planter_identifications without a photo_url
        val planterCursor: List<PlanterIdentificationsEntity> = TreeTrackerApplication.getAppDatabase().planterDao().getPlanterIdentificationsToUpload()

        planterCursor.forEach { planterIndentification ->

            uploadPlanterPhoto(planterIndentification).await()?.let { imageUrl ->

                // update the planter photo_url to reflect this
                planterIndentification.photoUrl = imageUrl
                TreeTrackerApplication.getAppDatabase().planterDao().updatePlanterIdentification(planterIndentification)
            }
        }
    }

    private suspend fun uploadNewTrees() {
        val treeList = TreeTrackerApplication.getAppDatabase().treeDao().getTreesToUpload()

        Timber.tag("DataFragment").d("treeCursor: $treeList")
        Timber.tag("DataFragment").d("treeCursor: " + treeList.size)

        treeList.onEach {
            val treeRequest = createTreeRequest(it)

            val uploadSuccess = if (treeRequest != null) {
                uploadNextTreeAsync(it, treeRequest).await()
            } else {
                Timber.e("TreeRequest creation failed")
                false
            }

            if (uploadSuccess) {
                updateData()
            } else {
                Timber.e("NewTree upload failed")
            }
        }
    }

    private fun uploadPlanterRegistration(planterDetailsEntity: PlanterDetailsEntity) = GlobalScope.async {
        val registration = RegistrationRequest(
            planterIdentifier = planterDetailsEntity.identifier,
            firstName = planterDetailsEntity.firstName,
            lastName = planterDetailsEntity.lastName,
            organization = planterDetailsEntity.organization
        )

        val result = Api.createPlanterRegistration(registration)
        if (result.isSuccessful) {
            planterDetailsEntity.uploaded = true
            TreeTrackerApplication.getAppDatabase().planterDao().updatePlanterDetails(planterDetailsEntity)
            return@async true
        } else {
            return@async false
        }
    }

    private fun uploadPlanterPhoto(planterIdentificationsEntity: PlanterIdentificationsEntity) = GlobalScope.async {
        /**
         * Implementation for saving image into DigitalOcean Spaces.
         */
        val imagePath = planterIdentificationsEntity.photoPath
        val imageUrl: String
        if (imagePath != null && imagePath != "") { // don't crash if image path is empty
            try {
                imageUrl = DOSpaces.instance().put(imagePath)
            } catch (ace: AmazonClientException) {
                Timber.d(
                    "Caught an AmazonClientException, which " +
                            "means the client encountered " +
                            "an internal error while trying to " +
                            "communicate with S3, " +
                            "such as not being able to access the network."
                )
                Timber.d("Error Message: " + ace.message)
                return@async null
            }

            Timber.d("imageUrl: $imageUrl")
            return@async imageUrl
        } else {
            return@async null
        }

    }

    private fun uploadNextTreeAsync(uploadedtree: TreeDto, newTreeRequest: NewTreeRequest) = GlobalScope.async {
        Timber.tag("DataFragment").d("tree_id: $uploadedtree.tree_id")
        /*
        * Save to the API
        */
        var treeIdResponse: Int? = null
        try {
            treeIdResponse = Api.createTree(newTreeRequest)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (treeIdResponse != null) {
            val values = ContentValues().apply {
                put("is_synced", "Y")
                put("main_db_id", treeIdResponse)
            }

            val missingTrees = TreeTrackerApplication.getAppDatabase().treeDao().getMissingTreeByID(uploadedtree.tree_id)
            if (missingTrees.isNotEmpty()) {
                TreeTrackerApplication.getAppDatabase().treeDao().deleteTree(missingTrees.first())
                val photos = TreeTrackerApplication.getAppDatabase().photoDao().getPhotosByTreeId(uploadedtree.tree_id)

                photos.forEach {
                    try {
                        val file = File(it.name)
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            } else {
                val treeList = TreeTrackerApplication.getAppDatabase().treeDao().getTreeByID(uploadedtree.tree_id)
                val treeEntity = treeList.first()
                treeEntity.isSynced = true
                treeEntity.mainDbId = treeIdResponse
                TreeTrackerApplication.getAppDatabase().treeDao().updateTree(treeEntity)


                val outdatedPhotos =
                    TreeTrackerApplication.getAppDatabase().photoDao().getOutdatedPhotos(uploadedtree.tree_id)

                outdatedPhotos.forEach {

                    try {
                        val file = File(it.name)
                        val deleted = file.delete()
                        if (deleted)
                            Timber.tag("DataFragment").d(
                                "delete file: "
                                        + it.name
                            )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
            return@async true
        } else {
            return@async false
        }

    }

    private suspend fun createTreeRequest(treeDto: TreeDto): NewTreeRequest? {

        suspend fun getImageUrl(): String? {
            /**
             * Implementation for saving image into DigitalOcean Spaces.
             */
            val imagePath = treeDto.name

            if (imagePath.isNullOrEmpty()) return null

            // don't crash if image path is empty
            return try {
                withContext(Dispatchers.IO) {
                    DOSpaces.instance().put(imagePath).also { Timber.d("imageUrl: $it") }
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

        val imageUrl = getImageUrl() ?: return null

        val attributesRequest = TreeManager.getTreeAttribute(treeDto.tree_id, TreeManager.TREE_COLOR_ATTR_KEY)?.let {
            AttributesRequest(
                heightColor = it,
                appVersion = TreeManager.getTreeAttribute(treeDto.tree_id, TreeManager.APP_VERSION_ATTR_KEY)!!,
                appBuild = TreeManager.getTreeAttribute(treeDto.tree_id, TreeManager.APP_BUILD_ATTR_KEY)!!,
                flavorId = TreeManager.getTreeAttribute(treeDto.tree_id, TreeManager.APP_FLAVOR_ATTR_KEY)!!
            )
        }

        return NewTreeRequest(
            uuid = treeDto.uuid,
            imageUrl = imageUrl,
            userId = UserManager.userId.toInt(),
            sequenceId = treeDto.tree_id,
            lat = treeDto.latitude,
            lon = treeDto.longitude,
            gpsAccuracy = treeDto.accuracy,
            planterIdentifier = treeDto.planter_identifier,
            planterPhotoUrl = treeDto.planter_photo_url,
            timestamp = Utils.convertDateToTimestamp(treeDto.tree_time_created!!),
            note = treeDto.note,
            attributes = attributesRequest
        )
    }

}