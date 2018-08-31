package org.greenstand.android.TreeTracker.managers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.util.Log

import org.greenstand.android.TreeTracker.api.Api
import org.greenstand.android.TreeTracker.api.DOSpaces
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest
import org.greenstand.android.TreeTracker.api.models.responses.PostResult
import org.greenstand.android.TreeTracker.database.DatabaseManager
import com.amazonaws.AmazonClientException
import kotlinx.coroutines.experimental.async

import java.io.IOException

import retrofit2.Response
import timber.log.Timber
import java.io.File


/**
 * Created by lei on 11/11/17.
 */

class SyncTask(private val mContext: Context, private val callback: SyncTaskListener, private val userId: Int) {

    private val databaseManager: DatabaseManager
    private val dataManager: DataManager<*>
    private var unsyncedTreesCount = 0
    val TAG = "SyncTask"

    interface SyncTaskListener {
        fun onPostExecute(message: String)
        fun onProgressUpdate(vararg values: Int)
    }

    init {
        // Setup DB manager
        this.databaseManager = DatabaseManager.getInstance(MainActivity.dbHelper!!)
        this.dataManager = object : DataManager<PostResult>() {

            override fun onDataLoaded(data: PostResult) {

            }

            override fun onRequestFailed(message: String) {

            }
        }
    }

    fun syncTreesForUser(): Int  {

        var postResult: PostResult? = null
        var numberOfTreesSynced = 0

        databaseManager.openDatabase()
        val syncQuery = "SELECT " +
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

        // Build Cursor for list of un-synced trees returned
        val treeCursor = databaseManager.queryCursor(syncQuery, null)
        Timber.tag("DataFragment").d("treeCursor: " + DatabaseUtils.dumpCursorToString(treeCursor))
        unsyncedTreesCount = treeCursor.count
        Timber.tag("DataFragment").d("treeCursor: " + treeCursor.count)

        while (treeCursor.moveToNext()) {
            val localTreeID = treeCursor.getLong(treeCursor.getColumnIndex("tree_id")).toString()
            Timber.tag("DataFragment").d("tree_id: $localTreeID")

            val newTree = buildNewTreeRequest(treeCursor)

            /** Implementation for saving above un-synced tree image into DigitalOcean Spaces.*/
            val newTreeImagePath = treeCursor.getString(treeCursor.getColumnIndex("name"))
            newTree.imageUrl = saveTreeToDOSpaces(newTreeImagePath) // method name should be changed as use new infrastructure.

            /** Save to the API */
            async {
                try {
                    var treeResponse: Response<PostResult> = Api.instance().api!!.createTree(newTree).execute()
                    postResult = treeResponse.body()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            /** Tree Synced Successfully ... Delete locally stored backup image **/
            if(postResult != null) {
                numberOfTreesSynced += 1
                deleteLocalCopy(localTreeID, postResult!!.status)
            }
        }

        return numberOfTreesSynced
    }

    private fun buildNewTreeRequest(treeCursor: Cursor): NewTreeRequest {
        val newTree = NewTreeRequest()
        newTree.userId = userId
        newTree.sequenceId = treeCursor.getLong(treeCursor.getColumnIndex("tree_id"))
        newTree.lat = treeCursor.getDouble(treeCursor.getColumnIndex("lat"))
        newTree.lon = treeCursor.getDouble(treeCursor.getColumnIndex("long"))
        newTree.setGpsAccuracy(treeCursor.getFloat(treeCursor.getColumnIndex("accuracy")).toInt())
        var note: String? = treeCursor.getString(treeCursor.getColumnIndex("content"))
        if(note == null) {
            note = ""
        }
        newTree.note = note
        val timeCreated = treeCursor.getString(treeCursor.getColumnIndex("tree_time_created"))
        newTree.timestamp = Utils.convertDateToTimestamp(timeCreated)

        return newTree
    }

    private fun saveTreeToDOSpaces(imagePath: String): String {
        var imageURL = ""
        if (imagePath != "") { // don't crash if image path is empty
            try {
                // validated image path, get url ...
                imageURL = DOSpaces.instance().put(imagePath)
                Timber.d("SyncTask", "imageUrl: $imageURL")
            } catch (ace: AmazonClientException) {
                Log.e("SyncTask", "Caught an AmazonClientException, which " +
                        "means the client encountered " +
                        "an internal error while trying to " +
                        "communicate with S3, " +
                        "such as not being able to access the network.")
                Log.e("SyncTask", "Error Message: " + ace.message)
                //return "Failed."
            }
        }
        return imageURL
    }

    private fun deleteLocalCopy(localTreeID: String, treeIdResponse: Int) {
        val values = ContentValues()
        values.put("is_synced", "Y")
        values.put("main_db_id", treeIdResponse)
        val isMissingCursor = databaseManager.queryCursor("SELECT is_missing FROM tree WHERE is_missing = 'Y' AND _id = $localTreeID", null)
        if (isMissingCursor.moveToNext()) {
            databaseManager.delete("tree", "_id = ?", arrayOf<String>(localTreeID))
            val photoQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where tree_id = $localTreeID"
            val photoCursor = databaseManager.queryCursor(photoQuery, null)
            while (photoCursor.moveToNext()) {
                try {
                    val file = File(photoCursor.getString(photoCursor.getColumnIndex("name")))
                    val deleted = file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            databaseManager.update("tree", values, "_id = ?", arrayOf<String>(localTreeID))
            val outDatedQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where is_outdated = 'Y' and tree_id = $localTreeID"
            val outDatedPhotoCursor = databaseManager.queryCursor(outDatedQuery, null)
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
    }


}