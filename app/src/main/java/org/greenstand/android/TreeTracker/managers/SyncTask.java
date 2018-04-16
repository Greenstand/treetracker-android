package org.greenstand.android.TreeTracker.managers;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.greenstand.android.TreeTracker.api.Api;
import org.greenstand.android.TreeTracker.api.DOSpaces;
import org.greenstand.android.TreeTracker.managers.DataManager;
import org.greenstand.android.TreeTracker.utilities.Utils;
import org.greenstand.android.TreeTracker.activities.MainActivity;
import org.greenstand.android.TreeTracker.api.models.requests.NewTreeRequest;
import org.greenstand.android.TreeTracker.api.models.responses.PostResult;
import org.greenstand.android.TreeTracker.database.DatabaseManager;
import com.amazonaws.AmazonClientException;

import java.io.File;
import java.io.IOException;

import retrofit2.Response;
import timber.log.Timber;

/**
 * Created by lei on 11/11/17.
 */

public class SyncTask extends AsyncTask<Void, Integer, String> {

    public interface SyncTaskListener {
        void onPostExecute(String message);

        void onProgressUpdate(Integer... values);
    }

    private Context mContext;
    private SyncTaskListener callback;
    private DatabaseManager mDatabaseManager;
    private DataManager mDataManager;
    private int userId;
    private int value;

    public SyncTask(Context context, SyncTaskListener listener, int userId) {
        this.mContext = context;
        this.callback = listener;
        this.mDatabaseManager = DatabaseManager.getInstance(MainActivity.dbHelper);
        this.userId = userId;
        this.mDataManager = new DataManager<PostResult>() {
            @Override
            public void onDataLoaded(PostResult data) {

            }

            @Override
            public void onRequestFailed(String message) {

            }
        };
    }

    @Override
    protected String doInBackground(Void... voids) {
        mDatabaseManager.openDatabase();
        String query = "SELECT " +
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
                "is_synced = 'N'";

        Cursor treeCursor = mDatabaseManager.queryCursor(query, null);
        Timber.tag("DataFragment").d("treeCursor: " + DatabaseUtils.dumpCursorToString(treeCursor));
        value = treeCursor.getCount();
        Timber.tag("DataFragment").d("treeCursor: " + treeCursor.getCount());

        while (treeCursor.moveToNext()) {
            String localTreeId = treeCursor.getString(treeCursor.getColumnIndex("tree_id"));
            Timber.tag("DataFragment").d("tree_id: " + localTreeId);

            NewTreeRequest newTree = new NewTreeRequest();
            newTree.setUserId(userId);
            newTree.setLat(treeCursor.getDouble(treeCursor.getColumnIndex("lat")));
            newTree.setLon(treeCursor.getDouble(treeCursor.getColumnIndex("long")));
            newTree.setGpsAccuracy((int) treeCursor.getFloat(treeCursor.getColumnIndex("accuracy")));
            String note = treeCursor.getString(treeCursor.getColumnIndex("content"));
            if (note == null) {
                note = "";
            }
            newTree.setNote(note);
            String timeCreated = treeCursor.getString(treeCursor.getColumnIndex("tree_time_created"));
            newTree.setTimestamp(Utils.convertDateToTimestamp(timeCreated));

            /**
             * Implementation for saving image into DigitalOcean Spaces.
             */
            String imagePath = treeCursor.getString(treeCursor.getColumnIndex("name"));
            String imageUrl;
            try {
                imageUrl = DOSpaces.instance().put(imagePath);
            } catch (AmazonClientException ace) {
                Log.e("SyncTask", "Caught an AmazonClientException, which " +
                        "means the client encountered " +
                        "an internal error while trying to " +
                        "communicate with S3, " +
                        "such as not being able to access the network.");
                Log.e("SyncTask", "Error Message: " + ace.getMessage());
                return "Failed.";
            }
            Log.d("SyncTask", "imageUrl: " + imageUrl);
            newTree.setImageUrl(imageUrl); // method name should be changed as use new infrastructure.

            /*
            * Save to the API
            */
            PostResult postResult = null;
            try {
                Response<PostResult> treeResponse = null;
                treeResponse = Api.instance().getApi().createTree(newTree).execute();
                postResult = treeResponse.body();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (postResult != null) {
                int treeIdResponse = postResult.getStatus();
                ContentValues values = new ContentValues();
                values.put("is_synced", "Y");
                values.put("main_db_id", treeIdResponse);
                Cursor isMissingCursor = mDatabaseManager.queryCursor("SELECT is_missing FROM tree WHERE is_missing = 'Y' AND _id = " + localTreeId, null);
                if (isMissingCursor.moveToNext()) {
                    mDatabaseManager.delete("tree", "_id = ?", new String[]{localTreeId});
                    String photoQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where tree_id = " + localTreeId;
                    Cursor photoCursor = mDatabaseManager.queryCursor(photoQuery, null);
                    while (photoCursor.moveToNext()) {
                        try {
                            File file = new File(photoCursor.getString(photoCursor.getColumnIndex("name")));
                            boolean deleted = file.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    mDatabaseManager.update("tree", values, "_id = ?", new String[]{localTreeId});
                    String outDatedQuery = "SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where is_outdated = 'Y' and tree_id = " + localTreeId;
                    Cursor outDatedPhotoCursor = mDatabaseManager.queryCursor(outDatedQuery, null);
                    while (outDatedPhotoCursor.moveToNext()) {
                        try {
                            File file = new File(outDatedPhotoCursor.getString(outDatedPhotoCursor.getColumnIndex("name")));
                            boolean deleted = file.delete();
                            if (deleted)
                                Timber.tag("DataFragment").d("delete file: " + outDatedPhotoCursor.getString(outDatedPhotoCursor.getColumnIndex("name")));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    this.mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                return "Failed.";
            }
            value--;
            publishProgress(value);
        }
        return "Completed.";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        callback.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String message) {
        super.onPostExecute(message);
        callback.onPostExecute(message);
        mDatabaseManager.closeDatabase();
    }

    @Override
    protected void onCancelled(String message) {
        super.onCancelled(message);
        mDatabaseManager.closeDatabase();
    }
}
