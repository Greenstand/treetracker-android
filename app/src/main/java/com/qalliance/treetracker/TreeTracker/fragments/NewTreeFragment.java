package com.qalliance.treetracker.TreeTracker.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.activities.CameraActivity;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.application.Permissions;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class NewTreeFragment extends Fragment implements OnClickListener, TextWatcher, ActivityCompat.OnRequestPermissionsResultCallback {

	private ImageView mImageView;
	private String mCurrentPhotoPath;
	private long userId;
	private SharedPreferences mSharedPreferences;
    private Uri mPhotoUri;

	public NewTreeFragment() {
		// some overrides and settings go here
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

		View v = inflater.inflate(R.layout.fragment_new_tree, container, false);
		
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		
		((RelativeLayout)v.findViewById(R.id.fragment_new_tree)).setVisibility(View.INVISIBLE);

	    ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.new_tree);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mSharedPreferences = getActivity().getSharedPreferences(
				"com.qalliance.treetracker", Context.MODE_PRIVATE);

		userId = mSharedPreferences.getLong(ValueHelper.MAIN_USER_ID, -1);

		Button saveBtn = (Button) v.findViewById(R.id.fragment_new_tree_save);
		saveBtn.setOnClickListener(NewTreeFragment.this);

		Button resetGPSLocBtn = (Button) v
				.findViewById(R.id.fragment_new_tree_reset_gps);
		resetGPSLocBtn.setOnClickListener(NewTreeFragment.this);
		
		ImageButton takePhoto = (ImageButton) v.findViewById(R.id.fragment_new_tree_take_photo);
		takePhoto.setOnClickListener(NewTreeFragment.this);
		

		mImageView = (ImageView) v.findViewById(R.id.fragment_new_tree_image);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			new FroyoAlbumDirFactory();
		} else {
			new BaseAlbumDirFactory();
		}
		
		TextView newTreeDistance = (TextView)v.findViewById(R.id.fragment_new_tree_distance);
		newTreeDistance.setText(Integer.toString(0) + " " + getResources().getString(R.string.meters));
		
		TextView newTreeGpsAccuracy = (TextView)v.findViewById(R.id.fragment_new_tree_gps_accuracy);
		if (MainActivity.mCurrentLocation != null) {
			newTreeGpsAccuracy.setText(Integer.toString(Math.round(MainActivity.mCurrentLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
		} else {
			newTreeGpsAccuracy.setText("0 " + getResources().getString(R.string.meters));
		}
		
		
		int timeToNextUpdate = mSharedPreferences.getInt(
				ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences.getInt(
						ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
						ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING));

		EditText newTreetimeToNextUpdate = (EditText)v.findViewById(R.id.fragment_new_tree_next_update);
		newTreetimeToNextUpdate.setText(Integer.toString(timeToNextUpdate));
		
		if (mSharedPreferences.getBoolean(ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING_PRESENT, false)) {
			newTreetimeToNextUpdate.setEnabled(false);
		}
		
		newTreetimeToNextUpdate.addTextChangedListener(NewTreeFragment.this);
		

		takePicture();

		return v;
	}

	public void onClick(View v) {

		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
				HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

		switch (v.getId()) {

		case R.id.fragment_new_tree_save:
			
			saveToDb();
			
			Toast.makeText(getActivity(), "Tree saved", Toast.LENGTH_SHORT).show();
			getActivity().getSupportFragmentManager().popBackStack();
			break;
		case R.id.fragment_new_tree_take_photo:
			takePicture();
			break;
		case R.id.fragment_new_tree_reset_gps:
			((MainActivity)getActivity()).getLocation();
			break;
		}

	}
	
	private void takePicture() {
		if ( ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions( getActivity(), new String[] {  Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE  },
					Permissions.MY_PERMISSION_CAMERA);
		} else {
			Intent takePictureIntent = new Intent(getActivity(), CameraActivity.class);
            /*
			mPhotoUri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
			startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE_CONTENT_RESOLVER);
*/
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

	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			
			mCurrentPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH);
			
			if (mCurrentPhotoPath != null) {
				((RelativeLayout)getActivity().findViewById(R.id.fragment_new_tree)).setVisibility(View.VISIBLE);
				
				MainActivity.mCurrentTreeLocation = new Location("treetracker");
				if (MainActivity.mCurrentLocation != null) {
					MainActivity.mCurrentTreeLocation.setLatitude(MainActivity.mCurrentLocation.getLatitude());
					MainActivity.mCurrentTreeLocation.setLongitude(MainActivity.mCurrentLocation.getLongitude());
				}
				
				setPic();
				
				boolean saveAndEdit = mSharedPreferences.getBoolean(ValueHelper.SAVE_AND_EDIT, true);
				
				if (!saveAndEdit) {
					saveToDb();
					
					Toast.makeText(getActivity(), "Tree saved", Toast.LENGTH_SHORT).show();
					getActivity().getSupportFragmentManager().popBackStack();
				}
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			if (((RelativeLayout)getActivity().findViewById(R.id.fragment_new_tree)).getVisibility() != View.VISIBLE) {
				getActivity().getSupportFragmentManager().popBackStack();
			}
		}
	}

	private void saveToDb() {
		SQLiteDatabase dbw = MainActivity.dbHelper.getWritableDatabase();

		ContentValues contentValues = new ContentValues();

		// location
		contentValues.put("user_id", userId);
		
		if (MainActivity.mCurrentLocation == null) {
			Toast.makeText(getActivity(), "Insufficient accuracy", Toast.LENGTH_SHORT).show();
			getActivity().getSupportFragmentManager().popBackStack();
		} else {
			MainActivity.mCurrentLocation.getAccuracy();
			contentValues.put("user_id", userId);
			contentValues.put("accuracy",
					Float.toString(MainActivity.mCurrentLocation.getAccuracy()));
			contentValues.put("lat",
					Double.toString(MainActivity.mCurrentLocation.getLatitude()));
			contentValues.put("long",
					Double.toString(MainActivity.mCurrentLocation.getLongitude()));

			long locationId = dbw.insert("location", null, contentValues);
			
			
			CheckBox removePhoto = (CheckBox) getActivity().findViewById(R.id.fragment_new_tree_remove_photo);
			

			Log.d("locationId", Long.toString(locationId));

			long photoId = -1;
			if (!removePhoto.isChecked()) {
				Log.e("checked", "false");
				// photo
				contentValues = new ContentValues();
				contentValues.put("user_id", userId);
				contentValues.put("location_id", locationId);
				contentValues.put("name", mCurrentPhotoPath);

				photoId = dbw.insert("photo", null, contentValues);
				Log.d("photoId", Long.toString(photoId));
			} else {
				Log.e("checked", "true");
			}



			int minAccuracy = mSharedPreferences.getInt(
					ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
					ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);
			
			EditText newTreetimeToNextUpdate = (EditText)getActivity().findViewById(R.id.fragment_new_tree_next_update);
			int timeToNextUpdate = Integer.parseInt(newTreetimeToNextUpdate.getText().toString().equals("") ? 
					"0" : newTreetimeToNextUpdate.getText().toString());

			// settings
			contentValues = new ContentValues();
			contentValues.put("time_to_next_update", timeToNextUpdate);
			contentValues.put("min_accuracy", minAccuracy);

			long settingsId = dbw.insert("settings", null, contentValues);
			Log.d("settingsId", Long.toString(settingsId));
			
			
			// note
			String content = ((EditText) getActivity().findViewById(R.id.fragment_new_tree_note)).getText().toString();
			contentValues = new ContentValues();
			contentValues.put("user_id", userId);
			contentValues.put("content", content);
			
			long noteId = dbw.insert("note", null, contentValues);
			Log.d("noteId", Long.toString(noteId));

			

			// tree
			String threeDigitNumber = ((EditText) getActivity().findViewById(R.id.fragment_new_tree_three_digits)).getText().toString();
			contentValues = new ContentValues();
			contentValues.put("user_id", userId);
			contentValues.put("location_id", locationId);
			contentValues.put("settings_id", settingsId);
			contentValues.put("three_digit_number", threeDigitNumber);
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			Date date = new Date();
		    Calendar calendar = Calendar.getInstance();
		    calendar.setTime(date);
		    
		    calendar.add(Calendar.DAY_OF_MONTH, timeToNextUpdate);
		    date = (Date) calendar.getTime();
		    
		    Log.i("date", date.toString());
			
		    contentValues.put("time_created", dateFormat.format(new Date()));
		    contentValues.put("time_updated", dateFormat.format(new Date()));
			contentValues.put("time_for_update", dateFormat.format(date));

			long treeId = dbw.insert("tree", null, contentValues);
			Log.d("treeId", Long.toString(treeId));

			if (!removePhoto.isChecked()) {
				// tree_photo
				contentValues = new ContentValues();
				contentValues.put("tree_id", treeId);
				contentValues.put("photo_id", photoId);
				long treePhotoId = dbw.insert("tree_photo", null, contentValues);
				Log.d("treePhotoId", Long.toString(treePhotoId));
			}
			
			
			// tree_note
			contentValues = new ContentValues();
			contentValues.put("tree_id", treeId);
			contentValues.put("note_id", noteId);

			long treeNoteId = dbw.insert("tree_note", null, contentValues);
			Log.d("treeNoteId", Long.toString(treeNoteId));
		}

		dbw.close();

	}

	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

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
			Toast.makeText(getActivity(), "Error setting image. Please try again.", Toast.LENGTH_SHORT).show();
			getActivity().getSupportFragmentManager().popBackStack();
		}


		ExifInterface exif = null;
		try {
			exif = new ExifInterface(mCurrentPhotoPath);
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

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}
	



	public void afterTextChanged(Editable s) {
		Log.e("days", s.toString());
		

	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

}
