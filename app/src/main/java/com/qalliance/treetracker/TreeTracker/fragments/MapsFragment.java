package com.qalliance.treetracker.TreeTracker.fragments;


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.qalliance.treetracker.TreeTracker.BuildConfig;
import com.qalliance.treetracker.TreeTracker.activities.MainActivity;
import com.qalliance.treetracker.TreeTracker.application.Permissions;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MapsFragment extends Fragment implements OnClickListener, OnMarkerClickListener, OnMapReadyCallback {
    private static final String TAG = "MapsFragment";

    public interface LocationDialogListener {
		void refreshMap();
	}

	LocationDialogListener mSettingCallback;

	private ArrayList<Marker> redPulsatingMarkers = new ArrayList<Marker>();
	private ArrayList<Marker> redToGreenPulsatingMarkers = new ArrayList<Marker>();

	private SharedPreferences mSharedPreferences;
	private int mCurrentRedToGreenMarkerColor = -1;
	private boolean paused = false;
	protected int mCurrentMarkerColor;
	private static View view;


	public MapsFragment() {
		//some overrides and settings go here
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);

		try {
			mSettingCallback = (LocationDialogListener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString()
					+ " must implement LocationDialogListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


	}

	@Override
	public void onPause() {
		super.onPause();

		paused = true;
		Log.d("GPS_Bugs", "MasFragment onPause");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("GPS_Bugs", "MasFragment on Destroy");
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("GPS_Bugs", "MasFragment onResume");
		if (paused) {
			((SupportMapFragment) getChildFragmentManager()
					.findFragmentById(R.id.map)).getMapAsync(this);
		}
		paused = false;

		mCurrentRedToGreenMarkerColor = R.drawable.green_pin;
		mCurrentMarkerColor = R.drawable.red_pin_pulsating_4;

		handler.post(new Runnable() {
			public void run() {
				if (mCurrentRedToGreenMarkerColor == R.drawable.red_pin) {
					mCurrentRedToGreenMarkerColor = R.drawable.green_pin;
				} else {
					mCurrentRedToGreenMarkerColor = R.drawable.red_pin;
				}
				for (Marker marker : redToGreenPulsatingMarkers) {
					marker.setIcon(BitmapDescriptorFactory.fromResource(mCurrentRedToGreenMarkerColor));
				}

				if (!paused)
					handler.postDelayed(this, 500);
			}
		});

		handler.post(new Runnable() {
			public void run() {
				if (mCurrentMarkerColor == R.drawable.red_pin) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_1;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_1) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_2;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_2) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_3;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_3) {
					mCurrentMarkerColor = R.drawable.red_pin_pulsating_4;
				} else if (mCurrentMarkerColor == R.drawable.red_pin_pulsating_4) {
					mCurrentMarkerColor = R.drawable.red_pin;
				}

				for (Marker marker : redPulsatingMarkers) {
					marker.setIcon(BitmapDescriptorFactory.fromResource(mCurrentMarkerColor));
				}

				if (!paused)
					handler.postDelayed(this, 200);
			}
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		try {
			SupportMapFragment fragment = (SupportMapFragment) getActivity()
					.getSupportFragmentManager().findFragmentById(
							R.id.map);
			if (fragment != null)
				getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();

		} catch (IllegalStateException e) {
			//handle this situation because you are necessary will get
			//an exception here :-(
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		if (view != null) {
			ViewGroup parent = (ViewGroup) view.getParent();
			if (parent != null)
				parent.removeView(view);
		}
		try {
			view = inflater.inflate(R.layout.fragment_map, container, false);
		} catch (InflateException e) {
	        /* map is already there, just return view as it is */
		}

		View v = view;

		mSharedPreferences = getActivity().getSharedPreferences(
				"com.qalliance.treetracker", Context.MODE_PRIVATE);

        if (!((AppCompatActivity) getActivity()).getSupportActionBar().isShowing()) {
            Log.d("MainActivity", "toolbar hide");
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
		((TextView) getActivity().findViewById(R.id.toolbar_title)).setText(R.string.map);
		((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) v.findViewById(R.id.fab);
        fab.setOnClickListener(this);

		((SupportMapFragment) getChildFragmentManager()
				.findFragmentById(R.id.map)).getMapAsync(this);


		TextView mapGpsAccuracy = ((TextView) v.findViewById(R.id.fragment_map_gps_accuracy));
		TextView mapGpsAccuracyValue = ((TextView) v.findViewById(R.id.fragment_map_gps_accuracy_value));

		int minAccuracy = mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING);

		if (mapGpsAccuracy != null) {
			Log.i("ođe", "0");
			if (MainActivity.mCurrentLocation != null) {
				Log.i("ođe", "1");
				if (MainActivity.mCurrentLocation.hasAccuracy() && (MainActivity.mCurrentLocation.getAccuracy() < minAccuracy)) {
					Log.i("ođe", "2");
					mapGpsAccuracy.setTextColor(Color.GREEN);
					mapGpsAccuracyValue.setTextColor(Color.GREEN);
					mapGpsAccuracyValue.setText(Integer.toString(Math.round(MainActivity.mCurrentLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
					MainActivity.mAllowNewTreeOrUpdate = true;
				} else {
					Log.i("ođe", "3");
					mapGpsAccuracy.setTextColor(Color.RED);
					MainActivity.mAllowNewTreeOrUpdate = false;

					if (MainActivity.mCurrentLocation.hasAccuracy()) {
						mapGpsAccuracyValue.setTextColor(Color.RED);
						mapGpsAccuracyValue.setText(Integer.toString(Math.round(MainActivity.mCurrentLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
					} else {
						mapGpsAccuracyValue.setTextColor(Color.RED);
						mapGpsAccuracyValue.setText("N/A");
					}
				}
			} else {
				Log.i("ođe", "5");
				if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
						ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					requestPermissions(
							new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
							Permissions.MY_PERMISSION_ACCESS_COURSE_LOCATION);
				}
				mapGpsAccuracy.setTextColor(Color.RED);
				mapGpsAccuracyValue.setTextColor(Color.RED);
				mapGpsAccuracyValue.setText("N/A");
				MainActivity.mAllowNewTreeOrUpdate = false;
			}

		}

		return v;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == Permissions.MY_PERMISSION_ACCESS_COURSE_LOCATION) {
			mSettingCallback.refreshMap();
		}
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

		}
	};

	private Fragment fragment;

	private Bundle bundle;

	private FragmentTransaction fragmentTransaction;


	public void onClick(View v) {


		v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

		Cursor photoCursor;
		switch (v.getId()) {
            case R.id.fab:
            	Log.d(TAG, "fab click");
                if (MainActivity.mAllowNewTreeOrUpdate || BuildConfig.GPS_ACCURACY.equals("off")) {
					fragment = new NewTreeFragment();
					bundle = getActivity().getIntent().getExtras();
					fragment.setArguments(bundle);

					fragmentTransaction = getActivity().getSupportFragmentManager()
							.beginTransaction();
					fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.NEW_TREE_FRAGMENT).commit();
				} else {
					Toast.makeText(getActivity(), "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show();
				}
				break;
//			case R.id.fragment_map_update_tree:
//
//				if (MainActivity.mAllowNewTreeOrUpdate) {
//					SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
//
////					String query = "select * from tree_photo " +
////							"left outer join tree on tree._id = tree_id " +
////							"left outer join photo on photo._id = photo_id " +
////							"left outer join location on location._id = photo.location_id " +
////							"where is_outdated = 'N'";
//
//					String query = "select * from tree " +
//							"left outer join location on location._id = tree.location_id " +
//							"left outer join tree_photo on tree._id = tree_id " +
//							"left outer join photo on photo._id = photo_id ";
//
//					Log.e("query", query);
//
//					photoCursor = db.rawQuery(query, null);
//
//					if (photoCursor.getCount() <= 0) {
//						Toast.makeText(getActivity(), "No trees to update", Toast.LENGTH_SHORT).show();
//						db.close();
//						return;
//					}
//
//					db.close();
//
//					fragment = new UpdateTreeFragment();
//					bundle = getActivity().getIntent().getExtras();
//					fragment.setArguments(bundle);
//
//					fragmentTransaction = getActivity().getSupportFragmentManager()
//							.beginTransaction();
//					fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.UPDATE_TREE_FRAGMENT).commit();
//				} else {
//					Toast.makeText(getActivity(), "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show();
//				}
//
//				break;
		}


	}

	public boolean onMarkerClick(Marker marker) {
		fragment = new TreePreviewFragment();
		bundle = getActivity().getIntent().getExtras();

		if (bundle == null)
			bundle = new Bundle();

		bundle.putString(ValueHelper.TREE_ID, marker.getTitle());
		fragment.setArguments(bundle);

		fragmentTransaction = getActivity().getSupportFragmentManager()
				.beginTransaction();
		fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.TREE_PREVIEW_FRAGMENT).commit();
		return true;
	}


	@Override
	public void onMapReady(GoogleMap map) {


		if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		map.setMyLocationEnabled(true);

		SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();

		Cursor treeCursor = db.rawQuery("select *, tree._id as tree_id from tree left outer join location on location_id = location._id where is_missing = 'N'", null);
		treeCursor.moveToFirst();

		redToGreenPulsatingMarkers.clear();
		redPulsatingMarkers.clear();

		if (treeCursor.getCount() > 0) {

			LatLng latLng = new LatLng(-33.867, 151.206);
			int bla = 0;

			do {


				Log.e("time_for_update", treeCursor.getString(treeCursor.getColumnIndex("_id")));

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


				Boolean isSynced = Boolean.parseBoolean(treeCursor.getString(treeCursor.getColumnIndex("is_synced")));

				Log.e("issynced", Boolean.toString(isSynced));

				Date dateForUpdate = new Date();
				try {
					dateForUpdate = dateFormat.parse(treeCursor.getString(treeCursor.getColumnIndex("time_for_update")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Date updated = new Date();
				try {
					updated = dateFormat.parse(treeCursor.getString(treeCursor.getColumnIndex("time_updated")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Date created = new Date();
				try {
					created = dateFormat.parse(treeCursor.getString(treeCursor.getColumnIndex("time_created")));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				boolean priority = treeCursor.getString(treeCursor.getColumnIndex("is_priority")).equals("Y");
				latLng = new LatLng(Double.parseDouble(treeCursor.getString(treeCursor.getColumnIndex("lat"))),
						Double.parseDouble(treeCursor.getString(treeCursor.getColumnIndex("long"))));


				MarkerOptions markerOptions = new MarkerOptions()
						.title(Long.toString(treeCursor.getLong(treeCursor.getColumnIndex("tree_id"))))// set Id instead of title
						.icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin))
						.position(latLng);
				Marker marker = map.addMarker(markerOptions);



				if (priority) {
					redPulsatingMarkers.add(marker);
					continue;
				}


				Log.i("updated", "*************");
				Log.i("updated", updated.toLocaleString());
				Log.i("dateForUpdate", dateForUpdate.toLocaleString());


				if (dateForUpdate.before(new Date())) {


					Log.e("updated", "should be red");

					Calendar calendar = Calendar.getInstance();
					calendar.setTime(dateForUpdate);

					Calendar currCalendar = Calendar.getInstance();
					currCalendar.setTime(new Date());

					marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_pin));
				}

//		        Log.i("updated", "*************");
//		        if (created.before(updated) && !isSynced) {
//		        	redToGreenPulsatingMarkers.add(marker);
//		        }
			} while (treeCursor.moveToNext());


			db.close();

			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

		} else {
			if (MainActivity.mCurrentLocation != null) {
				LatLng myLatLng = new LatLng(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude());
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10));
			}
		}

		map.setOnMarkerClickListener(MapsFragment.this);

		// Other supported types include: MAP_TYPE_NORMAL,
		// MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID and MAP_TYPE_NONE
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	}
}
