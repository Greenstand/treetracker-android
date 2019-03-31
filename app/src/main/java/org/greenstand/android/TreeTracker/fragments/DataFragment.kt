package org.greenstand.android.TreeTracker.fragments


import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.amazonaws.AmazonClientException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_data.view.*
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.api.models.requests.*
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.database.dao.TreeDto
import org.greenstand.android.TreeTracker.database.entity.PlanterDetailsEntity
import org.greenstand.android.TreeTracker.database.entity.PlanterIdentificationsEntity
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.Integer.valueOf

/**
 * Created by lei on 11/9/17.
 */

class DataFragment : Fragment(), View.OnClickListener {

    lateinit var totalTrees: TextView
    lateinit var locatedTrees: TextView
    lateinit var tosyncTrees: TextView

    private var progressDialog: ProgressDialog? = null
    private var mSharedPreferences: SharedPreferences? = null
    private var userId: Long = -1
    var numbersOfTreesToSync: Int = 0;
    var syncBtn: Button? = null

    private var operationAttempt: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mSharedPreferences = activity!!.getSharedPreferences(
            ValueHelper.NAME_SPACE, Context.MODE_PRIVATE
        )
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_data, container, false)
        totalTrees = v.fragmentDataTotalTreesValue
        locatedTrees = v.fragmentDataLocatedValue
        tosyncTrees = v.fragmentDataToSyncValue

        activity?.toolbarTitle?.setText(R.string.data)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        syncBtn = v.fragmentDataSync
        syncBtn?.setOnClickListener(this)

        return v
    }

    override fun onResume() {
        super.onResume()
        updateData()

        val extras = arguments
        if (extras != null) {
            if (extras.getBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN)) {
                resolvePendingUpdates()
                progressDialog = ProgressDialog(activity)
                progressDialog!!.setCancelable(false)
                progressDialog!!.setMessage(activity!!.getString(R.string.downloading_your_trees))
                progressDialog!!.show()
            }

            if (extras.getBoolean(ValueHelper.RUN_FROM_NOTIFICATION_SYNC)) {
                Toast.makeText(activity, R.string.sync_started, Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onPause() {
        super.onPause()
        if (operationAttempt != null) {
            operationAttempt!!.cancel()
            Toast.makeText(activity, R.string.sync_stopped, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startDataSynchronization() {
        syncBtn?.setText(R.string.stop)
        userId = mSharedPreferences!!.getLong(ValueHelper.MAIN_USER_ID, -1)
        operationAttempt = GlobalScope.launch(Dispatchers.IO) {

            var success: Boolean
            if (Api.instance().api != null) {
                success = identifyDevice().await()
            } else {
                success = false
            }

            if (!success) {
                withContext(Dispatchers.Main) {
                    syncBtn?.setText(R.string.sync)
                    Toast.makeText(activity, R.string.sync_failed, Toast.LENGTH_SHORT).show()
                }
            } else {

                val registrations = TreeTrackerApplication.getAppDatabase().planterDao().getPlanterRegistrationsToUpload()

                registrations.forEach {
                    success = uploadPlanterRegistration(it).await()
                }

                // Get all planter_identifications without a photo_url
                val planterCursor: List<PlanterIdentificationsEntity> = TreeTrackerApplication.getAppDatabase().planterDao().getPlanterIdentificationsToUpload()
                planterCursor.forEach {
                    val imageUrl = uploadPlanterPhoto(it).await()
                    if (imageUrl != null) {
                        // update the planter photo_url to reflect this
                        it.photoUrl = imageUrl
                        TreeTrackerApplication.getAppDatabase().planterDao().updatePlanterIdentification(it)
                    }
                }


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

                if (activity != null) {
                    withContext(Dispatchers.Main) {
                        syncBtn?.setText(R.string.sync)
                        if (success) {
                            Toast.makeText(activity, R.string.sync_successful, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, R.string.sync_failed, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun identifyDevice() = GlobalScope.async {

        val deviceId = Settings.Secure.getString(activity!!.contentResolver, Settings.Secure.ANDROID_ID)
        val authenticationRequest = AuthenticationRequest(deviceAndroidId = deviceId)

        try {
            val signInReponse = Api.instance().api!!.signIn(authenticationRequest).execute()
            if (!signInReponse.isSuccessful) {
                return@async false
            }
            val tokenResponse = signInReponse!!.body()
            Api.instance().setAuthToken(tokenResponse?.token!!)
        } catch (e: IOException) {
            return@async false
        }

        try {
            val response = Api.instance().api!!.updateDevice(DeviceRequest()).execute()
            if (!response.isSuccessful) {
                Timber.e("Device Message: ${response.message()}")
                Timber.e("Device Code: ${response.code()}")
                Timber.e("Device Code: ${response.errorBody()?.string()}")
            }
            return@async response.isSuccessful
        } catch (e: IOException) {
            return@async false
        }
    }

    private fun uploadPlanterRegistration(planterDetailsEntity: PlanterDetailsEntity) = GlobalScope.async {
        val registration = RegistrationRequest(
            planterIdentifier = planterDetailsEntity.identifier,
            firstName = planterDetailsEntity.firstName,
            lastName = planterDetailsEntity.lastName,
            organization = planterDetailsEntity.organization
        )

        val result = Api.instance().api!!.createPlanterRegistration(registration).execute()
        if (result != null) {
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

        val attributesRequest = treeDto.height_color?.let {
            AttributesRequest(
                heightColor = treeDto.height_color!!,
                appVersion = treeDto.app_version!!,
                appBuild = treeDto.app_build!!,
                flavorId = treeDto.flavor_id!!
            )
        }

        return NewTreeRequest(
            imageUrl = imageUrl,
            userId = userId.toInt(),
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

    private fun uploadNextTreeAsync(uploadedtree: TreeDto, newTreeRequest: NewTreeRequest) = GlobalScope.async {
        Timber.tag("DataFragment").d("tree_id: $uploadedtree.tree_id")
        /*
        * Save to the API
        */
        var postResult: PostResult? = null
        try {
            var treeResponse: Response<PostResult>? = null
            treeResponse = Api.instance().api!!.createTree(newTreeRequest).execute()
            postResult = treeResponse!!.body()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (postResult != null) {
            val treeIdResponse = postResult.status
            val values = ContentValues()
            values.put("is_synced", "Y")
            values.put("main_db_id", treeIdResponse)

            val missingTrees =
                TreeTrackerApplication.getAppDatabase().treeDao().getMissingTreeByID(uploadedtree.tree_id)
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

    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        val userId = mSharedPreferences!!.getString(ValueHelper.MAIN_DB_USER_ID, "-1")
        numbersOfTreesToSync = valueOf(tosyncTrees.text.toString())
        when (numbersOfTreesToSync) {
            0 -> {
                operationAttempt?.cancel();
                Toast.makeText(activity, R.string.nothing_to_sync, Toast.LENGTH_SHORT).show()
                syncBtn?.setText(R.string.sync)
            }
            else -> {
                Toast.makeText(activity, R.string.sync_started, Toast.LENGTH_SHORT).show()
                startDataSynchronization()
            }
        }
    }

    fun updateData() {

        GlobalScope.launch {
            val treeCount =
                TreeTrackerApplication.getAppDatabase().treeDao().getTotalTreeCount()

            val syncedTreeCount =
                TreeTrackerApplication.getAppDatabase().treeDao().getSyncedTreeCount()

            val notSyncedTreeCount =
                TreeTrackerApplication.getAppDatabase().treeDao().getToSyncTreeCount()

            withContext(Dispatchers.Main) {
                totalTrees.text = treeCount.toString()
                Timber.d("total $treeCount")
                locatedTrees.text = syncedTreeCount.toString()
                Timber.d("located $syncedTreeCount")
                tosyncTrees.text = notSyncedTreeCount.toString()
                Timber.d("to sync $notSyncedTreeCount")
            }
        }
    }

    fun resolvePendingUpdates() {
        updateData()
        val pendingUpdates = TreeTrackerApplication.getAppDatabase().treeDao().getPendingUpdates()

        val trees = (activity as MainActivity).userTrees

        if (trees != null && pendingUpdates.isNotEmpty()) {
            //UpdateLocalDb().execute(trees)
        }

    }

}
