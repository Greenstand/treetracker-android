package com.qalliance.treetracker.TreeTracker.fragments;


import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.network.NetworkUtilities;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;
import com.qalliance.treetracker.TreeTracker.api.models.UserTree;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Deprecated.
 */
public class DataFragmentBK extends Fragment implements OnClickListener {


	private AsyncTask<String, Void, String> syncTreesTask;
	private AsyncTask<String, Void, String> getPendingUpdates;
	private AsyncTask<String, Void, String> getTreeUpdate;
	private AsyncTask<String, Void, String> clearPendingUpdate;
	private AsyncTask<String, Void, String> getSettingUpdate;
	
	
	
	private SharedPreferences mSharedPreferences;
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	private ProgressDialog progressDialog;
	private boolean syncFromExitStarted = false;

	public DataFragmentBK() {
		//some overrides and settings go here
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

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
		    	syncTreesTask = new SyncTreesTask().execute(new String[]{});
		    	
		    	Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();

	    	}

	    }
	    
	    
	    if (MainActivity.syncDataFromExitScreen == true) {
	    	MainActivity.syncDataFromExitScreen = false;
	    	
	    	syncFromExitStarted = true;
	    	
	    	syncTreesTask = new SyncTreesTask().execute(new String[]{});
	    	
	    	Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();
	    }
	    
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		boolean isSyncStopped = false;
		
//		private AsyncTask<String, Void, String> syncTreesTask;
//		private AsyncTask<String, Void, String> getPendingUpdates;
//		private AsyncTask<String, Void, String> getTreeUpdate;
//		private AsyncTask<String, Void, String> clearPendingUpdate;
//		private AsyncTask<String, Void, String> getSettingUpdate;

		if (clearPendingUpdate != null) {
			clearPendingUpdate.cancel(true);
			isSyncStopped = true;
		}
		
		if (getSettingUpdate != null) {
			getSettingUpdate.cancel(true);
			isSyncStopped = true;
		}
		
		if (getPendingUpdates != null) {
			getPendingUpdates.cancel(true);
			isSyncStopped = true;
		}
		
		
		if (getTreeUpdate != null) {
			getTreeUpdate.cancel(true);
			isSyncStopped = true;
		}

		
		if (syncTreesTask != null) {
			syncTreesTask.cancel(true);
			isSyncStopped = true;
			
			Toast.makeText(getActivity(), "Sync stopped", Toast.LENGTH_SHORT).show();
		}
			
	}

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		
	    View v = inflater.inflate(R.layout.fragment_data, container, false);
        if (!((AppCompatActivity) getActivity()).getSupportActionBar().isShowing()) {
            Log.d("MainActivity", "toolbar hide");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }

	    ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.data);
