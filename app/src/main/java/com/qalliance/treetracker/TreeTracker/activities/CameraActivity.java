package com.qalliance.treetracker.TreeTracker.activities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.qalliance.treetracker.TreeTracker.camera.AlbumStorageDirFactory;
import com.qalliance.treetracker.TreeTracker.camera.BaseAlbumDirFactory;
import com.qalliance.treetracker.TreeTracker.camera.CameraPreview;
import com.qalliance.treetracker.TreeTracker.camera.FroyoAlbumDirFactory;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.Utils;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;


public class CameraActivity extends Activity implements PictureCallback, OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private Camera mCamera;
    private CameraPreview mPreview;
	private String TAG = "Camera activity";
	private PictureCallback mPicture;
	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;
	private String mCurrentPhotoPath;
	private ImageView mImageView;
	private View mParameters;
	private byte[] mCurrentPictureData;
	private ImageButton saveImg;
	private ImageButton cancelImg;
	private ImageButton captureButton;
	private File tmpImageFile;
	private ImageButton reloadButton;
	private AsyncTask<String, Void, String> openCameraTask;
	
    
    public static final int MEDIA_TYPE_IMAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
		}

        mImageView = (ImageView) findViewById(R.id.camera_preview_taken);
        
        saveImg = (ImageButton) findViewById(R.id.camera_preview_ok);
        cancelImg = (ImageButton) findViewById(R.id.camera_preview_cancel);
        captureButton = (ImageButton) findViewById(R.id.button_capture);
        reloadButton = (ImageButton) findViewById(R.id.camera_preview_reload);
        
        
        
        
     // Add a listener to the buttons
        captureButton.setOnClickListener(CameraActivity.this);
        cancelImg.setOnClickListener(CameraActivity.this);
        saveImg.setOnClickListener(CameraActivity.this);
        reloadButton.setOnClickListener(CameraActivity.this);
        
        openCameraTask = new OpenCameraTask().execute(new String[]{});
        
    }
    
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            Log.i("in use", e.getLocalizedMessage());
        }
        return c; // returns null if camera is unavailable
    }
    
    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

	public void onPictureTaken(byte[] data, Camera camera) {
		saveImg.setVisibility(View.VISIBLE);
		reloadButton.setVisibility(View.VISIBLE);
		captureButton.setVisibility(View.INVISIBLE);
		
		mCurrentPictureData = data;
		tmpImageFile = null;
		try {
			tmpImageFile = File.createTempFile("tmpimage.jpg", null, getCacheDir());
		} catch (IOException e) {
			Log.e("file not", "created");
			e.printStackTrace();
		}
		
		try {
            FileOutputStream fo = new FileOutputStream(tmpImageFile);
            fo.write(data);
            fo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setPic();
	}
	
	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(
				"android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		
		Bitmap photo = Utils.resizedImage(mCurrentPhotoPath);
		
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 70, bytes);

        File f = new File(mCurrentPhotoPath);
        try {
			f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            fo.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
//		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		sendBroadcast(mediaScanIntent);
	}
	
	/** Create a file Uri for saving an image or video */
	private static Uri getOutputMediaFileUri(int type){
	      return Uri.fromFile(getOutputMediaFile(type));
	}

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "MyCameraApp");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
	@Override
    protected void onPause() {
        super.onPause();
	    releaseCamera();              // release the camera immediately on pause event
    }
	
	private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
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
	
	private File setUpPhotoFile() throws IOException {

		File f = createImageFile();
		mCurrentPhotoPath = f.getAbsolutePath();

		return f;
	}

	private void setPic() {

		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(tmpImageFile.getAbsolutePath(), bmOptions);
		int imageWidth = bmOptions.outWidth;

		// Calculate your sampleSize based on the requiredWidth and
		// originalWidth
		// For e.g you want the width to stay consistent at 500dp
		int requiredWidth = (int) (500 * getResources().getDisplayMetrics().density);

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
		Bitmap bitmap = BitmapFactory.decodeFile(tmpImageFile.getAbsolutePath(), bmOptions);
				
		if (bitmap == null) {
			return;
		}


		ExifInterface exif = null;
		try {
			exif = new ExifInterface(tmpImageFile.getAbsolutePath());
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

		Matrix matrix = new Matrix();
		matrix.setRotate(rotationAngle, (float) bitmap.getWidth() / 2,
				(float) bitmap.getHeight() / 2);
		Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
				bmOptions.outWidth, bmOptions.outHeight, matrix, true);

		/* Associate the Bitmap to the ImageView */
		mImageView.setImageBitmap(rotatedBitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
	
	public void onOrientationChanged(int orientation) {
	  
	}

    public void onClick(View v) {
    	v.setHapticFeedbackEnabled(true);
    	
    	switch (v.getId()) {
		case R.id.button_capture:
	        // get an image from the camera
	        mCamera.takePicture(null, null, CameraActivity.this);
	        Log.e("take", "pic");
			break;
		case R.id.camera_preview_cancel:
			setResult(Activity.RESULT_CANCELED);
	        finish();
			break;
		case R.id.camera_preview_reload:
			mCamera.startPreview();
			
			FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			preview.removeAllViews();
	        preview.addView(mPreview);
			
			mImageView.setImageDrawable(null);
			reloadButton.setVisibility(View.INVISIBLE);
			captureButton.setVisibility(View.VISIBLE);
			saveImg.setVisibility(View.INVISIBLE);
			break;
		case R.id.camera_preview_ok:
			File pictureFile = null;
	        
			try {
				pictureFile = setUpPhotoFile();
				mCurrentPhotoPath = pictureFile.getAbsolutePath();
			} catch (IOException e) {
				e.printStackTrace();
				pictureFile = null;
				mCurrentPhotoPath = null;
			}

			boolean saved = true;
	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(mCurrentPictureData);
	            fos.close();
	            
	            galleryAddPic();
	        } catch (FileNotFoundException e) {
	            Log.d(TAG, "File not found: " + e.getMessage());
	            saved = false;
	        } catch (IOException e) {
	            Log.d(TAG, "Error accessing file: " + e.getMessage());
	            saved = false;
	        } catch (Exception e) {
	            Log.d(TAG, "Error accessing file: " + e.getMessage());
	            saved = false;
	        }
	        
	        
	        if (saved) {
		        Intent data = new Intent();
		        data.putExtra(ValueHelper.TAKEN_IMAGE_PATH, mCurrentPhotoPath);
		        setResult(Activity.RESULT_OK, data);
		        
	        } else {
	        	setResult(Activity.RESULT_CANCELED);
	        }
	        
	        finish();
	        
			break;
		default:
			break;
		}
    }

    
    class OpenCameraTask extends AsyncTask<String, Void, String> {
    	
    	protected void onPreExecute() {
	        cancelImg.setVisibility(View.INVISIBLE);
	        captureButton.setVisibility(View.INVISIBLE);
	        saveImg.setVisibility(View.INVISIBLE);
    	}

		@Override
		protected String doInBackground(String... params) {
	        mCamera = getCameraInstance();
	        

			return null;
		}
		
		
		 protected void onPostExecute(String response) {
		        super.onPostExecute(response);
		        
		        if (mCamera == null) {
		        	openCameraTask = new OpenCameraTask().execute(new String[]{});
		        } else {
			        // Create our Preview view and set it as the content of our activity.
			        mPreview = new CameraPreview(CameraActivity.this, mCamera);
			        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
			        preview.addView(mPreview);

			        
			        cancelImg.setVisibility(View.VISIBLE);
			        captureButton.setVisibility(View.VISIBLE);
		        }
			
		 }
		
		
	}


}



