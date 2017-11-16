package com.qalliance.treetracker.TreeTracker.fragments;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TreePreviewFragment extends Fragment implements OnClickListener {
	
	private ImageView mImageView;
	private String mCurrentPhotoPath;
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	private Bitmap mImageBitmap;
	private String treeIdStr = "";
	private Fragment fragment;
	private FragmentTransaction fragmentTransaction;
	private Bundle bundle;
	
	public TreePreviewFragment() {
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
		
	    View v = inflater.inflate(R.layout.fragment_tree_preview, container, false);
	    
	    ((TextView)getActivity().findViewById(R.id.toolbar_title)).setText(R.string.tree_preview);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	    
	    Bundle extras = getArguments();
	    
	    treeIdStr = extras.getString(ValueHelper.TREE_ID);
	    
	    mImageView = (ImageView) v.findViewById(R.id.fragment_tree_preview_image);
	    
	    ((Button) v.findViewById(R.id.fragment_tree_preview_more)).setOnClickListener(TreePreviewFragment.this);
	    
	    
		SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
		String query = "select * from tree " +
				"left outer join location on location._id = tree.location_id " +
				"left outer join tree_photo on tree._id = tree_id " +
				"left outer join photo on photo._id = photo_id where tree._id =" + treeIdStr;
		
		Cursor photoCursor = db.rawQuery(query, null);
		
		photoCursor.moveToFirst();
		
		
		Log.e("query", query);

		do {
			
			mCurrentPhotoPath = photoCursor.getString(photoCursor.getColumnIndex("name"));
			
			boolean isOutdated = photoCursor.getString(photoCursor.getColumnIndex("is_outdated")) == null 
					? false : photoCursor.getString(photoCursor.getColumnIndex("is_outdated")).equals("Y") ;

			
			TextView noImage = (TextView) v.findViewById(R.id.fragment_tree_preview_no_image);
			
			if (mCurrentPhotoPath != null && !isOutdated) {
				setPic();
				
				noImage.setVisibility(View.INVISIBLE);
			} else {
				noImage.setVisibility(View.VISIBLE);
			}
			
			MainActivity.mCurrentTreeLocation = new Location("treetracker");
			MainActivity.mCurrentTreeLocation.setLatitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("lat"))));
			MainActivity.mCurrentTreeLocation.setLongitude(Double.parseDouble(photoCursor.getString(photoCursor.getColumnIndex("long"))));

			// No GPS accuracy info from new api.
//			MainActivity.mCurrentTreeLocation.setAccuracy(Float.parseFloat(photoCursor.getString(photoCursor.getColumnIndex("accuracy"))));
			
			float[] results = {0,0,0};
			if (MainActivity.mCurrentLocation != null) {
				Location.distanceBetween(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude(),
						MainActivity.mCurrentTreeLocation.getLatitude(), MainActivity.mCurrentTreeLocation.getLongitude(), results);
			}
			
			TextView distanceTxt = (TextView) v.findViewById(R.id.fragment_tree_preview_distance);
			distanceTxt.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
			
			TextView accuracyTxt = (TextView) v.findViewById(R.id.fragment_tree_preview_gps_accuracy);
			accuracyTxt.setText(Integer.toString(Math.round(MainActivity.mCurrentTreeLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
			

			TextView createdTxt = (TextView) v.findViewById(R.id.fragment_tree_preview_created);
			createdTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_created")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_created")).lastIndexOf(":")));
			
			TextView updatedTxt = (TextView) v.findViewById(R.id.fragment_tree_preview_last_update);
			updatedTxt.setText(photoCursor.getString(photoCursor.getColumnIndex("time_updated")).substring(0, photoCursor.getString(photoCursor.getColumnIndex("time_updated")).lastIndexOf(":")));
			
			TextView statusTxt = (TextView) v.findViewById(R.id.fragment_tree_preview_image_status);
			
			Date dateForUpdate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				dateForUpdate = dateFormat.parse(photoCursor.getString(photoCursor.getColumnIndex("time_for_update")));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (dateForUpdate.before(new Date())) {
				statusTxt.setText(R.string.outdated);
			}		
			
			
			String noteQuery = "select tree_id, note.*, content as notetext from tree " +
					"left outer join tree_note on tree_id = tree._id " +
					"left outer join note on note_id = note._id " +
					"where content is not null and tree_id = " + treeIdStr + " order by note.time_created asc";
			
			Cursor noteCursor = db.rawQuery(noteQuery, null);
			
 
			TextView notesTxt = (TextView) v.findViewById(R.id.fragment_tree_preview_notes);
			
			notesTxt.setText(" ");
			
			while(noteCursor.moveToNext()) {
				String currentText = notesTxt.getText().toString();
				
				Log.e("tree_id", noteCursor.getString(noteCursor.getColumnIndex("tree_id")));
				Log.e("note", noteCursor.getString(noteCursor.getColumnIndex("notetext")));
				
				if (noteCursor.getString(noteCursor.getColumnIndex("notetext")).trim().length() == 0) {
					continue;
				}
				
				
				String text = noteCursor.getString(noteCursor.getColumnIndex("notetext")) + "\n\n" + currentText;

				notesTxt.setText(text);
				
			}
			

			
		} while (photoCursor.moveToNext());
		
		db.close();
  
	    return v;
	}

	public void onClick(View v) {
		
		
		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		
		switch (v.getId()) {
		case R.id.fragment_tree_preview_more:
			fragment = new NoteFragment();
			
			bundle = getActivity().getIntent().getExtras();
			
			if (bundle == null)
				bundle = new Bundle();
			
			bundle.putString(ValueHelper.TREE_ID, treeIdStr);
			fragment.setArguments(bundle);
			
			fragmentTransaction = getActivity().getSupportFragmentManager()
					.beginTransaction();
			fragmentTransaction.replace(R.id.container_fragment, fragment)
					.addToBackStack(ValueHelper.NOTE_FRAGMENT).commit();
			
			break;

		default:
			break;
		}
		
		
	}
	
	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		int targetH = mImageView.getHeight();

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
	


}
