package com.qalliance.treetracker.TreeTracker.api;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import com.qalliance.treetracker.TreeTracker.utilities.Utils;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.api.models.NewTree;
import com.qalliance.treetracker.TreeTracker.api.models.PostResult;
import com.qalliance.treetracker.TreeTracker.database.DatabaseManager;

import java.io.File;

import timber.log.Timber;

/**
 * Created by lei on 11/11/17.
 */

public class SyncTask extends AsyncTask<Void, Void, String> {

    public interface SyncTaskListener {
        void onPostExecute(String message);
    }

    private Context mContext;
    private SyncTaskListener callback;
    private DatabaseManager mDatabaseManager;
    private DataManager mDataManager;
    private int userId;

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
        while (treeCursor.moveToNext()) {
            String localTreeId = treeCursor.getString(treeCursor.getColumnIndex("tree_id"));
            Timber.tag("DataFragment").d("tree_id: " + localTreeId);

            NewTree newTree = new NewTree();
            newTree.setUserId(userId);
            newTree.setLat(treeCursor.getDouble(treeCursor.getColumnIndex("lat")));
            newTree.setLon(treeCursor.getDouble(treeCursor.getColumnIndex("long")));
            newTree.setGpsAccuracy(treeCursor.getFloat(treeCursor.getColumnIndex("accuracy")));
            String note = treeCursor.getString(treeCursor.getColumnIndex("content"));
            if (note == null) {
                note = "";
            }
            newTree.setNote(note);
            String timeCreated = treeCursor.getString(treeCursor.getColumnIndex("tree_time_created"));
            newTree.setTimestamp(Utils.convertDateToTimestamp(timeCreated));

            String image = Utils.base64Image(treeCursor.getString(treeCursor.getColumnIndex("name")));
            newTree.setBase64Image(image);
//            Timber.tag("DataFragment").d("user_id: " + newTree.getUserId());
//            Timber.tag("DataFragment").d("lat: " + newTree.getLat());
//            Timber.tag("DataFragment").d("lon: " + newTree.getLon());
//            Timber.tag("DataFragment").d("note: " + newTree.getNote());
//            Timber.tag("DataFragment").d("gps: " + newTree.getGpsAccuracy());
//            Timber.tag("DataFragment").d("timestamp: " + newTree.getTimestamp());
//            Timber.tag("DataFragment").d("image: " + newTree.getBase64Image());

            PostResult response = (PostResult) mDataManager.createNewTree(newTree);
            if (response != null) {
                int treeIdResponse = response.getStatus();
                Timber.tag("DataFragment").d("status code: " + treeIdResponse);

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
        }
        return "Completed.";
    }

    @Override
    protected void onPostExecute(String message) {
        super.onPostExecute(message);
        mDatabaseManager.closeDatabase();
        callback.onPostExecute(message);
    }

    @Override
    protected void onCancelled(String message) {
        super.onCancelled(message);
        mDatabaseManager.closeDatabase();
    }
}
