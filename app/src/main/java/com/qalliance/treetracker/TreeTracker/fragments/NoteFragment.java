package com.qalliance.treetracker.TreeTracker.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qalliance.treetracker.TreeTracker.activities.CameraActivity;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.application.Permissions;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class NoteFragment extends Fragment implements OnClickListener, OnCheckedChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

	private ImageView mImageView;
	private String mCurrentPhotoPath;
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	private Bitmap mImageBitmap;
	private Fragment fragment;
	private Bundle bundle;
	private FragmentTransaction fragmentTransaction;
	private long userId;
	private SharedPreferences mSharedPreferences;
	private String treeIdStr;
	private boolean mTreeIsMissing;

	public NoteFragment() {
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

		View v = inflater.inflate(R.layout.fragment_note, container, false);
		
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		
	    ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.tree_preview);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    Bundle extras = getArguments();
	    
	    treeIdStr = extras.getString(ValueHelper.TREE_ID);
		
		mSharedPreferences = getActivity().getSharedPreferences(
				"com.qalliance.treetracker", Context.MODE_PRIVATE);

		userId = mSharedPreferences.getLong(ValueHelper.MAIN_USER_ID, -1);

		Button saveBtn = (Button) v.findViewById(R.id.fragment_note_save);
		saveBtn.setOnClickListener(NoteFragment.this);

		Button treeMissingBtn = (Button) v
				.findViewById(R.id.fragment_note_tree_missing);
		treeMissingBtn.setOnClickListener(NoteFragment.this);
		
		CheckBox treeMissingChk = (CheckBox) v.findViewById(R.id.fragment_note_missing_tree_checkbox);
		treeMissingChk.setOnCheckedChangeListener(NoteFragment.this);

		mImageView = (ImageView) v.findViewById(R.id.fragment_note_image);
	    
		SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
		
		String query = "select * from tree " +
				"left outer join location on location._id = tree.location_id " +
				"left outer join tree_photo on tree._id = tree_id " +
				"left outer join photo on photo._id = photo_id where is_outdated = 'N' and tree._id =" + treeIdStr;
		
		Cursor photoCursor = db.rawQuery(query, null);
		photoCursor.moveToFirst();

		do {
			mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"));

			TextView noImage = (TextView) v.findViewById(R.id.fragment_note_no_image);
			
			if (mCurrentPhotoPath != null) {
				setPic();
				noImage.setVisibility(View.INVISIBLE);
			} else {
				noImage.setVisibility(View.VISIBLE);
			}
			
			String lat = photoCursor.getString(photoCursor.getColumnIndex("lat"));
			String lon = photoCursor.getString(photoCursor.getColumnIndex("long"));
			
			MainActivity.mCurrentTreeLocation = new Location("treetracker");
			
			MainActivity.mCurrentTreeLocation.setLatitude(Double.parseDouble(lat));
			MainActivity.mCurrentTreeLocation.setLongitude(Double.parseDouble(lon));
			
		} while (photoCursor.moveToNext());
		
		db.close();

		return v;
	}

	public void onClick(View v) {

		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
				HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

		switch (v.getId()) {

		case R.id.fragment_note_save:
			
			if (mTreeIsMissing) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				 
				builder.setTitle(R.string.tree_missing);
				builder.setMessage(R.string.you_are_about_to_mark_this_tree_as_missing);
				 
				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				 
				   public void onClick(DialogInterface dialog, int which) {
				         
						saveToDb();
							
						Toast.makeText(getActivity(), "Tree saved", Toast.LENGTH_SHORT)
								.show();
						FragmentManager manager = getActivity().getSupportFragmentManager();
						FragmentManager.BackStackEntry second = manager.getBackStackEntryAt(1);
						manager.popBackStack(second.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
						
				        dialog.dismiss();
				   }
				 
				});
				 
				 
				builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				 
				   public void onClick(DialogInterface dialog, int which) {
				 
				        // Code that is executed when clicking NO
				 
				        dialog.dismiss();
				   }
				 
				});
				 
				 
				AlertDialog alert = builder.create();
				alert.show();
			} else {
				saveToDb();
					
				Toast.makeText(getActivity(), "Tree saved", Toast.LENGTH_SHORT)
						.show();
				
				FragmentManager manager = getActivity().getSupportFragmentManager();
				FragmentManager.BackStackEntry second = manager.getBackStackEntryAt(1);
				manager.popBackStack(second.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);			
				
			}
			
			
			
			break;
		case R.id.fragment_note_tree_missing:
//			takePicture();
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

	/* Photo album for this application */
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

	private File setUpPhotoFile() throws IOException {

		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();

		return f;
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			
			mCurrentPhotoPath = data.getStringExtra(ValueHelper.TAKEN_IMAGE_PATH);
			
			if (mCurrentPhotoPath != null) {
				((RelativeLayout)getActivity().findViewById(R.id.fragment_note)).setVisibility(View.VISIBLE);
				setPic();			
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
//			if (((RelativeLayout)getActivity().findViewById(R.id.fragment_new_tree)).getVisibility() != View.VISIBLE) {
//				getActivity().getSupportFragmentManager().popBackStack();
//			}
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

		// note
		String content = ((EditText) getActivity().findViewById(R.id.fragment_note_note)).getText().toString();
		contentValues = new ContentValues();
		contentValues.put("user_id", userId);
		contentValues.put("content", content);
		
		long noteId = dbw.insert("note", null, contentValues);
		Log.d("noteId", Long.toString(noteId));
		

		// tree
		contentValues = new ContentValues();
		contentValues.put("location_id", locationId);
		contentValues.put("is_synced", "N");
		contentValues.put("is_priority", "N");
		
		if (mTreeIsMissing) {
			Log.e("missing", "ok");
			contentValues.put("is_missing", "Y");
			contentValues.put("cause_of_death_id", noteId);
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Date date = new Date();
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(date);
	    
		int timeToNextUpdate = mSharedPreferences.getInt(
				ValueHelper.TIME_TO_NEXT_UPDATE_ADMIN_DB_SETTING, mSharedPreferences.getInt(
						ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING,
						ValueHelper.TIME_TO_NEXT_UPDATE_DEFAULT_SETTING));
	    
	    calendar.add(Calendar.DAY_OF_MONTH, timeToNextUpdate);
	    date = (Date) calendar.getTime();
	    
	    Log.i("date", date.toString());
		
		contentValues.put("time_for_update", dateFormat.format(date));
		contentValues.put("time_updated", dateFormat.format(new Date()));

		dbw.update("tree", contentValues, "_id = ?", new String[] { treeIdStr });
		
		// tree_note
		contentValues = new ContentValues();
		contentValues.put("tree_id", treeIdStr);
		contentValues.put("note_id", noteId);

		long treeNoteId = dbw.insert("tree_note", null, contentValues);
		Log.d("treeNoteId", Long.toString(treeNoteId));



		dbw.close();

	}

	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int imageHeight = bmOptions.outHeight;
		int imageWidth = bmOptions.outWidth;
		String imageType = bmOptions.outMimeType;

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

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.fragment_note_missing_tree_checkbox:
			mTreeIsMissing = isChecked;
			EditText noteTxt = (EditText) getActivity().findViewById(R.id.fragment_note_note);
			
			if (isChecked) {
				noteTxt.setHint(getActivity().getResources().getString(R.string.cause_of_death));
			} else {
				noteTxt.setHint(getActivity().getResources().getString(R.string.add_text_note));
			}
			break;

		default:
			break;
		}
		
	}

}
