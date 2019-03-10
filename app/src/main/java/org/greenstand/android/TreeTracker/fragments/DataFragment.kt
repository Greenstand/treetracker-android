package org.greenstand.android.TreeTracker.fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.DatabaseUtils
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.amazonaws.AmazonClientException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_data.view.*
import kotlinx.coroutines.*


import kotlinx.coroutines.android.UI

import org.greenstand.android.TreeTracker.BuildConfig

import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.api.models.requests.AuthenticationRequest
import org.greenstand.android.TreeTracker.api.models.requests.DeviceRequest
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.requests.RegistrationRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.*
import retrofit2.Response

import java.io.File
import java.io.IOException
import timber.log.Timber
import java.lang.Integer.valueOf

/**
 * Created by lei on 11/9/17.
 */

class DataFragment : Fragment(), View.OnClickListener {

    lateinit var totalTrees: TextView
    lateinit var updateTrees: TextView
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
                ValueHelper.NAME_SPACE, Context.MODE_PRIVATE)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_data, container, false)
        totalTrees = v.fragmentDataTotalTreesValue
        updateTrees = v.fragmentDataUpdateValue
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
        userId = mSharedPreferences!!.getLong(ValueHelper.MAIN_USER_ID, -1);
        operationAttempt = launch(UI) {

            var success: Boolean
            if(Api.instance().api != null){
                success = identifyDevice().await()
            } else {
                success = false
            }

            if(!success){
                Toast.makeText(activity, R.string.sync_failed, Toast.LENGTH_SHORT).show()
            } else {

                val registrationsCursor = getPlanterRegistrationsToUploadCursor().await()
                while(registrationsCursor.moveToNext()){
                    success = uploadPlanterRegistration(registrationsCursor).await()
                }

                // Get all planter_identifications without a photo_url
                val planterCursor = getPlanterIdentificationsToUploadCursor().await()
                while(planterCursor.moveToNext()){
                    val imageUrl = uploadPlanterPhoto(planterCursor).await()
                    if(imageUrl != null){
                        // update the planter photo_url to reflect this
                        val planterIdentificationsId = planterCursor.getString(planterCursor.getColumnIndex("_id"))
                        val values = ContentValues()
                        values.put("photo_url", imageUrl)
                        TreeTrackerApplication.getDatabaseManager().update("planter_identifications",
                                values, "_id = ?", arrayOf(planterIdentificationsId))
                    }
                }


                val treeCursor = getTreesToUploadCursor().await()
                Timber.tag("DataFragment").d("treeCursor: " + DatabaseUtils.dumpCursorToString(treeCursor))
                Timber.tag("DataFragment").d("treeCursor: " + treeCursor.count)

                while (treeCursor.moveToNext()) {

                    val localTreeId = treeCursor.loadLong("tree_id").toString()

                    val treeRequest = createTreeRequest(treeCursor)

                    val uploadSuccess = if (treeRequest != null) {
                        uploadNextTreeAsync(localTreeId, treeRequest).await()
                    } else {
                        Timber.e("TreeRequest creation failed")
                        false
                    }

                    if (uploadSuccess) {
                        updateData()
                    } else {
                        Timber.e("NewTree upload failed")
                        break
                    }
                }

                if(activity != null) {
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

    private fun identifyDevice() = async {

        val deviceId = Settings.Secure.getString(activity!!.contentResolver, Settings.Secure.ANDROID_ID)
        val authenticationRequest = AuthenticationRequest(deviceAndroidId = deviceId)

        try {
            val signInReponse = Api.instance().api!!.signIn(authenticationRequest).execute()
            if (!signInReponse.isSuccessful) {
                return@async false
            }
            val tokenResponse = signInReponse!!.body()
            Api.instance().setAuthToken(tokenResponse?.token!!)
        } catch ( e: IOException) {
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
        } catch ( e: IOException) {
            return@async false
        }
    }

    private fun getTreesToUploadCursor() = async {
        TreeTrackerApplication.getDatabaseManager().openDatabase()
        val query = "SELECT " +
                "tree._id as tree_id, " +
                "tree.time_created as tree_time_created, " +
                "tree.is_synced, " +
                "location.lat, " +
                "location.long, " +
                "location.accuracy, " +
                "photo.name, " +
                "note.content, " +
                "planter_identifications.identifier as planter_identifier, " +
                "planter_identifications.photo_path as planter_photo_path, " +
                "planter_identifications.photo_url as planter_photo_url, " +
                "planter_identifications._id as planter_identifications_id " +
                "FROM tree " +
                "LEFT OUTER JOIN location ON location._id = tree.location_id " +
                "LEFT OUTER JOIN tree_photo ON tree._id = tree_photo.tree_id " +
                "LEFT OUTER JOIN photo ON photo._id = tree_photo.photo_id " +
                "LEFT OUTER JOIN tree_note ON tree._id = tree_note.tree_id " +
                "LEFT OUTER JOIN note ON note._id = tree_note.note_id " +
                "LEFT OUTER JOIN planter_identifications ON  planter_identifications._id = tree.planter_identification_id  " +
                "WHERE " +
                "is_synced = 'N'"

        return@async TreeTrackerApplication.getDatabaseManager().queryCursor(query, null)
    }

    private fun getPlanterIdentificationsToUploadCursor() = async {
        TreeTrackerApplication.getDatabaseManager().openDatabase()
        val query = "SELECT * FROM planter_identifications WHERE photo_url IS NULL"
        return@async TreeTrackerApplication.getDatabaseManager().queryCursor(query, null)
    }

    private fun getPlanterRegistrationsToUploadCursor() = async {
        TreeTrackerApplication.getDatabaseManager().openDatabase()
        val query = "SELECT * FROM planter_details WHERE uploaded = 'N'"
        return@async TreeTrackerApplication.getDatabaseManager().queryCursor(query, null)
    }

    private fun uploadPlanterRegistration(registrationsCursor: Cursor) = async {
        val registration = RegistrationRequest(
            planterIdentifier = registrationsCursor.loadString("identifier"),
            firstName = registrationsCursor.loadString("first_name"),
            lastName = registrationsCursor.loadString("last_name"),
            organization = registrationsCursor.loadString("organization")
        )

        val result = Api.instance().api!!.createPlanterRegistration(registration).execute()
        if(result != null) {
            val id = registrationsCursor.getString(registrationsCursor.getColumnIndex("_id"))
            val values = ContentValues()
            values.put("uploaded", "Y")
            TreeTrackerApplication.getDatabaseManager().update("planter_details", values, "_id = ?",
                arrayOf(id))
            return@async true
        } else {
            return@async false
        }
    }

    private fun uploadPlanterPhoto(planterCursor: Cursor) = async {
        /**
         * Implementation for saving image into DigitalOcean Spaces.
         */
        val imagePath = planterCursor.getString(planterCursor.getColumnIndex("photo_path"))
        val imageUrl: String
        if (imagePath != null && imagePath != "") { // don't crash if image path is empty
            try {
                imageUrl = DOSpaces.instance().put(imagePath)
            } catch (ace: AmazonClientException) {
                Timber.d("Caught an AmazonClientException, which " +
                        "means the client encountered " +
                        "an internal error while trying to " +
                        "communicate with S3, " +
                        "such as not being able to access the network.")
                Timber.d("Error Message: " + ace.message)
                return@async null
            }

            Timber.d("imageUrl: $imageUrl")
            return@async imageUrl
        } else {
            return@async null
        }

    }

    private suspend fun createTreeRequest(treeCursor: Cursor): NewTreeRequest? {

        suspend fun getImageUrl(): String? {
            /**
             * Implementation for saving image into DigitalOcean Spaces.
             */
            val imagePath = treeCursor.getString(treeCursor.getColumnIndex("name"))

            if (imagePath.isNullOrEmpty()) return null

            // don't crash if image path is empty
            return try {
                withContext(Dispatchers.IO) {
                    DOSpaces.instance().put(imagePath).also { Timber.d("imageUrl: $it") }
                }
            } catch (ace: AmazonClientException) {
                Timber.e("Caught an AmazonClientException, which " +
                                 "means the client encountered " +
                                 "an internal error while trying to " +
                                 "communicate with S3, " +
                                 "such as not being able to access the network.")
                Timber.e("Error Message: ${ace.message}")
                null
            }
        }

        val imageUrl = getImageUrl() ?: return null

        return with(treeCursor) {
            NewTreeRequest(
                imageUrl = imageUrl,
                userId = userId.toInt(),
                sequenceId = loadLong("tree_id"),
                lat = loadDouble("lat"),
                lon = loadDouble("long"),
                gpsAccuracy = loadFloat("accuracy").toInt(),
                planterIdentifier = loadString("planter_identifier"),
                planterPhotoUrl = loadString("planter_photo_url"),
                timestamp = Utils.convertDateToTimestamp(loadString("tree_time_created")!!),
                note = loadString("content").orEmpty()
            )
        }
    }

    private fun uploadNextTreeAsync(localTreeId: String, newTreeRequest: NewTreeRequest) = async{
        Timber.tag("DataFragment").d("tree_id: $localTreeId")
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
            val isMissingCursor = TreeTrackerApplication.getDatabaseManager()
                    .queryCursor("SELECT is_missing FROM tree WHERE is_missing = 'Y' AND _id = $localTreeId",
                        null)
            if (isMissingCursor.moveToNext()) {
                TreeTrackerApplication.getDatabaseManager().delete("tree", "_id = ?", arrayOf(localTreeId))
                val photoQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where tree_id = $localTreeId"
                val photoCursor = TreeTrackerApplication.getDatabaseManager().queryCursor(photoQuery, null)
                while (photoCursor.moveToNext()) {
                    try {
                        val file = File(photoCursor.getString(photoCursor.getColumnIndex("name")))
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            } else {
                TreeTrackerApplication.getDatabaseManager().update("tree", values, "_id = ?",
                    arrayOf(localTreeId))
                val outDatedQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where is_outdated = 'Y' and tree_id = $localTreeId"
                val outDatedPhotoCursor = TreeTrackerApplication.getDatabaseManager().queryCursor(outDatedQuery, null)
                while (outDatedPhotoCursor.moveToNext()) {
                    try {
                        val file = File(outDatedPhotoCursor.getString(outDatedPhotoCursor.getColumnIndex("name")))
                        val deleted = file.delete()
                        if (deleted)
                            Timber.tag("DataFragment").d("delete file: "
                                    + outDatedPhotoCursor.getString(outDatedPhotoCursor.getColumnIndex("name")))
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
        when(numbersOfTreesToSync) {
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
//        when (v.id) {
//            R.id.fragment_data_sync -> {
//                Toast.makeText(activity, "Start syncing", Toast.LENGTH_SHORT).show()
//                startDataSynchronization()
//            }
//            R.id.fragment_data_pause -> {
//                operationAttempt?.cancel();
//                Toast.makeText(activity, "Pause syncing", Toast.LENGTH_SHORT).show()
//            }
//            R.id.fragment_data_resume -> {
//                Toast.makeText(activity, "Resume syncing", Toast.LENGTH_SHORT).show()
//                startDataSynchronization()
//            }
//        }

    }

    fun updateData() {
        TreeTrackerApplication.getDatabaseManager().openDatabase()

        var treeCursor = TreeTrackerApplication.getDatabaseManager().queryCursor("SELECT COUNT(*) AS total FROM tree", null)
        treeCursor.moveToFirst()
        totalTrees.text = treeCursor.getString(treeCursor.getColumnIndex("total"))
        Timber.d("total ${treeCursor.loadString("total")}")

        /*treeCursor = mDatabaseManager.queryCursor("SELECT COUNT(*) AS updated FROM tree WHERE is_synced = 'Y' AND time_for_update < DATE('NOW')", null);
        treeCursor.moveToFirst();
        updateTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("updated")));
        Timber.d("updated " + treeCursor.getString(treeCursor.getColumnIndex("updated")));

        treeCursor = mDatabaseManager.queryCursor("SELECT COUNT(*) AS located FROM tree WHERE is_synced = 'Y' AND time_for_update >= DATE('NOW')", null);
        treeCursor.moveToFirst();
        locatedTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("located")));
        Timber.d("located " + treeCursor.getString(treeCursor.getColumnIndex("located")));
        */

        treeCursor = TreeTrackerApplication.getDatabaseManager()
                .queryCursor("SELECT COUNT(*) AS located FROM tree WHERE is_synced = 'Y'", null)
        treeCursor.moveToFirst()
        locatedTrees.text = treeCursor.getString(treeCursor.getColumnIndex("located"))
        Timber.d("located " + treeCursor.getString(treeCursor.getColumnIndex("located")))

        treeCursor = TreeTrackerApplication.getDatabaseManager()
                .queryCursor("SELECT COUNT(*) AS tosync FROM tree WHERE is_synced = 'N'", null)
        treeCursor.moveToFirst()
        tosyncTrees.text = treeCursor.getString(treeCursor.getColumnIndex("tosync"))
        Timber.d("to sync " + treeCursor.getString(treeCursor.getColumnIndex("tosync")))

    }

    fun resolvePendingUpdates() {
        updateData()
        TreeTrackerApplication.getDatabaseManager().openDatabase()

        val treeCursor = TreeTrackerApplication.getDatabaseManager()
                .queryCursor("SELECT DISTINCT tree_id FROM pending_updates WHERE tree_id NOT NULL and tree_id <> 0", null)
        val trees = (activity as MainActivity).userTrees

        if (trees != null && treeCursor.moveToFirst()) {
            //UpdateLocalDb().execute(trees)
        }

    }


/*    internal inner class UpdateLocalDb : AsyncTask<List<UserTree>, Void, Void>() {

        override fun doInBackground(lists: Array<List<UserTree>>): Void? {
            val trees = lists[0]
            for (tree in trees) {
                mDatabaseManager.openDatabase()
                val photoPath = saveImage(tree.imageUrl)
                var photoId: Long = -1

                //add photo
                val photoValues = ContentValues()
                photoValues.put("lat", tree.lat)
                photoValues.put("long", tree.lng)
                //                photoValues.put("accuracy", tree.getGps());
                val locationId = mDatabaseManager.insert("location", null, photoValues)

                //add location
                val locationValues = ContentValues()
                locationValues.put("location_id", locationId)
                locationValues.put("name", photoPath)
                photoId = mDatabaseManager.insert("photo", null, locationValues)

                //                Date date = new Date();
                //                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                //                Calendar calendar = Calendar.getInstance();
                //                calendar.setTime(date);
                //                calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(tree.getNextUpdate()));
                //                date = calendar.getTime();

                val treeExists = mDatabaseManager.queryCursor("SELECT * FROM tree WHERE main_db_id = " + tree.id!!, null)
                var treeId: Long = -1
                val priority = if (tree.priority == "1") "Y" else "N"

                if (treeExists.moveToNext()) {
                    treeId = java.lang.Long.parseLong(treeExists.getString(treeExists.getColumnIndex("_id")))
                    val photoOutdated = "select photo._id from tree_photo left outer join photo on photo_id = photo._id where tree_id = " +
                            "" + treeExists.getString(treeExists.getColumnIndex("_id"))
                    Timber.tag("photoOutDated").d(photoOutdated)
                    val photoCursor = mDatabaseManager.queryCursor(photoOutdated, null)
                    while (photoCursor.moveToNext()) {
                        val values = ContentValues()
                        values.put("is_outdated", "Y")

                        mDatabaseManager.update("photo", values, "_id = ?", arrayOf(photoCursor.getString(photoCursor.getColumnIndex("_id"))))
                    }

                    val treeValues = ContentValues()
                    treeValues.put("time_created", tree.created)
                    treeValues.put("time_updated", tree.updated)
                    //                    treeValues.put("time_for_update", dateFormat.format(date));
                    treeValues.put("is_synced", "Y")
                    treeValues.put("is_priority", priority ?: "N")
                    treeValues.put("location_id", locationId)
                    mDatabaseManager.update("tree", treeValues, "_id = ?", arrayOf(treeExists.getString(treeExists.getColumnIndex("_id"))))
                } else {
                    val treeValues = ContentValues()
                    treeValues.put("time_created", tree.created)
                    treeValues.put("time_updated", tree.updated)
                    //                    treeValues.put("time_for_update", dateFormat.format(date));
                    treeValues.put("is_synced", "Y")
                    treeValues.put("is_priority", priority ?: "N")
                    treeValues.put("main_db_id", tree.id)
                    treeValues.put("location_id", locationId)
                    treeId = mDatabaseManager.insert("tree", null, treeValues)
                }

                if (photoId != -1L) {
                    // tree_photo
                    val values = ContentValues()
                    values.put("tree_id", treeId)
                    values.put("photo_id", photoId)
                    val treePhotoId = mDatabaseManager.insert("tree_photo", null, values)
                }

                //                Cursor pendingUpdateCursor = mDatabaseManager.queryCursor("SELECT main_db_id FROM pending_updates WHERE main_db_id IS NOT NULL AND tree_id = " + tree.getId(), null);
                //                if (pendingUpdateCursor.moveToFirst()) {
                //                    clearPendingUpdate = new ClearPendingUpdateTask().execute(new String[]{pendingUpdateCursor.getString(pendingUpdateCursor.getColumnIndex("main_db_id"))});
                //                }

                // mDatabaseManager.delete("pending_updates", "tree_id = ?", arrayOf<String>(tree.id ?: -))
            }
            return null
        }

        override fun onProgressUpdate(vararg values: Void) {
            super.onProgressUpdate(*values)

        }

        override fun onPostExecute(aVoid: Void) {
            super.onPostExecute(aVoid)
            mSharedPreferences!!.edit().putBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false).commit()
            updateData()
            if (progressDialog != null) {
                progressDialog!!.dismiss()
            }
            val fm = activity.supportFragmentManager
            for (entry in 0 until fm.backStackEntryCount) {
                Timber.d("CheckFragmentBackStack", "Found fragment: " + fm.getBackStackEntryAt(entry).name)
            }
            if (fm.backStackEntryCount > 1) {
                fm.popBackStack()
            }

        }
    }
    */

}
