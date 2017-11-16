package com.qalliance.treetracker.TreeTracker.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.activities.CameraActivity;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.application.Permissions;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.database.Tree;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

public class UpdateTreeFragment extends Fragment implements OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
	
	private ImageView mImageView;
	private String mCurrentPhotoPath;
	private String mTakenPhotoPath;
	private String treeIdStr = "";
	private Fragment fragment;
	private Bundle bundle;
	private FragmentTransaction fragmentTransaction;
	private ImageButton previousBtn;
	private ImageButton nextBtn;
	private SharedPreferences mSharedPreferences;
	private long userId;
	private MatrixCursor photoCursor;
	private Cursor initialCursor;
	
	public UpdateTreeFragment() {
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
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		menu.clear();
	}
    	 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState) {
		
	    View v = inflater.inflate(R.layout.fragment_update_tree, container, false);
	    
	    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	    
	    mSharedPreferences = getActivity().getSharedPreferences(
				"com.qalliance.treetracker", Context.MODE_PRIVATE);
	    
	    userId = mSharedPreferences.getLong(ValueHelper.MAIN_USER_ID, -1);
	    
	    ((RelativeLayout)v.findViewById(R.id.fragment_update_tree)).setVisibility(View.INVISIBLE);
	    
	    ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.update_tree);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    mImageView = (ImageView) v.findViewById(R.id.fragment_update_tree_image);
	    
	    
	    previousBtn = (ImageButton) v.findViewById(R.id.fragment_update_tree_previous);
	    nextBtn = (ImageButton) v.findViewById(R.id.fragment_update_tree_next);
	    
	    previousBtn.setOnClickListener(UpdateTreeFragment.this);
	    nextBtn.setOnClickListener(UpdateTreeFragment.this);
	    
		SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
		
		String query = "select distinct tree.*, tree._id as the_tree_id, photo.name, location.* from tree " +
				"left outer join tree_photo on tree._id = tree_id " +
				"left outer join photo on photo_id = photo._id " +
				"left outer join location on location._id = tree.location_id " +
				"where (is_outdated = 'N' or is_outdated is null) and is_missing = 'N'";
		
		Log.e("query", query);
		
		initialCursor = db.rawQuery(query, null);
		photoCursor = new MatrixCursor(new String[]{"name", "the_tree_id", "lat", "long", "accuracy", "time_created", "time_updated", "time_for_update"});
		ArrayList<Tree> orderedList = new ArrayList<Tree>();
		
		while(initialCursor.moveToNext()) {
			Double photoLat = Double.parseDouble(initialCursor.getString(initialCursor.getColumnIndex("lat")));
			Double photoLong = Double.parseDouble(initialCursor.getString(initialCursor.getColumnIndex("long")));
			Float photoAcc = Float.parseFloat(initialCursor.getString(initialCursor.getColumnIndex("accuracy")));
			
			float[] results = {0,0,0};
			if (MainActivity.mCurrentLocation != null) {
				Location.distanceBetween(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude(),
						photoLat, photoLong, results);
			}
			
			int distance = Math.round(results[0]);
			
			if (MainActivity.mCurrentLocation != null) {
				if (MainActivity.mCurrentLocation.hasAccuracy()) {

					boolean imagePresent = true;
					
					if (initialCursor.getString(initialCursor.getColumnIndex("name")) == null) {
						imagePresent = false;
					}

					
					if ((distance < (MainActivity.mCurrentLocation.getAccuracy() + photoAcc)) ) {
						
						orderedList.add(new Tree(distance,
								new Object[] {
										!imagePresent ? null : initialCursor.getString(initialCursor.getColumnIndex("name")),
										initialCursor.getString(initialCursor.getColumnIndex("the_tree_id")),
										initialCursor.getString(initialCursor.getColumnIndex("lat")),
										initialCursor.getString(initialCursor.getColumnIndex("long")),
										initialCursor.getString(initialCursor.getColumnIndex("accuracy")),
										initialCursor.getString(initialCursor.getColumnIndex("time_created")),
										initialCursor.getString(initialCursor.getColumnIndex("time_updated")),
										initialCursor.getString(initialCursor.getColumnIndex("time_for_update"))
										}));
					}
					
				}
			}
		}
		
		
		Collections.sort(orderedList);
		
		Iterator<Tree> iter = orderedList.iterator();
		while (iter.hasNext()) {
			Tree tree = (Tree) iter.next();
			
			Log.e("tree distance", Integer.toString(tree.getDistance()));
			
			photoCursor.addRow(tree.getRestOfData());
			
		}
		
		photoCursor.moveToFirst();
		
		
		if (photoCursor.isLast()) {
			nextBtn.setVisibility(View.INVISIBLE);
		}
		
		if (photoCursor.isFirst()) {
			previousBtn.setVisibility(View.INVISIBLE);
		}
		
		if (photoCursor.getCount() <= 0) {
			Toast.makeText(getActivity(), "No trees to update", Toast.LENGTH_SHORT).show();
			getActivity().getSupportFragmentManager().popBackStack();
		} else {
			
			mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"));
			
			if (mCurrentPhotoPath != null) {
				if (mCurrentPhotoPath.equals("")) {
					mCurrentPhotoPath = null;
				}
			}
			
			
			
			TextView noImage = (TextView) v.findViewById(R.id.fragment_update_tree_no_image);
			if (mCurrentPhotoPath != null) {
				noImage.setVisibility(View.INVISIBLE);
				setPic();
			} else {
				noImage.setVisibility(View.VISIBLE);
			}
			
			MainActivity.mCurrentTreeLocation = new Location("treetracker");
			MainActivity.mCurrentTreeLocation.setLatitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("lat"))));
			MainActivity.mCurrentTreeLocation.setLongitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("long"))));
			MainActivity.mCurrentTreeLocation.setAccuracy(Float.parseFloat(photoCursor.getString(photoCursor.getColumnIndex("accuracy"))));
			
			treeIdStr = photoCursor.getString(photoCursor.getColumnIndex("the_tree_id"));
			
			float[] results = {0,0,0};
			if (MainActivity.mCurrentLocation != null) {
				Location.distanceBetween(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude(),
						MainActivity.mCurrentTreeLocation.getLatitude(), MainActivity.mCurrentTreeLocation.getLongitude(), results);
			}
			
			TextView distanceTxt = (TextView) v.findViewById(R.id.fragment_update_tree_distance);
			distanceTxt.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
			
			TextView accuracyTxt = (TextView) v.findViewById(R.id.fragment_update_tree_gps_accuracy);
			accuracyTxt.setText(Integer.toString(Math.round(MainActivity.mCurrentTreeLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
			

			TextView createdTxt = (TextView) v.findViewById(R.id.fragment_update_tree_created);
			createdTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_created")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_created")).lastIndexOf(":")));
			
			TextView updatedTxt = (TextView) v.findViewById(R.id.fragment_update_tree_last_update);
			updatedTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_updated")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_updated")).lastIndexOf(":")));
			
			
			TextView statusTxt = (TextView) v.findViewById(R.id.fragment_update_tree_image_status);
			
			Date dateForUpdate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				dateForUpdate = dateFormat.parse(photoCursor.getString(photoCursor.getColumnIndex("time_for_update")));
				
				Log.e("dateForupdate", dateForUpdate.toLocaleString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.e("date", dateForUpdate.toLocaleString());
			if (dateForUpdate.before(new Date())) {
				statusTxt.setText(R.string.outdated);
			}		
		}
		
		
		Button yesBtn = (Button) v.findViewById(R.id.fragment_update_tree_yes);
		yesBtn.setOnClickListener(UpdateTreeFragment.this);

		mImageView = (ImageView) v.findViewById(R.id.fragment_update_tree_image);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			new FroyoAlbumDirFactory();
		} else {
			new BaseAlbumDirFactory();
		}

		if (photoCursor.getCount() > 0) {
			takePicture();
		}