//        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.data);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

	    mSharedPreferences = getActivity().getSharedPreferences(
	      	      "com.qalliance.treetracker", Context.MODE_PRIVATE);

	    Button syncBtn = (Button) v.findViewById(R.id.fragment_data_sync);
	    syncBtn.setOnClickListener(DataFragmentBK.this);

	    Button pauseBtn = (Button) v.findViewById(R.id.fragment_data_pause);
	    pauseBtn.setOnClickListener(DataFragmentBK.this);
	    
	    Button resumeBtn = (Button) v.findViewById(R.id.fragment_data_resume);
	    resumeBtn.setOnClickListener(DataFragmentBK.this);
	    
	    
	    Bundle extras = getArguments();
	    
	    
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}

	    
	    
	    
	    return v;
	}
	
	
	private void updateData() {
		SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
		
		Cursor treeCursor = db.rawQuery("SELECT COUNT(*) AS total FROM tree", null);
		treeCursor.moveToFirst();
		
		TextView totalTrees = (TextView) getActivity().findViewById(R.id.fragment_data_total_trees_value);
		totalTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("total")));
		Log.e("total", treeCursor.getString(treeCursor.getColumnIndex("total")));
		
		
		treeCursor = db.rawQuery("SELECT COUNT(*) AS updated FROM tree WHERE is_synced = 'Y' AND time_for_update < DATE('NOW')", null);
		treeCursor.moveToFirst();
		
		TextView updateTrees = (TextView) getActivity().findViewById(R.id.fragment_data_update_value);
		updateTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("updated")));
		Log.e("updated", treeCursor.getString(treeCursor.getColumnIndex("updated")));
		
		treeCursor = db.rawQuery("SELECT COUNT(*) AS located FROM tree WHERE is_synced = 'Y' AND time_for_update >= DATE('NOW')", null);
		treeCursor.moveToFirst();
		
		TextView locatedTrees = (TextView) getActivity().findViewById(R.id.fragment_data_located_value);
		locatedTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("located")));
		Log.e("located", treeCursor.getString(treeCursor.getColumnIndex("located")));

		
		treeCursor = db.rawQuery("SELECT COUNT(*) AS tosync FROM tree WHERE is_synced = 'N'", null);
		treeCursor.moveToFirst();
		
		TextView tosyncTrees = (TextView) getActivity().findViewById(R.id.fragment_data_to_sync_value);
		tosyncTrees.setText(treeCursor.getString(treeCursor.getColumnIndex("tosync")));
		Log.e("to sync", treeCursor.getString(treeCursor.getColumnIndex("tosync")));
		
		
		db.close();
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_data_sync:
				Toast.makeText(getActivity(), "Start syncing", Toast.LENGTH_SHORT).show();
				
				syncTreesTask = new SyncTreesTask().execute(new String[]{});
				break;
			case R.id.fragment_data_pause:
				if (clearPendingUpdate != null) {
					clearPendingUpdate.cancel(true);
				}
				
				if (getSettingUpdate != null) {
					getSettingUpdate.cancel(true);
				}
				
				if (getPendingUpdates != null) {
					getPendingUpdates.cancel(true);
				}
				
				
				if (getTreeUpdate != null) {
					getTreeUpdate.cancel(true);
				}

				
				if (syncTreesTask != null) {
					syncTreesTask.cancel(true);
					
				}
				
				Toast.makeText(getActivity(), "Pause syncing", Toast.LENGTH_SHORT).show();
				
				break;				
			case R.id.fragment_data_resume:
				Toast.makeText(getActivity(), "Resume syncing", Toast.LENGTH_SHORT).show();
				
				syncTreesTask = new SyncTreesTask().execute(new String[]{});
				break;				
		}

		
	}
	
	class SyncTreesTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
	        
			SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();

			
			String query = "SELECT " +
					"tree.*, " +
					"tree.main_db_id as tree_main_db_id, " +
					"tree._id as tree_tree_id, " +
					"tree.time_created as tree_time_created, " +
					"tree.user_id as tree_user_id, " +
					"settings.*, " +
					"override_settings.min_accuracy as override_min_accuracy, " +
					"override_settings.time_to_next_update AS override_time_to_next_update, " +
					"location.*, " +
					"users.*, " +
					"cause_of_death.* " +

				"FROM tree " +
					"LEFT OUTER JOIN settings AS override_settings ON settings_override_id = override_settings._id " +
					"LEFT OUTER JOIN settings ON settings_id = settings._id " +
					"LEFT OUTER JOIN location ON location._id = tree.location_id " +
					"LEFT OUTER JOIN users ON users._id = tree.user_id " +
					"LEFT OUTER join note as cause_of_death ON cause_of_death_id = cause_of_death._id " +
				"WHERE " +
					"is_synced = 'N'";

			
			Cursor treeCursor = db.rawQuery(query, null);

			
			
			Log.e("query", query);
			
			Cursor noteCursor = null;
			Cursor photoCursor = null;
