package com.qalliance.treetracker.TreeTracker.fragments;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.database.DatabaseManager;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.api.SyncTask;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;
import com.qalliance.treetracker.TreeTracker.api.models.UserTree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Created by lei on 11/9/17.
 */

public class DataFragment extends Fragment implements View.OnClickListener, SyncTask.SyncTaskListener {

    private DatabaseManager mDatabaseManager;
    private TextView totalTrees;
    private TextView updateTrees;
    private TextView locatedTrees;
    private TextView tosyncTrees;
    private ProgressDialog progressDialog;
    private AlbumStorageDirFactory mAlbumStorageDirFactory;
    private SharedPreferences mSharedPreferences;

    private AsyncTask<Void, Void, String> syncTask;

    public DataFragment() {
        mDatabaseManager = DatabaseManager.getInstance(MainActivity.dbHelper);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mSharedPreferences = getActivity().getSharedPreferences(
                "com.qalliance.treetracker", Context.MODE_PRIVATE);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data, container, false);
        totalTrees = (TextView) v.findViewById(R.id.fragment_data_total_trees_value);
        updateTrees = (TextView) v.findViewById(R.id.fragment_data_update_value);
        locatedTrees = (TextView) v.findViewById(R.id.fragment_data_located_value);
        tosyncTrees = (TextView) v.findViewById(R.id.fragment_data_to_sync_value);

        ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.data);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button syncBtn = (Button) v.findViewById(R.id.fragment_data_sync);
        syncBtn.setOnClickListener(this);

        Button pauseBtn = (Button) v.findViewById(R.id.fragment_data_pause);
        pauseBtn.setOnClickListener(this);

        Button resumeBtn = (Button) v.findViewById(R.id.fragment_data_resume);
        resumeBtn.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
        } else {
            mAlbumStorageDirFactory = new BaseAlbumDirFactory();
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();

        Bundle extras = getArguments();
        if (extras != null) {
            if (extras.getBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN)) {
                resolvePendingUpdates();
                progressDialog = new ProgressDialog(getActivity());
                progressDialog.setCancelable(false);
                progressDialog.setMessage(getActivity().getString(R.string.downloading_your_trees));
                progressDialog.show();
            }

            if (extras.getBoolean(ValueHelper.RUN_FROM_NOTIFICATION_SYNC)) {
//                syncTreesTask = new DataFragmentBK.SyncTreesTask().execute(new String[]{});
                Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();
            }
        }

        // User exit from app
