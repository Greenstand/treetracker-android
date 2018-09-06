package org.greenstand.android.TreeTracker.managers

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.os.AsyncTask
import android.util.Log

import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.database.DatabaseManager
import com.amazonaws.AmazonClientException

import java.io.File
import java.io.IOException

import retrofit2.Response
import timber.log.Timber

/**
 * Created by lei on 11/11/17.
 */

class SyncTask(private val mContext: Context, private val callback: SyncTaskListener, private val userId: Int) : AsyncTask<Void, Int, String>() {
    private val mDatabaseManager: DatabaseManager
    private val mDataManager: DataManager<*>
    private var value: Int = 0

    interface SyncTaskListener {
        fun onPostExecute(message: String)

        fun onProgressUpdate(vararg values: Int?)
    }

    init {
        this.mDatabaseManager = DatabaseManager.getInstance(MainActivity.dbHelper!!)
        this.mDataManager = object : DataManager<PostResult>() {
            override fun onDataLoaded(data: PostResult) {

            }

            override fun onRequestFailed(message: String) {

            }
        }
    }

    override fun doInBackground(vararg voids: Void): String {
        mDatabaseManager.openDatabase()
        val query = "SELECT " +
                "tree._id as tree_id, " +
                "tree.time_created as tree_time_created, " +
                "tree.is_synced, " +
                "location.lat, " +
                "location.long, " +
                "location.accuracy, " +
                "photo.name, " +
                "note.content " +
                "FROM tree " +
                "LEFT OUTER JOIN location ON location._id = tree.location_id " +
                "LEFT OUTER JOIN tree_photo ON tree._id = tree_photo.tree_id " +
                "LEFT OUTER JOIN photo ON photo._id = tree_photo.photo_id " +
                "LEFT OUTER JOIN tree_note ON tree._id = tree_note.tree_id " +
                "LEFT OUTER JOIN note ON note._id = tree_note.note_id " +
                "WHERE " +
                "is_synced = 'N'"

        val treeCursor = mDatabaseManager.queryCursor(query, null)
        Timber.tag("DataFragment").d("treeCursor: " + DatabaseUtils.dumpCursorToString(treeCursor))
        value = treeCursor.count
        Timber.tag("DataFragment").d("treeCursor: " + treeCursor.count)

        while (treeCursor.moveToNext()) {
            val localTreeId = treeCursor.getLong(treeCursor.getColumnIndex("tree_id")).toString()
            Timber.tag("DataFragment").d("tree_id: $localTreeId")

            val newTree = NewTreeRequest()
            newTree.userId = userId
            newTree.sequenceId = treeCursor.getLong(treeCursor.getColumnIndex("tree_id"))
            newTree.lat = treeCursor.getDouble(treeCursor.getColumnIndex("lat"))
            newTree.lon = treeCursor.getDouble(treeCursor.getColumnIndex("long"))
            newTree.setGpsAccuracy(treeCursor.getFloat(treeCursor.getColumnIndex("accuracy")).toInt())
            var note: String? = treeCursor.getString(treeCursor.getColumnIndex("content"))
            if (note == null) {
                note = ""
            }
            newTree.note = note
            val timeCreated = treeCursor.getString(treeCursor.getColumnIndex("tree_time_created"))
            newTree.timestamp = Utils.convertDateToTimestamp(timeCreated)

            /**
             * Implementation for saving image into DigitalOcean Spaces.
             */
            val imagePath = treeCursor.getString(treeCursor.getColumnIndex("name"))
            val imageUrl: String
            if (imagePath != null && imagePath !== "") { // don't crash if image path is empty
                try {
                    imageUrl = DOSpaces.instance().put(imagePath)
                } catch (ace: AmazonClientException) {
                    Log.e("SyncTask", "Caught an AmazonClientException, which " +
                            "means the client encountered " +
                            "an internal error while trying to " +
                            "communicate with S3, " +
                            "such as not being able to access the network.")
                    Log.e("SyncTask", "Error Message: " + ace.message)
                    return "Failed."
                }

                Timber.d("SyncTask " +"imageUrl: $imageUrl")
                newTree.imageUrl = imageUrl // method name should be changed as use new infrastructure.
            }
            /*
            * Save to the API
            */
            var postResult: PostResult? = null
            try {
                var treeResponse: Response<PostResult>? = null
                treeResponse = Api.instance().api!!.createTree(newTree).execute()
                postResult = treeResponse!!.body()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (postResult != null) {
                val treeIdResponse = postResult.status
                val values = ContentValues()
                values.put("is_synced", "Y")
                values.put("main_db_id", treeIdResponse)
                val isMissingCursor = mDatabaseManager.queryCursor("SELECT is_missing FROM tree WHERE is_missing = 'Y' AND _id = $localTreeId", null)
                if (isMissingCursor.moveToNext()) {
                    mDatabaseManager.delete("tree", "_id = ?", arrayOf(localTreeId))
                    val photoQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where tree_id = $localTreeId"
                    val photoCursor = mDatabaseManager.queryCursor(photoQuery, null)
                    while (photoCursor.moveToNext()) {
                        try {
                            val file = File(photoCursor.getString(photoCursor.getColumnIndex("name")))
                            val deleted = file.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                } else {
                    mDatabaseManager.update("tree", values, "_id = ?", arrayOf(localTreeId))
                    val outDatedQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where is_outdated = 'Y' and tree_id = $localTreeId"
                    val outDatedPhotoCursor = mDatabaseManager.queryCursor(outDatedQuery, null)
                    while (outDatedPhotoCursor.moveToNext()) {
                        try {
                            val file = File(outDatedPhotoCursor.getString(outDatedPhotoCursor.getColumnIndex("name")))
                            val deleted = file.delete()
                            if (deleted)
                                Timber.tag("DataFragment").d("delete file: " + outDatedPhotoCursor.getString(outDatedPhotoCursor.getColumnIndex("name")))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }

            } else {
                return "Failed."
            }
            value--
            publishProgress(value)
        }
        return "Completed."
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        callback.onProgressUpdate(*values)
    }

    override fun onPostExecute(message: String) {
        super.onPostExecute(message)
        callback.onPostExecute(message)
    }

    override fun onCancelled(message: String) {
        super.onCancelled(message)
    }
}