//			Cursor noteCursor = db.rawQuery("SELECT * FROM tree_note LEFT OUTER JOIN tree ON tree_id = tree._id LEFT OUTER JOIN note ON note._id = note_id  WHERE tree_id = ", null);
//			
//			query
//			SELECT tree.*, tree.main_db_id as tree_main_db_id , tree._id as tree_tree_id , settings.*, override_settings.min_accuracy as override_min_accuracy, override_settings.time_to_next_update AS override_time_to_next_update, location.*, users.*, cause_of_death.*, photo_location.accuracy as photo_accuracy, photo_location.lat as photo_lat, photo_location.long as photo_long FROM tree LEFT OUTER JOIN settings AS override_settings ON settings_override_id = override_settings._id LEFT OUTER JOIN settings ON settings_id = settings._id LEFT OUTER JOIN location ON location._id = tree.location_id LEFT OUTER JOIN users ON users._id = tree.user_id LEFT OUTER join note as cause_of_death ON cause_of_death_id = cause_of_death._id LEFT OUTER JOIN tree_photo ON tree._id = tree_photo.tree_id LEFT OUTER JOIN photo ON photo._id = tree_photo.photo_id LEFT OUTER JOIN location as photo_location ON photo.location_id = photo_location._id WHERE is_synced = 'N' AND is_outdated = 'N';				
			
			JSONObject jsonOut = new JSONObject();
			JSONObject photoOut = new JSONObject();
			JSONObject locationOut = new JSONObject();
			JSONObject settingsOut = new JSONObject();
			JSONObject notesOut = new JSONObject();
			
			JSONArray notesArray = new JSONArray();
			
			String rsp = null;
			if (treeCursor.getCount() > 0) {
				treeCursor.moveToFirst();

				String treeId = Long.toString(treeCursor.getLong(treeCursor.getColumnIndex("tree_tree_id")));
				
				try {
					
					jsonOut.put("token", mSharedPreferences.getString(ValueHelper.TOKEN, ""));
					jsonOut.put("id", treeCursor.getString(treeCursor.getColumnIndex("tree_main_db_id")));
					jsonOut.put("cause_of_death_id", treeCursor.getString(treeCursor.getColumnIndex("cause_of_death_id")));
					jsonOut.put("local_id", treeCursor.getString(treeCursor.getColumnIndex("tree_tree_id")));
					jsonOut.put("user_id", mSharedPreferences.getString(ValueHelper.MAIN_DB_USER_ID, "-1"));
					
					jsonOut.put("time_created", treeCursor.getString(treeCursor.getColumnIndex("tree_time_created")));
					jsonOut.put("time_updated", treeCursor.getString(treeCursor.getColumnIndex("time_updated")));
					
					jsonOut.put("is_missing", treeCursor.getString(treeCursor.getColumnIndex("is_missing")));
					

					
					
					locationOut = new JSONObject();
					
					
					
					//primary location
					locationOut.put("lat", treeCursor.getString(treeCursor.getColumnIndex("lat")));
					locationOut.put("long", treeCursor.getString(treeCursor.getColumnIndex("long")));
					locationOut.put("gps_accuracy", treeCursor.getString(treeCursor.getColumnIndex("accuracy")));
					
					jsonOut.put("primary_location", locationOut);
					locationOut = new JSONObject();
					
					
					
					settingsOut.put("time_to_update", treeCursor.getString(treeCursor.getColumnIndex("time_to_next_update")));
					settingsOut.put("min_gps_accuracy", treeCursor.getString(treeCursor.getColumnIndex("min_accuracy")));
					
					jsonOut.put("settings", settingsOut);
					settingsOut = new JSONObject();
					
					settingsOut.put("time_to_update", treeCursor.getString(treeCursor.getColumnIndex("override_time_to_next_update")));
					settingsOut.put("min_gps_accuracy", treeCursor.getString(treeCursor.getColumnIndex("override_min_accuracy")));
					
					jsonOut.put("settings_override", settingsOut);
					settingsOut = new JSONObject();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				

					
					String noteQuery = "SELECT " +
							"note.* " +
						"FROM tree " +
							"LEFT OUTER JOIN tree_note ON tree_note.tree_id = tree._id " +
							"LEFT OUTER JOIN note ON tree_note.note_id = note._id " +
						"WHERE " +
							"tree._id = " + treeId;
					Log.e("note quety", noteQuery );
					

					noteCursor = db.rawQuery(noteQuery, null);
					
					while(noteCursor.moveToNext()) {
						Log.e("content", noteCursor.getString(noteCursor.getColumnIndex("content")) + " ");
						
						try {
							notesOut.put("content", noteCursor.getString(noteCursor.getColumnIndex("content")));
							notesOut.put("time_created", noteCursor.getString(noteCursor.getColumnIndex("time_created")));
							notesArray.put(notesOut);
							notesOut = new JSONObject();
							
							
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
					}
					
					try {
						jsonOut.put("notes", notesArray);
					} catch (JSONException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					
					
					
					String photoQuery = "select * from photo left outer join tree_photo on photo._id = photo_id left outer join location on location._id = location_id where is_outdated = 'N'" +
							" and tree_id = " + treeId;
					Log.e("photo query", photoQuery );
					

					photoCursor = db.rawQuery(photoQuery, null);
					
					if(photoCursor.moveToNext()) {
						
						try {
							photoOut.put("base64_image", base64Image(photoCursor.getString(photoCursor.getColumnIndex("name"))));
//								photoOut.put("base64_image", "some base 64 image");
							photoOut.put("time_taken", photoCursor.getString(photoCursor.getColumnIndex("time_taken")));
							photoOut.put("is_outdated", photoCursor.getString(photoCursor.getColumnIndex("is_outdated")));
							
							//photo location
							locationOut.put("lat", photoCursor.getString(photoCursor.getColumnIndex("lat")));
							locationOut.put("long", photoCursor.getString(photoCursor.getColumnIndex("long")));
							locationOut.put("gps_accuracy", photoCursor.getString(photoCursor.getColumnIndex("accuracy")));
							
							photoOut.put("location", locationOut);
							
							
							jsonOut.put("photo", photoOut);
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					
				
				db.close();


		        
		        StringEntity se = null;
				try {
					se = new StringEntity(jsonOut.toString());
					Log.i("json", jsonOut.toString());
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}  
		  
				se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		        
		        HttpPut post = null;
				try {
					//FIXME NO VALUE FOR ID JSON
					post = new HttpPut(NetworkUtilities.TREE_SYNC_URI + jsonOut.getString("id"));
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        post.setEntity(se);
		        post.setHeader("Accept-Charset","utf-8");


		        DefaultHttpClient mHttpClient = NetworkUtilities.createHttpClient();
		        
		        try {
		            resp = mHttpClient.execute(post);
		        } catch (final ConnectException e) {
		            Log.d("", "Connect exception", e);
		        } catch (final IOException e) {
		                Log.v("", "IOException when getting authtoken", e);
		        } finally {
		                Log.v("", "getAuthtoken completing");
		        }
		        
				try {
					
					if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
							rsp = "Token invalid";
							return rsp;
						
					} else {
						rsp = EntityUtils.toString(resp.getEntity());
					}
					
					
					
				} catch (IllegalStateException e) {
					Log.e("onpostexec", "IllegalStateException");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e("onpostexec", "IOException");
					e.printStackTrace();
				} catch (Exception e) {
					Log.e("Exception", "Exception");
					e.printStackTrace();
				}

			}
			
			
	        
	        return rsp;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        if (response != null) {
		        	JSONObject jsonResponse = null;
					String local_id = null;
					String maindb_id = null;
					String priority = null;
					try {
						jsonResponse = new JSONObject(response);
						local_id = jsonResponse.getString("local_id");
						maindb_id = jsonResponse.getString("id");
						
						
						try {
							priority = jsonResponse.getString("priority").equals("1") ? "Y" : "N" ;
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (JSONException e) {
						if (syncTreesTask != null) {
							syncTreesTask.cancel(true);
							Toast.makeText(getActivity(), "Sync error: " + response, Toast.LENGTH_SHORT).show();
							
							Log.e("RESPONSE", response);
							
							syncTreesTask = null;
						}
						e.printStackTrace();
						
						return;
					}
					
					
		  			SQLiteDatabase db = MainActivity.dbHelper.getWritableDatabase();
		  			
		  			 
		  		    ContentValues values = new ContentValues();
		  		    values.put("is_synced", "Y");
		  		    values.put("main_db_id", maindb_id);
		  		    values.put("is_priority", (priority == null ? "N" : priority ));
		  		  
		  		    Cursor isMissingCursor = db.rawQuery("SELECT is_missing FROM tree WHERE is_missing = 'Y' AND _id = " + local_id, null);
		  		    
		  		    if (isMissingCursor.moveToNext()) {
		  		    	db.delete("tree", "_id = ?", new String[] { local_id });
		  		    	
		  		    	Cursor photoCursor = db.rawQuery("SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where tree_id = " + local_id, null);
		  		    	
		  		    	while (photoCursor.moveToNext()) {
		  		    		try {
								File file = new File(photoCursor.getString(photoCursor.getColumnIndex("name")));
								boolean deleted = file.delete();
								
								
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		  		    		
		  		    		
		  		    	}
		  		    	
		  		    	//TODO delete all the images 
		  		    } else {
			  		    db.update("tree", values, "_id = ?", new String[] { local_id });
			  		    //TODO delete all the outdated images
			  		    Cursor photoCursor = db.rawQuery("SELECT name FROM photo left outer join tree_photo on photo_id = photo._id where is_outdated = 'Y' and tree_id = " + local_id, null);
		  		    	
		  		    	while (photoCursor.moveToNext()) {
		  		    		try {
								File file = new File(photoCursor.getString(photoCursor.getColumnIndex("name")));
								boolean deleted = file.delete();
								
								if (deleted)
									Log.e("OBRISANO", photoCursor.getString(photoCursor.getColumnIndex("name")));
								
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		  		    		
		  		    		
		  		    	}
		  		    }
		  		    
		  		    try {
						getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		  		    
		  		    updateData();
		  		 
		  		    
		  			db.close();
		  			
			        syncTreesTask = new SyncTreesTask().execute(new String[]{});
		        } else {
		  			SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
		  			
					Cursor treeCursor = db.rawQuery("SELECT COUNT(*) AS tosync FROM tree WHERE is_synced = 'N'", null);
					treeCursor.moveToFirst();
					
					if (0 == Integer.parseInt(treeCursor.getString(treeCursor.getColumnIndex("tosync")))) {
						
						
						syncTreesTask = null;
						
						getPendingUpdates = new PendingUpdatesTask().execute(new String[]{});
					}
					
		  			db.close();
		        }
				
		 }
		
		
	}
	
	
	class PendingUpdatesTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
			String rsp = null;
	        
	        HttpGet post = null;
			post = new HttpGet(NetworkUtilities.PENDING_UPDATES_URI + mSharedPreferences.getString(ValueHelper.MAIN_DB_USER_ID, "-1") 
					+ "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));

	        post.setHeader("Accept-Charset","utf-8");


	        DefaultHttpClient mHttpClient = NetworkUtilities.createHttpClient();
	        
	        try {
	            resp = mHttpClient.execute(post);
	        } catch (final ConnectException e) {
	            Log.d("", "Connect exception", e);
	        } catch (final IOException e) {
	                Log.v("", "IOException when getting authtoken", e);
	        } finally {
	                Log.v("", "getAuthtoken completing");
	        }
	        
			try {
				
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
						rsp = "Token invalid";
						return rsp;
					
				} else {
					rsp = EntityUtils.toString(resp.getEntity());
				}
				
				
				
			} catch (IllegalStateException e) {
				Log.e("onpostexec", "IllegalStateException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("onpostexec", "IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Exception", "Exception");
				e.printStackTrace();
			}
		
			
	        
	        return rsp;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        if (response != null) {
		        	JSONObject jsonResponse = null;
		        	JSONArray jsonReponseArray = null;
					String local_id = null;
					String maindb_id = null;
					try {
						jsonResponse = new JSONObject(response);
						
						
						Log.i("pending updates", jsonResponse.toString());
						
					} catch (JSONException e) {
						
						try {
							jsonReponseArray = new JSONArray(response);
							
							for (int i = 0; i < jsonReponseArray.length(); i++) {
								if (jsonReponseArray.get(i) instanceof JSONArray) {
									for (int j = 0; j < ((JSONArray)jsonReponseArray.get(i)).length(); j++) {
										if (((JSONArray)jsonReponseArray.get(i)).get(j) instanceof JSONObject) {
											JSONObject obj = (JSONObject)((JSONArray)jsonReponseArray.get(i)).get(j);
											
											
								  			SQLiteDatabase db = MainActivity.dbHelper.getWritableDatabase();
								  			
								  			 
								  		    ContentValues values = new ContentValues();
								  		    values.put("main_db_id", obj.getString("main_db_id"));
								  		    values.put("user_id", Long.toString(mSharedPreferences.getLong(ValueHelper.MAIN_USER_ID, -1)));
								  			
											
											if (obj.getString("update_type").contains("global_settings")) {
												Log.i("settings_id", obj.getString("id"));
												values.put("settings_id", obj.getString("id"));
												
											} else if (obj.getString("update_type").contains("tree")) {
												Log.i("tree_id", obj.getString("id"));
												values.put("tree_id", obj.getString("id"));
											}
											
											
											
											db.insert("pending_updates", null, values);
											db.close();
										}
										
									}
								}
							}


							Log.i("pending updates", jsonReponseArray.toString());
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
		  			
		        } else {
	        		
		        }
		        
		        Log.i("jesam ", "tu?");
		        
		        resolvePendingUpdates();
				
		 }
		
		
	}
	
	class ClearPendingUpdateTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
	        String mainDbId = params[0];
	        
	        Log.d("params", "params");
	        
	        if (mainDbId == null)
	        	return "";
	        
	        Log.d("params", mainDbId);
	        
			String rsp = "OK";
	        
			
			HttpDelete post = null;
			post = new HttpDelete(NetworkUtilities.PENDING_UPDATES_CLEAR_URI + mainDbId + "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));
			
			Log.d("url", NetworkUtilities.PENDING_UPDATES_CLEAR_URI + mainDbId + "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));

			
	        post.setHeader("Accept-Charset","utf-8");


	        DefaultHttpClient mHttpClient = NetworkUtilities.createHttpClient();
	        
	        try {
	            resp = mHttpClient.execute(post);
	        } catch (final ConnectException e) {
	            Log.d("", "Connect exception", e);
	        } catch (final IOException e) {
	                Log.v("", "IOException when getting authtoken", e);
	        } finally {
	                Log.v("", "getAuthtoken completing");
	        }
	        
			try {
				
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
						rsp = "Token invalid";
						return rsp;
					
				} else {
					rsp = EntityUtils.toString(resp.getEntity());
				}
				
				
				
			} catch (IllegalStateException e) {
				Log.e("onpostexec", "IllegalStateException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("onpostexec", "IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Exception", "Exception");
				e.printStackTrace();
			}
		
			
	        
	        return rsp;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        Log.i("clear pending update", response + " ");
		        
		        if (response != null) {
		        	
		        } else {
	        		
		        }
				
		 }
		
		
	}
	
	
	class GetTreeTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
	        String treeId = params[0];
	        
	        Log.d("params", "params");
	        
	        if (treeId == null)
	        	return "";
	        
	        Log.d("params", treeId);
	        
			String rsp = null;
	        
	        HttpGet post = null;
			post = new HttpGet(NetworkUtilities.TREE_SYNC_URI + treeId + "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));

	        post.setHeader("Accept-Charset","utf-8");
	        
	        Log.e("GET", NetworkUtilities.TREE_SYNC_URI + treeId + "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));


	        DefaultHttpClient mHttpClient = NetworkUtilities.createHttpClient();
	        
	        try {
	            resp = mHttpClient.execute(post);
	        } catch (final ConnectException e) {
	            Log.d("", "Connect exception", e);
	        } catch (final IOException e) {
	                Log.v("", "IOException when getting authtoken", e);
	        } finally {
	                Log.v("", "getAuthtoken completing");
	        }
	        
			try {
				
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
						rsp = "Token invalid";
						return rsp;
					
				} else {
					rsp = EntityUtils.toString(resp.getEntity());
				}
				
				
				
			} catch (IllegalStateException e) {
				Log.e("onpostexec", "IllegalStateException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("onpostexec", "IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Exception", "Exception");
				e.printStackTrace();
			}
		
			
	        
	        return rsp;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        String photoPath = null;
		        
		        if (response != null) {
		        	JSONObject jsonResponse = null;

					try {
						jsonResponse = new JSONObject(response);
						
						String error = null;
						try {
							error = jsonResponse.getString("error");
						} catch (JSONException e) {
							
						}
						
						if (error != null) {
							resolvePendingUpdates();
							return;
						}
						
			  			SQLiteDatabase db = MainActivity.dbHelper.getWritableDatabase();
			  			ContentValues values = new ContentValues();
						long photoId = -1;
						
						try {
							JSONObject photo = new JSONObject(jsonResponse.getString("photo"));
							
							photoPath = base64SaveImage(photo.getString("base64_image"));

							
							//add photo
							if (photoPath != null) {
								//add photo
					  			values = new ContentValues();
								values.put("lat", photo.getString("lat"));
								values.put("long", photo.getString("lon"));
								values.put("accuracy", photo.getString("gps_accuracy"));
								long locationId = db.insert("location", null, values);
		
								//add location
								values = new ContentValues();
								values.put("location_id", locationId);
								values.put("name", photoPath);
								photoId = db.insert("photo", null, values);

							}
							
						} catch (Exception e) {
							
						}
						
						
						Date date = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");						
						try {
							JSONObject settings = new JSONObject(jsonResponse.getString("settings"));
							

						    Calendar calendar = Calendar.getInstance();
						    calendar.setTime(date);
						    
						    calendar.add(Calendar.DAY_OF_MONTH, Integer.parseInt(settings.getString("next_update")));
						    date = (Date) calendar.getTime();
						} catch (Exception e) {
							
						}
						

						
			  			JSONObject location = new JSONObject(jsonResponse.getString("primary_location"));
			  			
						//add location
			  			values = new ContentValues();
						values.put("lat", location.getString("lat"));
						values.put("long", location.getString("lon"));
						values.put("accuracy", location.getString("gps_accuracy"));
						long locationId = db.insert("location", null, values);
						
						Cursor treeExists = db.rawQuery("SELECT * FROM tree WHERE main_db_id = " + jsonResponse.getString("id"), null);
						long treeId = -1;
						
						String priority = null;
						try {
							priority = jsonResponse.getString("priority").equals("1") ? "Y" : "N" ;
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						
						if (treeExists.moveToNext()) {
							treeId = Long.parseLong(treeExists.getString(treeExists.getColumnIndex("_id")));
							
							String photoOutdated = "select photo._id from tree_photo left outer join photo on photo_id = photo._id where tree_id = " +
									"" + treeExists.getString(treeExists.getColumnIndex("_id"));
							
							Log.i("query", photoOutdated);
							
							Cursor photoCursor = db.rawQuery(photoOutdated, null);
							
							while(photoCursor.moveToNext()){
								values = new ContentValues();
								values.put("is_outdated", "Y");
								
								db.update("photo", values, "_id = ?", new String[] { photoCursor.getString(photoCursor.getColumnIndex("_id")) });
							}
							
							values = new ContentValues();
							values.put("time_created", jsonResponse.getString("time_created"));
							values.put("time_updated", jsonResponse.getString("time_updated"));
							values.put("time_for_update", dateFormat.format(date));
							values.put("is_missing", jsonResponse.getString("missing").equals("0") ? "N" : "Y");
							values.put("is_synced", "Y");
							values.put("is_priority", (priority == null ? "N" : priority ));
							values.put("location_id", locationId);
							
							
							
							db.update("tree", values, "_id = ?", new String[] { treeExists.getString(treeExists.getColumnIndex("_id")) });
							
						} else {
							//add tree
							values = new ContentValues();
							values.put("time_created", jsonResponse.getString("time_created"));
							values.put("time_updated", jsonResponse.getString("time_updated"));
							values.put("is_missing", jsonResponse.getString("missing").equals("0") ? "N" : "Y");
							values.put("time_for_update", dateFormat.format(date));
							values.put("is_synced", "Y");
							values.put("is_priority", (priority == null ? "N" : priority ));
							values.put("main_db_id", jsonResponse.getString("id"));
							values.put("location_id", locationId);
							treeId = db.insert("tree", null, values);

						}
						
						
						try {
							
							Log.e("notes", jsonResponse.getString("notes"));
							
							JSONArray notes = new JSONArray(jsonResponse.getString("notes"));
							
							for (int k = 0; k < notes.length(); k++) {
								if (notes.get(k) instanceof JSONObject) {
									JSONObject obj = (JSONObject)(notes.get(k));
									
									String noteExists = "SELECT * FROM note WHERE content = '" + obj.getString("content") + "' AND time_created = '" + obj.getString("time_created") + "'";
									
									Cursor noteExistsCursor = db.rawQuery(noteExists, null);
									
									Log.d("note exists", noteExists);
									
									if (noteExistsCursor.moveToFirst()) {
										Log.e("ne kuzim", "1");
										continue;
									} else {
										Log.e("ne kuzim", "2");
										values = new ContentValues();
										values.put("content", obj.getString("content"));
										values.put("time_created", obj.getString("time_created"));
										long noteId = db.insert("note", null, values);
										
										values = new ContentValues();
										values.put("tree_id", treeId);
										values.put("note_id", noteId);
										long treeNoteId = db.insert("tree_note", null, values);
									}
 									
								}
							}
							
							
							
						} catch (Exception e) {
							Log.i("exseption", e.getLocalizedMessage());
						}
						
						
						
						if (photoId != -1) {
							// tree_photo
							values = new ContentValues();
							values.put("tree_id", treeId);
							values.put("photo_id", photoId);
							long treePhotoId = db.insert("tree_photo", null, values);
						}
						
						Cursor pendingUpdateCursor = db.rawQuery("SELECT main_db_id FROM pending_updates WHERE main_db_id IS NOT NULL AND tree_id = " + jsonResponse.getString("id"), null);
						
						if (pendingUpdateCursor.moveToFirst()) {
							clearPendingUpdate = new ClearPendingUpdateTask().execute(new String[]{pendingUpdateCursor.getString(pendingUpdateCursor.getColumnIndex("main_db_id"))});
						}
						
						
						db.delete("pending_updates", "tree_id = ?", new String[] { jsonResponse.getString("id") });
						db.close();
						
						resolvePendingUpdates();
						Log.i("tree", jsonResponse.toString());
						
					} catch (JSONException e) {
						Log.i("tree", response);
						Log.i("something to catch", e.getLocalizedMessage());
					}
		  			
		        } else {
	        		
		        }
				
		        
		 }
		
		
	}
	
	
	class GetSettingTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
	        String settingId = params[0];
	        
	        Log.d("params", "params");
	        
	        if (settingId == null)
	        	return "";
	        
	        Log.d("params", settingId);
	        
			String rsp = null;
	        
	        HttpGet post = null;
			post = new HttpGet(NetworkUtilities.SETTINGS_SYNC_URI + settingId + "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));

	        post.setHeader("Accept-Charset","utf-8");


	        DefaultHttpClient mHttpClient = NetworkUtilities.createHttpClient();
	        
	        try {
	            resp = mHttpClient.execute(post);
	        } catch (final ConnectException e) {
	            Log.d("", "Connect exception", e);
	        } catch (final IOException e) {
	                Log.v("", "IOException when getting authtoken", e);
	        } finally {
	                Log.v("", "getAuthtoken completing");
	        }
	        
			try {
				
				if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
						rsp = "Token invalid";
						return rsp;
					
				} else {
					rsp = EntityUtils.toString(resp.getEntity());
				}
				
				
				
			} catch (IllegalStateException e) {
				Log.e("onpostexec", "IllegalStateException");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e("onpostexec", "IOException");
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Exception", "Exception");
				e.printStackTrace();
			}
		
			
	        
	        return rsp;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        Log.i("SETTINGS", response + " ");
		        
		        if (response != null) {
		        	JSONObject jsonResponse = null;

					try {
						
						jsonResponse = new JSONObject(response);
						
						
						String error = null;
						try {
							error = jsonResponse.getString("error");
						} catch (JSONException e) {
							
						}
						
						if (error != null) {
							return;
						}
						
						
			  			SQLiteDatabase db = MainActivity.dbHelper.getWritableDatabase();

			  			
						ContentValues values = new ContentValues();
						values.put("main_db_id", jsonResponse.getString("id"));
						values.put("time_to_next_update", jsonResponse.getString("next_update"));
						values.put("min_accuracy", jsonResponse.getString("min_gps_accuracy"));
						
						mSharedPreferences.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, Integer.parseInt(jsonResponse.getString("next_update"))).commit();
						mSharedPreferences.edit().putBoolean(ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING_PRESENT, true).commit();
						
//						mSharedPreferences.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, Integer.parseInt(jsonResponse.getString("min_gps_accuracy"))).commit();
						
						long settingsId = db.insert("settings", null, values);
						
						Cursor pendingUpdateCursor = db.rawQuery("SELECT main_db_id FROM pending_updates WHERE settings_id = " + jsonResponse.getString("id"), null);
						
						if (pendingUpdateCursor.moveToFirst()) {
							clearPendingUpdate = new ClearPendingUpdateTask().execute(new String[]{pendingUpdateCursor.getString(pendingUpdateCursor.getColumnIndex("main_db_id"))});
						}

					
						db.delete("pending_updates", "settings_id = ?", new String[] { jsonResponse.getString("id") });
						db.close();
						
						
					} catch (JSONException e) {
						Log.i("something to catch", e.getLocalizedMessage());
					}
		  			
		        } else {
	        		
		        }
				
		        resolvePendingUpdates();
		 }
		
		
	}


	
	private String base64Image(String path) {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, bmOptions);
		int imageHeight = bmOptions.outHeight;
		int imageWidth = bmOptions.outWidth;
		String imageType = bmOptions.outMimeType;

		ExifInterface exif = null;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
		int orientation = orientString != null ? Integer.parseInt(orientString)
				: ExifInterface.ORIENTATION_NORMAL;
		int rotationAngle = 0;
		if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
			rotationAngle = 90;
		if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
			rotationAngle = 180;
		if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
			rotationAngle = 270;
		
		// Calculate your sampleSize based on the requiredWidth and
		// originalWidth
		// For e.g you want the width to stay consistent at 500dp
		int requiredWidth = 800;

		if (imageHeight > imageWidth) {
			requiredWidth = 600;
		}
		

		int sampleSize = (int) Math.ceil((float) imageWidth
				/ (float) requiredWidth);

		// If the original image is smaller than required, don't sample
		if (sampleSize < 1) {
			sampleSize = 1;
		}

		bmOptions.inSampleSize = sampleSize;
		bmOptions.inPurgeable = true;
		bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		bmOptions.inJustDecodeBounds = false;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
		
		Matrix matrix = new Matrix();
		matrix.setRotate(rotationAngle, (float) bitmap.getWidth() / 2,
				(float) bitmap.getHeight() / 2);
		Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
				bmOptions.outWidth, bmOptions.outHeight, matrix, true);
		
//Bitmap photo = Utils.resizedImage(mCurrentPhotoPath);
//		
//        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//        photo.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
		
		
	    int compressionQuality = 80;
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality,
                byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        
         
//
//		
//       ContextWrapper cw = new ContextWrapper(getActivity());
//        // path to /data/data/yourapp/app_data/imageDir
//       File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
//       // Create imageDir
//       File mypath=new File("/storage/sdcard0/Pictures/TreeTracker/test1234.jpg");
//
//       FileOutputStream fos = null;
//       try {           
//
//           fos = new FileOutputStream(mypath);
//
//      // Use the compress method on the BitMap object to write image to the OutputStream
//           rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//           fos.close();
//       } catch (Exception e) {
//           e.printStackTrace();
//       }
//		
//        Log.e("encodedImage", encodedImage);
        
		return encodedImage;

		
		
	}
	
	private String getAlbumName() {
		return getString(R.string.album_name);
	}

	
	
	private File getAlbumDir() {
		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {

			storageDir = mAlbumStorageDirFactory
					.getAlbumStorageDir(getAlbumName());

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
	
	private String base64SaveImage(String base64) {

		File f = null;
		try {
			f = createImageFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		String photoPath = f.getAbsolutePath();
		byte[] b = Base64.decode(base64, Base64.DEFAULT);
		
		 FileOutputStream fo;
		try {
			fo = new FileOutputStream(f);
			fo.write(b);
			fo.close();
			
			
			Intent mediaScanIntent = new Intent(
					"android.intent.action.MEDIA_SCANNER_SCAN_FILE");
			Uri contentUri = Uri.fromFile(f);
			mediaScanIntent.setData(contentUri);
			getActivity().sendBroadcast(mediaScanIntent);
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		BitmapFactory.decodeByteArray(b, 0, b.length);
		
		return photoPath;
		
	}
	

	public void resolvePendingUpdates() {
		updateData();
		SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
		
		boolean isThereAJob = false;
		
		Cursor treeCursor = db.rawQuery("SELECT DISTINCT tree_id FROM pending_updates WHERE tree_id NOT NULL and tree_id <> 0", null);

		List<UserTree> trees = ((MainActivity)getActivity()).getUserTrees();
		while (treeCursor.moveToNext()) {
			Log.d("DataFragment", "pending_updates id: " + treeCursor.getString(treeCursor.getColumnIndex("tree_id")));
			Log.d("DataFragment", "getUserTrees id: " + trees.get(0).getId());
		}

		if (treeCursor.moveToFirst()) {
			isThereAJob = true;
			getTreeUpdate = new GetTreeTask().execute(new String[]{treeCursor.getString(treeCursor.getColumnIndex("tree_id"))});
		} else {
			
			if (mSharedPreferences.getBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false)) {
				if (progressDialog != null) {
					progressDialog.dismiss();
					isThereAJob = true;
				}
			}
			
			mSharedPreferences.edit().putBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false).commit();
			Cursor settingsCursor = db.rawQuery("SELECT DISTINCT settings_id FROM pending_updates WHERE settings_id NOT NULL and settings_id <> 0", null);
			
			if (settingsCursor.moveToFirst()) {
				isThereAJob = true;
				getSettingUpdate = new GetSettingTask().execute(new String[]{settingsCursor.getString(settingsCursor.getColumnIndex("settings_id"))});
			}
		}
		
		if (!isThereAJob) {
			Toast.makeText(getActivity(), "Sync complete", Toast.LENGTH_SHORT).show();
			if (syncFromExitStarted) {
				syncFromExitStarted = false;
				getActivity().finish();
			}
		}
		
		
		
		
		db.close();
	}


}
