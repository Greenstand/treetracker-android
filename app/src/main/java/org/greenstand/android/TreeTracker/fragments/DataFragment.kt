package org.greenstand.android.TreeTracker.fragments

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers

import org.greenstand.android.TreeTracker.database.DatabaseManager
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.managers.SyncTask
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.api.models.responses.UserTree

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import timber.log.Timber

/**
 * Created by lei on 11/9/17.
 */

class DataFragment : Fragment(), View.OnClickListener, SyncTask.SyncTaskListener {

    private val databaseManager: DatabaseManager
    private var totalTrees: TextView? = null
    private var updateTrees: TextView? = null
    private var locatedTrees: TextView? = null
    private var tosyncTrees: TextView? = null
    private var progressDialog: ProgressDialog? = null
    private var mSharedPreferences: SharedPreferences? = null

    private var syncTask: SyncTask? = null  //AsyncTask<Void, Int, String>? = null
    private val dataDisposables: CompositeDisposable

    private val albumName: String
        get() = getString(R.string.album_name)

    private val albumDir: File?
        get() {
            var storageDir: File? = null

            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {

                val cw = ContextWrapper(activity.applicationContext)
                storageDir = cw.getDir("treeImages", Context.MODE_PRIVATE)

                if (storageDir != null) {
                    if (!storageDir.mkdirs()) {
                        if (!storageDir.exists()) {
                            //Timber.d("CameraSample", "failed to create directory")
                            return null
                        }
                    }
                }

            } else {
                Log.v(getString(R.string.app_name),
                        "External storage is not mounted READ/WRITE.")
            }

            return storageDir
        }

    init {
        databaseManager = DatabaseManager.getInstance(MainActivity.dbHelper!!)
        dataDisposables = CompositeDisposable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mSharedPreferences = activity.getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE)