//        if (MainActivity.syncDataFromExitScreen == true) {
//            MainActivity.syncDataFromExitScreen = false;
//            syncFromExitStarted = true;
//            syncTreesTask = new DataFragmentBK.SyncTreesTask().execute(new String[]{});
//            Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (syncTask != null) {
            syncTask.cancel(true);
            Toast.makeText(getActivity(), "Sync stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        String userId = mSharedPreferences.getString(ValueHelper.MAIN_DB_USER_ID, "-1");
        switch (v.getId()) {
            case R.id.fragment_data_sync:
                Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();
                syncTask = new SyncTask(getActivity(), this, Integer.parseInt(userId)).execute();
                break;
            case R.id.fragment_data_pause:
                if (syncTask != null) {
                    syncTask.cancel(true);
                }
                Toast.makeText(getActivity(), "Pause syncing", Toast.LENGTH_SHORT).show();
                break;
            case R.id.fragment_data_resume:
                Toast.makeText(getActivity(), "Resume syncing", Toast.LENGTH_SHORT).show();
                syncTask = new SyncTask(getActivity(), this, Integer.parseInt(userId)).execute();
                break;

        }
    }

    private void updateData() {
        mDatabaseManager.openDatabase();

        Cursor treeCursor = mDatabaseManager.queryCursor("SELECT COUNT(*) AS total FROM tree", null);
        treeCursor.moveToFirst();
        totalTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("total")));
        Log.i("total", treeCursor.getString(treeCursor.getColumnIndex("total")));

        treeCursor = mDatabaseManager.queryCursor("SELECT COUNT(*) AS updated FROM tree WHERE is_synced = 'Y' AND time_for_update < DATE('NOW')", null);
        treeCursor.moveToFirst();
        updateTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("updated")));
        Log.i("updated", treeCursor.getString(treeCursor.getColumnIndex("updated")));

        treeCursor = mDatabaseManager.queryCursor("SELECT COUNT(*) AS located FROM tree WHERE is_synced = 'Y' AND time_for_update >= DATE('NOW')", null);
        treeCursor.moveToFirst();
        locatedTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("located")));
        Log.i("located", treeCursor.getString(treeCursor.getColumnIndex("located")));


        treeCursor = mDatabaseManager.queryCursor("SELECT COUNT(*) AS tosync FROM tree WHERE is_synced = 'N'", null);
        treeCursor.moveToFirst();
        tosyncTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("tosync")));
        Log.i("to sync", treeCursor.getString(treeCursor.getColumnIndex("tosync")));

        mDatabaseManager.closeDatabase();
    }

    public void resolvePendingUpdates() {
        updateData();
        mDatabaseManager.openDatabase();

        Cursor treeCursor = mDatabaseManager.queryCursor("SELECT DISTINCT tree_id FROM pending_updates WHERE tree_id NOT NULL and tree_id <> 0", null);
        List<UserTree> trees = ((MainActivity)getActivity()).getUserTrees();

        if (treeCursor.moveToFirst()) {
            new UpdateLocalDb().execute(trees);
        }

        mDatabaseManager.closeDatabase();
    }

    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name),
                    "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        String imageFileName = ValueHelper.JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName,
                ValueHelper.JPEG_FILE_SUFFIX, albumF);

        return imageF;
    }

    private String saveImage(String sUrl) {
        Bitmap bitmap;
        File file = null;
        FileOutputStream out = null;
        try {
            InputStream inputStream = new URL(sUrl).openStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            file = createImageFile();
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            Timber.tag("saveImage").d("Exception 1, Something went wrong!");
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Timber.tag("saveImage").d("filePath: " + file.getAbsolutePath());
        return file.getAbsolutePath();
    }

    @Override
    public void onPostExecute(String message) {
        updateData();
        Toast.makeText(getActivity(), "Sync " + message, Toast.LENGTH_SHORT).show();
    }

    class UpdateLocalDb extends AsyncTask<List<UserTree>, Void, Void> {

        @Override
        protected Void doInBackground(List<UserTree>[] lists) {
            List<UserTree> trees = lists[0];
            for (UserTree tree : trees) {
                mDatabaseManager.openDatabase();
                String photoPath = saveImage(tree.getImageUrl());
                long photoId = -1;

                //add photo
                ContentValues photoValues = new ContentValues();
                photoValues.put("lat", tree.getLat());
                photoValues.put("long", tree.getLng());
//                photoValues.put("accuracy", tree.getGps());
                long locationId = mDatabaseManager.insert("location", null, photoValues);

                //add location
                ContentValues locationValues = new ContentValues();
                locationValues.put("location_id", locationId);
                locationValues.put("name", photoPath);
                photoId = mDatabaseManager.insert("photo", null, locationValues);

//                Date date = new Date();
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                Calendar calendar = Calendar.getInstance();
//                calendar.setTime(date);
//                calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(tree.getNextUpdate()));
//                date = calendar.getTime();

                Cursor treeExists = mDatabaseManager.queryCursor("SELECT * FROM tree WHERE main_db_id = " + tree.getId(), null);
                long treeId = -1;
                String priority = tree.getPriority().equals("1") ? "Y" : "N";

                if (treeExists.moveToNext()) {
                    treeId = Long.parseLong(treeExists.getString(treeExists.getColumnIndex("_id")));
                    String photoOutdated = "select photo._id from tree_photo left outer join photo on photo_id = photo._id where tree_id = " +
                            "" + treeExists.getString(treeExists.getColumnIndex("_id"));
                    Timber.tag("photoOutDated").d(photoOutdated);
                    Cursor photoCursor = mDatabaseManager.queryCursor(photoOutdated, null);
                    while (photoCursor.moveToNext()) {
                        ContentValues values = new ContentValues();
                        values.put("is_outdated", "Y");

                        mDatabaseManager.update("photo", values, "_id = ?", new String[]{photoCursor.getString(photoCursor.getColumnIndex("_id"))});
                    }

                    ContentValues treeValues = new ContentValues();
                    treeValues.put("time_created", tree.getCreated());
                    treeValues.put("time_updated", tree.getUpdated());
//                    treeValues.put("time_for_update", dateFormat.format(date));
                    treeValues.put("is_synced", "Y");
                    treeValues.put("is_priority", (priority == null ? "N" : priority));
                    treeValues.put("location_id", locationId);
                    mDatabaseManager.update("tree", treeValues, "_id = ?", new String[]{treeExists.getString(treeExists.getColumnIndex("_id"))});
                } else {
                    ContentValues treeValues = new ContentValues();
                    treeValues.put("time_created", tree.getCreated());
                    treeValues.put("time_updated", tree.getUpdated());
//                    treeValues.put("time_for_update", dateFormat.format(date));
                    treeValues.put("is_synced", "Y");
                    treeValues.put("is_priority", (priority == null ? "N" : priority ));
                    treeValues.put("main_db_id", tree.getId());
                    treeValues.put("location_id", locationId);
                    treeId = mDatabaseManager.insert("tree", null, treeValues);
                }

                if (photoId != -1) {
                    // tree_photo
                    ContentValues values = new ContentValues();
                    values.put("tree_id", treeId);
                    values.put("photo_id", photoId);
                    long treePhotoId = mDatabaseManager.insert("tree_photo", null, values);
                }

//                Cursor pendingUpdateCursor = mDatabaseManager.queryCursor("SELECT main_db_id FROM pending_updates WHERE main_db_id IS NOT NULL AND tree_id = " + tree.getId(), null);
//                if (pendingUpdateCursor.moveToFirst()) {
//                    clearPendingUpdate = new ClearPendingUpdateTask().execute(new String[]{pendingUpdateCursor.getString(pendingUpdateCursor.getColumnIndex("main_db_id"))});
//                }

                mDatabaseManager.delete("pending_updates", "tree_id = ?", new String[] { tree.getId() });
                mDatabaseManager.closeDatabase();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mSharedPreferences.edit().putBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false).commit();
            updateData();
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            FragmentManager fm = getActivity().getSupportFragmentManager();
            for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                Log.d("CheckFragmentBackStack", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
            }
            if (fm.getBackStackEntryCount() > 1) {
                fm.popBackStack();
            }

        }
    }

}