//		
//		do {
//			mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"));
//			
//			setPic();
//		} while (photoCursor.moveToNext());
  
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
			case R.id.fragment_update_tree_previous:
				if (!photoCursor.isFirst()) {
					photoCursor.moveToPrevious();
					mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"));
					
					TextView noImage = (TextView) getActivity().findViewById(R.id.fragment_update_tree_no_image);
					if (mCurrentPhotoPath != null) {
						setPic();
						noImage.setVisibility(View.INVISIBLE);
						mImageView.setVisibility(View.VISIBLE);
						Log.d("ovdje", "2");
					} else {
						noImage.setVisibility(View.VISIBLE);
						mImageView.setVisibility(View.INVISIBLE);
					}
					
					treeIdStr = photoCursor.getString(photoCursor.getColumnIndex("the_tree_id"));
					
					
					MainActivity.mCurrentTreeLocation = new Location("treetracker");
					MainActivity.mCurrentTreeLocation.setLatitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("lat"))));
					MainActivity.mCurrentTreeLocation.setLongitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("long"))));
					MainActivity.mCurrentTreeLocation.setAccuracy(Float.parseFloat(photoCursor.getString(photoCursor.getColumnIndex("accuracy"))));
					
					float[] results = {0,0,0};
					if (MainActivity.mCurrentLocation != null) {
						Location.distanceBetween(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude(),
								MainActivity.mCurrentTreeLocation.getLatitude(), MainActivity.mCurrentTreeLocation.getLongitude(), results);
					}
					
					TextView distanceTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_distance);
					distanceTxt.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
					
					TextView accuracyTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_gps_accuracy);
					accuracyTxt.setText(Integer.toString(Math.round(MainActivity.mCurrentTreeLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
					

					TextView createdTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_created);
					createdTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_created")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_created")).lastIndexOf(":")));
					
					TextView updatedTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_last_update);
					updatedTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_updated")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_updated")).lastIndexOf(":")));
					
					if (!photoCursor.isLast()) {
						nextBtn.setVisibility(View.VISIBLE);
					}
					
					if (photoCursor.isFirst()) {
						previousBtn.setVisibility(View.INVISIBLE);
					}
					
					TextView statusTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_image_status);
					
					Date dateForUpdate = new Date();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					try {
						dateForUpdate = dateFormat.parse(photoCursor.getString(photoCursor.getColumnIndex("time_for_update")));
						
						Log.e("dateForupdate", dateForUpdate.toLocaleString());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.e("date", dateForUpdate.toLocaleString());
					if (dateForUpdate.before(new Date())) {
						statusTxt.setText(R.string.outdated);
					} else {
						statusTxt.setText(R.string.ok);
					}

				}
				
				break;
			case R.id.fragment_update_tree_next:
				
				if (!photoCursor.isLast()) {
					photoCursor.moveToNext();
					mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"));
					
					TextView noImage = (TextView) getActivity().findViewById(R.id.fragment_update_tree_no_image);
					
					if (mCurrentPhotoPath != null) {
						setPic();
						noImage.setVisibility(View.INVISIBLE);
						mImageView.setVisibility(View.VISIBLE);
						Log.d("ovdje", "3");
					} else {
						noImage.setVisibility(View.VISIBLE);
						mImageView.setVisibility(View.INVISIBLE);
					}

					
					treeIdStr = photoCursor.getString(photoCursor.getColumnIndex("the_tree_id"));
					
					MainActivity.mCurrentTreeLocation = new Location("treetracker");
					MainActivity.mCurrentTreeLocation.setLatitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("lat"))));
					MainActivity.mCurrentTreeLocation.setLongitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("long"))));
					MainActivity.mCurrentTreeLocation.setAccuracy(Float.parseFloat(photoCursor.getString(photoCursor.getColumnIndex("accuracy"))));
					
					float[] results = {0,0,0};
					if (MainActivity.mCurrentLocation != null) {
						Location.distanceBetween(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude(),
								MainActivity.mCurrentTreeLocation.getLatitude(), MainActivity.mCurrentTreeLocation.getLongitude(), results);
					}
					
					TextView distanceTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_distance);
					distanceTxt.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
					
					TextView accuracyTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_gps_accuracy);
					accuracyTxt.setText(Integer.toString(Math.round(MainActivity.mCurrentTreeLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
					

					TextView createdTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_created);
					createdTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_created")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_created")).lastIndexOf(":")));
					
					TextView updatedTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_last_update);
					updatedTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_updated")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_updated")).lastIndexOf(":")));
					
					if (!photoCursor.isFirst()) {
						previousBtn.setVisibility(View.VISIBLE);
					}
					
					if (photoCursor.isLast()) {
						nextBtn.setVisibility(View.INVISIBLE);
					}
					
					TextView statusTxt = (TextView) getActivity().findViewById(R.id.fragment_update_tree_image_status);
					
					Date dateForUpdate = new Date();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					try {
						dateForUpdate = dateFormat.parse(photoCursor.getString(photoCursor.getColumnIndex("time_for_update")));
						
						Log.e("dateForupdate", dateForUpdate.toLocaleString());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.e("date", dateForUpdate.toLocaleString());
					if (dateForUpdate.before(new Date())) {
						statusTxt.setText(R.string.outdated);
					} else {
						statusTxt.setText(R.string.ok);
					}

					

				}
				
				break;		
			case R.id.fragment_update_tree_yes:
				
				boolean saveAndEdit = mSharedPreferences.getBoolean(ValueHelper.SAVE_AND_EDIT, true);
				
				if (!saveAndEdit) {
					saveToDb();
					
					Toast.makeText(getActivity(), "Tree saved", Toast.LENGTH_SHORT).show();
					getActivity().getSupportFragmentManager().popBackStack();
				} else {
					fragment = new UpdateTreeDetailsFragment();
					bundle = getActivity().getIntent().getExtras();
					
					if (bundle == null)
						bundle = new Bundle();
					
					Log.e("treeIdStr", treeIdStr);
					
					bundle.putString(ValueHelper.TREE_ID, treeIdStr);
					
					Log.e("TakenPhotoPath", mTakenPhotoPath);
					
					BitmapFactory.Options bmOptions = new BitmapFactory.Options();
					bmOptions.inJustDecodeBounds = false;
					Bitmap testBmp = BitmapFactory.decodeFile(mTakenPhotoPath, bmOptions);
					if (testBmp == null) {
						testBmp = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
						if (testBmp == null) {
							Log.e(" i current je null", "1234");
						}
					}

					
					
					bundle.putString(ValueHelper.TREE_PHOTO, testBmp == null ? mCurrentPhotoPath : mTakenPhotoPath);
					
					fragment.setArguments(bundle);
					
					fragmentTransaction = getActivity().getSupportFragmentManager()
							.beginTransaction();
					fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.UPDATE_TREE_DETAILS_FRAGMENT).commit();
				}

				break;
		}
		
	}
    private void takePicture() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( getActivity(), new String[] {  Manifest.permission.CAMERA  },
                    Permissions.MY_PERMISSION_CAMERA);
        } else {
            Intent takePictureIntent = new Intent(getActivity(), CameraActivity.class);
            startActivityForResult(takePictureIntent, ValueHelper.INTENT_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Permissions.MY_PERMISSION_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        }
    }
	
	private void setPic() {
		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int imageWidth = bmOptions.outWidth;
		// Calculate your sampleSize based on the requiredWidth and
		// originalWidth
		// For e.g you want the width to stay consistent at 500dp
		int requiredWidth = (int) (500 * getResources().getDisplayMetrics().density);

		Log.e("required Width ", Integer.toString(requiredWidth));
		Log.e("imageWidth  ", Integer.toString(imageWidth));

		int sampleSize = (int) Math.ceil((float) imageWidth
				/ (float) requiredWidth);

		Log.e("sampleSize ", Integer.toString(sampleSize));
		// If the original image is smaller than required, don't sample
		if (sampleSize < 1) {
			sampleSize = 1;
		}

		Log.e("sampleSize 2 ", Integer.toString(sampleSize));
		bmOptions.inSampleSize = sampleSize;
		bmOptions.inPurgeable = true;
		bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
		bmOptions.inJustDecodeBounds = false;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		if (bitmap == null) {
			return;
		}

		ExifInterface exif = null;
		try {
			exif = new ExifInterface(mCurrentPhotoPath);
		} catch (IOException e) {
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

		Log.d("rotationAngle", Integer.toString(rotationAngle));

		Matrix matrix = new Matrix();
		matrix.setRotate(rotationAngle, (float) bitmap.getWidth() / 2,
				(float) bitmap.getHeight() / 2);
		Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
				bmOptions.outWidth, bmOptions.outHeight, matrix, true);

		/* Associate the Bitmap to the ImageView */
		mImageView.setImageBitmap(rotatedBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			
			mTakenPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH);
			
			if (mTakenPhotoPath != null) {
				((RelativeLayout)getActivity().findViewById(R.id.fragment_update_tree)).setVisibility(View.VISIBLE);
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			if (((RelativeLayout)getActivity().findViewById(R.id.fragment_update_tree)).getVisibility() != View.VISIBLE) {
				getActivity().getSupportFragmentManager().popBackStack();
			}
		}
	}

	private void saveToDb() {
		SQLiteDatabase dbw = MainActivity.dbHelper.getWritableDatabase();

		ContentValues contentValues = new ContentValues();

		// location
		contentValues.put("user_id", userId);

		MainActivity.mCurrentLocation.getAccuracy();
		contentValues.put("user_id", userId);
		contentValues.put("accuracy",
				Float.toString(MainActivity.mCurrentLocation.getAccuracy()));
		contentValues.put("lat",
				Double.toString(MainActivity.mCurrentLocation.getLatitude()));
		contentValues.put("long",
				Double.toString(MainActivity.mCurrentLocation.getLongitude()));

		long locationId = dbw.insert("location", null, contentValues);

		Log.d("locationId", Long.toString(locationId));

		

		String photoOutdated = "select photo._id from tree_photo left outer join photo on photo_id = photo._id where tree_id = " + treeIdStr;
		
		Log.i("query", photoOutdated);
		
		Cursor photoCursor = dbw.rawQuery(photoOutdated, null);
		
		while(photoCursor.moveToNext()){
			contentValues = new ContentValues();
			contentValues.put("is_outdated", "Y");
			
			dbw.update("photo", contentValues, "_id = ?", new String[] { photoCursor.getString(photoCursor.getColumnIndex("_id")) });
		}
		
//		db.update(TABLE_CONTACTS, values, KEY_ID + " = ?",
//new String[] { String.valueOf(contact.getID()) });
		
		// photo
		contentValues = new ContentValues();
		contentValues.put("user_id", userId);
		contentValues.put("location_id", locationId);
		contentValues.put("name", mTakenPhotoPath);

		long photoId = dbw.insert("photo", null, contentValues);
		Log.d("photoId", Long.toString(photoId));

		
		int timeToNextUpdate = mSharedPreferences.getInt(
				ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences.getInt(
						ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
						ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING));
		

		int minAccuracy = mSharedPreferences.getInt(
				ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
				ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);

		// settings
		contentValues = new ContentValues();
		contentValues.put("time_to_next_update", timeToNextUpdate);
		contentValues.put("min_accuracy", minAccuracy);

		long settingsId = dbw.insert("settings", null, contentValues);
		Log.d("settingsId", Long.toString(settingsId));
		
		
		// tree
		contentValues = new ContentValues();
		contentValues.put("user_id", userId);
		contentValues.put("location_id", locationId);
		contentValues.put("settings_id", settingsId);
		contentValues.put("three_digit_number", "000");
		contentValues.put("is_synced", "N");
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Date date = new Date();
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(date);
	    
	    
	    calendar.add(Calendar.DAY_OF_MONTH, timeToNextUpdate);
	    date = (Date) calendar.getTime();
	    
	    Log.i("date", date.toString());
		
		contentValues.put("time_for_update", dateFormat.format(date));
		contentValues.put("time_updated", dateFormat.format(new Date()));

		dbw.update("tree", contentValues, "_id = ?", new String[] { treeIdStr });

		long treeId = Long.parseLong(treeIdStr);
		Log.d("treeId", Long.toString(treeId));

		// tree_photo
		contentValues = new ContentValues();
		contentValues.put("tree_id", treeId);
		contentValues.put("photo_id", photoId);

		long treePhotoId = dbw.insert("tree_photo", null, contentValues);
		Log.d("treePhotoId", Long.toString(treePhotoId));
		
		
		dbw.close();

	}


}