        val userID = mSharedPreferences!!.getString(ValueHelper.MAIN_DB_USER_ID, "-1")
        syncTask = SyncTask(activity, this, Integer.parseInt(userID))
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        menu!!.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_data, container, false)
        totalTrees = v.findViewById(R.id.fragment_data_total_trees_value) as TextView
        updateTrees = v.findViewById(R.id.fragment_data_update_value) as TextView
        locatedTrees = v.findViewById(R.id.fragment_data_located_value) as TextView
        tosyncTrees = v.findViewById(R.id.fragment_data_to_sync_value) as TextView

        (activity.findViewById(R.id.toolbar_title) as TextView).setText(R.string.data)
        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val syncBtn = v.findViewById(R.id.fragment_data_sync) as Button
        syncBtn.setOnClickListener(this)

        val pauseBtn = v.findViewById(R.id.fragment_data_pause) as Button
        pauseBtn.setOnClickListener(this)

        val resumeBtn = v.findViewById(R.id.fragment_data_resume) as Button
        resumeBtn.setOnClickListener(this)

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
                progressDialog?.setCancelable(false)
                progressDialog?.setMessage(activity.getString(R.string.downloading_your_trees))
                progressDialog?.show()
            }

            if (extras.getBoolean(ValueHelper.RUN_FROM_NOTIFICATION_SYNC)) {
                //                syncTreesTask = new DataFragmentBK.SyncTreesTask().execute(new String[]{});
                Toast.makeText(activity, "Start syncing", Toast.LENGTH_SHORT).show()
            }
        }

        // User exited from app
        //        if (MainActivity.syncDataFromExitScreen == true) {
        //            MainActivity.syncDataFromExitScreen = false;
        //            syncFromExitStarted = true;
        //            syncTreesTask = new DataFragmentBK.SyncTreesTask().execute(new String[]{});
        //            Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();
        //        }
    }

    override fun onPause() {
        super.onPause()
        if (syncTask != null) {
            //syncTask!!.cancel(true)
            dataDisposables.clear()
            Toast.makeText(activity, "Sync stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        val userID = mSharedPreferences!!.getString(ValueHelper.MAIN_DB_USER_ID, "-1")
        when (v.id) {
            R.id.fragment_data_sync -> {
                Toast.makeText(context, "Started syncing", Toast.LENGTH_SHORT).show()
                //syncTask = SyncTask(activity, this, Integer.parseInt(userID)).execute()
                syncTrees()
            }
            R.id.fragment_data_pause -> {
                if(syncTask != null) {
                    //syncTask!!.cancel(true)
                    dataDisposables.dispose()
                    Toast.makeText(activity, "Paused Sync", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.fragment_data_resume -> {
                Toast.makeText(activity, "Resuming Sync", Toast.LENGTH_SHORT).show()
                //syncTask = SyncTask(activity, this, Integer.parseInt(userID)).execute()
                syncTrees()
            }
        }
    }

    fun syncTrees() {
        dataDisposables.add(
                Observable.create(ObservableOnSubscribe<Int> { syncTask?.syncTreesForUser() })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result -> Toast.makeText(context, "Successfully Synced $result Trees!", Toast.LENGTH_LONG).show()},
                                { error -> Toast.makeText(context, "Sync Failed: ${error.message}", Toast.LENGTH_LONG).show() }
                        ))
    }

    fun updateData() {
        databaseManager.openDatabase()

        var treeCursor = databaseManager.queryCursor("SELECT COUNT(*) AS total FROM tree", null)
        treeCursor.moveToFirst()
        totalTrees!!.text = treeCursor.getString(treeCursor.getColumnIndex("total"))
        Timber.d("total " + treeCursor.getString(treeCursor.getColumnIndex("total")))

        /*treeCursor = databaseManager.queryCursor("SELECT COUNT(*) AS updated FROM tree WHERE is_synced = 'Y' AND time_for_update < DATE('NOW')", null);
        treeCursor.moveToFirst();
        updateTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("updated")));
        Timber.d("updated " + treeCursor.getString(treeCursor.getColumnIndex("updated")));
        treeCursor = databaseManager.queryCursor("SELECT COUNT(*) AS located FROM tree WHERE is_synced = 'Y' AND time_for_update >= DATE('NOW')", null);
        treeCursor.moveToFirst();
        locatedTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("located")));
        Timber.d("located " + treeCursor.getString(treeCursor.getColumnIndex("located")));
        */

        treeCursor = databaseManager.queryCursor("SELECT COUNT(*) AS located FROM tree WHERE is_synced = 'Y'", null)
        treeCursor.moveToFirst()
        locatedTrees!!.text = treeCursor.getString(treeCursor.getColumnIndex("located"))
        Timber.d("located " + treeCursor.getString(treeCursor.getColumnIndex("located")))

        treeCursor = databaseManager.queryCursor("SELECT COUNT(*) AS tosync FROM tree WHERE is_synced = 'N'", null)
        treeCursor.moveToFirst()
        tosyncTrees!!.text = treeCursor.getString(treeCursor.getColumnIndex("tosync"))
        Timber.d("to sync " + treeCursor.getString(treeCursor.getColumnIndex("tosync")))

    }

    fun resolvePendingUpdates() {

        updateData()
        databaseManager.openDatabase()

        val treeCursor = databaseManager.queryCursor("SELECT DISTINCT tree_id FROM pending_updates WHERE tree_id NOT NULL and tree_id <> 0", null)
        val trees = (activity as MainActivity).userTrees

        if (trees != null && treeCursor.moveToFirst()) {
            //UpdateLocalDB().execute(trees)
            Observable.fromCallable {
                (this::updateLocalDB)(trees)
            }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { _ -> onPostExecuteUpdateLocalDB() },
                            { error -> Timber.d("Couldn't update local DB: ${error.message}")}
                    )
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = ValueHelper.JPEG_FILE_PREFIX + timeStamp + "_"
        val albumF = albumDir

        return File.createTempFile(imageFileName, ValueHelper.JPEG_FILE_SUFFIX, albumF)
    }

    private fun saveImage(sUrl: String?): String {
        val bitmap: Bitmap
        var file: File? = null
        var out: FileOutputStream? = null
        try {
            val inputStream = URL(sUrl).openStream()
            bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            file = createImageFile()
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } catch (e: IOException) {
            Timber.tag("saveImage").d("Exception 1, Something went wrong!")
            e.printStackTrace()
        } finally {
            try {
                out!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        Timber.tag("saveImage").d("filePath: " + file!!.absolutePath)
        return file.absolutePath
    }

    override fun onPostExecute(message: String) {
        updateData()
        if (activity != null && activity.isFinishing != true) {
            Toast.makeText(activity, "Sync $message", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onProgressUpdate(vararg values: Int) {
        updateData()
    }

    /** Update local sqlite database **/
    private fun updateLocalDB(listOfTrees: List<UserTree>) {
        //val trees = listOfTrees
        for (tree in listOfTrees) {
            databaseManager.openDatabase()
            val photoPath = saveImage(tree.imageUrl)
            var photoId: Long = -1

            //add photo
            val photoValues = ContentValues()
            photoValues.put("lat", tree.lat)
            photoValues.put("long", tree.lng)
            //                photoValues.put("accuracy", tree.getGps());
            val locationId = databaseManager.insert("location", null, photoValues)

            //add location
            val locationValues = ContentValues()
            locationValues.put("location_id", locationId)
            locationValues.put("name", photoPath)
            photoId = databaseManager.insert("photo", null, locationValues)

            //                Date date = new Date();
            //                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //                Calendar calendar = Calendar.getInstance();
            //                calendar.setTime(date);
            //                calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(tree.getNextUpdate()));
            //                date = calendar.getTime();

            val treeExists = databaseManager.queryCursor("SELECT * FROM tree WHERE main_db_id = " + tree.id!!, null)
            var treeId: Long = -1

            if (treeExists.moveToNext()) {
                treeId = java.lang.Long.parseLong(treeExists.getString(treeExists.getColumnIndex("_id")))
                val photoOutdated = "select photo._id from tree_photo left outer join photo on photo_id = photo._id where tree_id = " +
                        "" + treeExists.getString(treeExists.getColumnIndex("_id"))
                Timber.tag("photoOutDated").d(photoOutdated)
                val photoCursor = databaseManager.queryCursor(photoOutdated, null)
                while (photoCursor.moveToNext()) {
                    val values = ContentValues()
                    values.put("is_outdated", "Y")

                    databaseManager.update("photo", values, "_id = ?", arrayOf(photoCursor.getString(photoCursor.getColumnIndex("_id"))))
                }
                val treeValues = getTreeContentValues(tree, locationId)
                databaseManager.update("tree", treeValues, "_id = ?", arrayOf(treeExists.getString(treeExists.getColumnIndex("_id"))))
            } else {
                val treeValues = getTreeContentValues(tree, locationId)
                treeId = databaseManager.insert("tree", null, treeValues)
            }

            if (photoId != -1L) {
                // tree_photo
                val values = ContentValues()
                values.put("tree_id", treeId)
                values.put("photo_id", photoId)
                val treePhotoId = databaseManager.insert("tree_photo", null, values)
            }

            //                Cursor pendingUpdateCursor = databaseManager.queryCursor("SELECT main_db_id FROM pending_updates WHERE main_db_id IS NOT NULL AND tree_id = " + tree.getId(), null);
            //                if (pendingUpdateCursor.moveToFirst()) {
            //                    clearPendingUpdate = new ClearPendingUpdateTask().execute(new String[]{pendingUpdateCursor.getString(pendingUpdateCursor.getColumnIndex("main_db_id"))});
            //                }

            databaseManager.delete("pending_updates", "tree_id = ?", arrayOf<String>(tree.id!!))
        }
    }

    /** Convert user tree to content values **/
    private fun getTreeContentValues(tree: UserTree, locationID: Long): ContentValues {

        val priority = if (tree.priority == "1") "Y" else "N"
        val treeValues = ContentValues()
        treeValues.put("time_created", tree.created)
        treeValues.put("time_updated", tree.updated)
        //                    treeValues.put("time_for_update", dateFormat.format(date));
        treeValues.put("is_synced", "Y")
        treeValues.put("is_priority", priority ?: "N")
        treeValues.put("main_db_id", tree.id)
        treeValues.put("location_id", locationID)

        return treeValues
    }

    private fun onPostExecuteUpdateLocalDB() {
        mSharedPreferences!!.edit().putBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false).apply()
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