package com.qalliance.treetracker.TreeTracker.activities;


import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.qalliance.treetracker.TreeTracker.database.DatabaseManager;
import com.qalliance.treetracker.TreeTracker.database.DbHelper;
import com.qalliance.treetracker.TreeTracker.utilities.LocationUtils;
import com.qalliance.treetracker.TreeTracker.network.NetworkUtilities;
import com.qalliance.treetracker.TreeTracker.application.Permissions;
import com.qalliance.treetracker.TreeTracker.R;
import com.qalliance.treetracker.TreeTracker.utilities.ValueHelper;
import com.qalliance.treetracker.TreeTracker.api.DataManager;
import com.qalliance.treetracker.TreeTracker.api.models.UserTree;
import com.qalliance.treetracker.TreeTracker.fragments.AboutFragment;
import com.qalliance.treetracker.TreeTracker.fragments.DataFragment;
import com.qalliance.treetracker.TreeTracker.fragments.ForgotPasswordFragment;
import com.qalliance.treetracker.TreeTracker.fragments.LoginFragment;
import com.qalliance.treetracker.TreeTracker.fragments.MapsFragment;
import com.qalliance.treetracker.TreeTracker.fragments.SettingsFragment;
import com.qalliance.treetracker.TreeTracker.fragments.SignupFragment;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity implements
        OnGlobalLayoutListener,
        LocationListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback,
        MapsFragment.LocationDialogListener {

    FrameLayout mSplashScreen;
    private GoogleApiClient mGoogleApiClient;

    public static final Handler mHandler = new Handler();

    public Map<String, String> map;

    private LocationRequest mLocationRequest;

    private boolean mUpdatesRequested;

    private SharedPreferences mSharedPreferences;

    private Fragment fragment;

    private FragmentTransaction fragmentTransaction;

    private AsyncTask<String, Void, String> getMyTreesTask;

    public static DbHelper dbHelper;

    public static Location mCurrentLocation;
    public static Location mCurrentTreeLocation;

    public static boolean syncDataFromExitScreen = false;

    public static boolean mAllowNewTreeOrUpdate = false;

    public static ProgressDialog progressDialog;


    private DataManager mDataManager;
    private DatabaseManager mDatabaseManager;
    private List<UserTree> mUserTreeList;

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after 
     * previously being shut down then this Bundle contains the data it most 
     * recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("on", "create");

        mSharedPreferences = this.getSharedPreferences(
                "com.qalliance.treetracker", Context.MODE_PRIVATE);


        dbHelper = new DbHelper(this, "database", null, 1);
        mDatabaseManager = DatabaseManager.getInstance(MainActivity.dbHelper);

        try {
            dbHelper.createDataBase();
        } catch (IOException e) {
            Log.e("nije mi ", "uspjelo");
        }


        if (mSharedPreferences.getBoolean(ValueHelper.FIRST_RUN, true)) {

            if (mSharedPreferences.getBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)) {
                mSharedPreferences.edit().putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true).commit();
            }

            mSharedPreferences.edit().putBoolean(ValueHelper.FIRST_RUN, false).commit();
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        servicesConnected();
        //TODO handle when not connected

//        requestWindowFeature(Window.FEATURE_ACTION_BAR);

        setContentView(R.layout.activity_main);

        this.findViewById(android.R.id.content).getRootView().getViewTreeObserver().addOnGlobalLayoutListener(this);

//        FrameLayout mDrawerLayout = (FrameLayout) findViewById(R.id.drawer_layout);
//        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
//        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        List<Map<String, String>> data = getMenuData();
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                R.layout.item, new String[]{"menu_item", "menu_item_id"},
                new int[]{R.id.menu_item, R.id.menu_item_id});

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are on by default
        mUpdatesRequested = true;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("");

        boolean showSignupFragment = mSharedPreferences.getBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false);
        boolean showLoginFragment = mSharedPreferences.getBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, true);

        Log.d("MainActivity", "showSignupFragment: " + showSignupFragment + ", showLoginFragment: " + showLoginFragment);

        if (showSignupFragment) {

            mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, true).commit();
            mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit();

            SignupFragment signupFragment = new SignupFragment();
            signupFragment.setArguments(getIntent().getExtras());

            FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction();
            fragmentTransaction.add(R.id.container_fragment, signupFragment, ValueHelper.SIGNUP_FRAGMENT);

            fragmentTransaction.commit();
        } else if (showLoginFragment) {

            mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit();
            mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, true).commit();

            LoginFragment loginFragment = new LoginFragment();
            loginFragment.setArguments(getIntent().getExtras());

            FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction();
            fragmentTransaction.add(R.id.container_fragment, loginFragment, ValueHelper.LOGIN_FRAGMENT);

            fragmentTransaction.commit();
        } else {
            Bundle extras = getIntent().getExtras();
            boolean startDataSync = false;
            if (extras != null) {
                if (extras.getBoolean(ValueHelper.RUN_FROM_NOTIFICATION_SYNC)) {
                    startDataSync = true;
                }
            }

            if (startDataSync) {
                Log.d("MainActivity", "startDataSync is true");
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(ValueHelper.WIFI_NOTIFICATION_ID);

                DataFragment dataFragment = new DataFragment();
                dataFragment.setArguments(getIntent().getExtras());

                FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                        .beginTransaction();
                fragmentTransaction.add(R.id.container_fragment, dataFragment, ValueHelper.DATA_FRAGMENT);

                fragmentTransaction.commit();

            } else if (mSharedPreferences.getBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false)) {
                Log.d("MainActivity", "TREES_TO_BE_DOWNLOADED_FIRST is true");
                Bundle bundle = getIntent().getExtras();


//                fragment = new HomeFragment();
                fragment = new MapsFragment();
                fragment.setArguments(bundle);

                fragmentTransaction = getSupportFragmentManager()
                        .beginTransaction();
                fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.MAP_FRAGMENT).commit();

                if (bundle == null)
                    bundle = new Bundle();

                bundle.putBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN, true);


                fragment = new DataFragment();
                fragment.setArguments(bundle);

                fragmentTransaction = getSupportFragmentManager()
                        .beginTransaction();
                fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();

            } else {
                Log.d("MainActivity", "startDataSync is false");
//                HomeFragment homeFragment = new HomeFragment();
                MapsFragment homeFragment = new MapsFragment();
                homeFragment.setArguments(getIntent().getExtras());

                FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                        .beginTransaction();
//                fragmentTransaction.add(R.id.container_fragment, homeFragment, ValueHelper.HOME_FRAGMENT);
                fragmentTransaction.replace(R.id.container_fragment, homeFragment).addToBackStack(ValueHelper.MAP_FRAGMENT);
                fragmentTransaction.commit();
            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        Log.d("MainActivity", "menu_main created");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bundle;
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container_fragment);
        Log.d("MainActivity", currentFragment.toString());
        FragmentManager fm = getSupportFragmentManager();
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d("MainActivity", "press back button");

                Log.d("MainActivity", "click back, back stack count: " + fm.getBackStackEntryCount());
                for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                    Log.d("MainActivity", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
                }
                if (fm.getBackStackEntryCount() > 1) {
                    fm.popBackStack();
                }
                return true;
            case R.id.action_data:
                fragment = new DataFragment();
                bundle = getIntent().getExtras();
                fragment.setArguments(bundle);

                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();
                for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                    Log.d("MainActivity", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
                }
                return true;
            case R.id.action_settings:
                fragment = new SettingsFragment();
                bundle = getIntent().getExtras();
                fragment.setArguments(bundle);

                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.SETTINGS_FRAGMENT).commit();
                for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                    Log.d("MainActivity", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
                }
                return true;
            case R.id.action_about:
                Fragment someFragment = getSupportFragmentManager().findFragmentById(R.id.container_fragment);

                boolean aboutIsRunning = false;

                if (someFragment != null) {
                    if (someFragment instanceof AboutFragment) {
                        aboutIsRunning = true;
                    }
                }

                if (!aboutIsRunning) {
                    fragment = new AboutFragment();
                    fragment.setArguments(getIntent().getExtras());

                    fragmentTransaction = getSupportFragmentManager()
                            .beginTransaction();
                    fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.ABOUT_FRAGMENT).commit();
                }
                for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                    Log.d("MainActivity", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
                }
                return true;
            case R.id.action_exit:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.exit);
                builder.setMessage(R.string.do_you_want_to_sync_your_data_now);

                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        MainActivity.syncDataFromExitScreen = true;

                        fragment = new DataFragment();
                        fragment.setArguments(getIntent().getExtras());

                        fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();

                        dialog.dismiss();
                    }

                });


                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        // Code that is executed when clicking NO

                        finish();

                        dialog.dismiss();
                    }

                });


                AlertDialog alert = builder.create();
                alert.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Map<String, String>> getMenuData() {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();

        Map<String, String> map = new HashMap<String, String>();

        map = new HashMap<String, String>();
        map.put("menu_item", getResources().getString(R.string.home));
        map.put("menu_item_id", ValueHelper.MENU_HOME);
        list.add(map);

        map = new HashMap<String, String>();
        map.put("menu_item", getResources().getString(R.string.map));
        map.put("menu_item_id", ValueHelper.MENU_MAP);
        list.add(map);

        map = new HashMap<String, String>();
        map.put("menu_item", getResources().getString(R.string.data));
        map.put("menu_item_id", ValueHelper.MENU_DATA);
        list.add(map);

        map = new HashMap<String, String>();
        map.put("menu_item", getResources().getString(R.string.settings));
        map.put("menu_item_id", ValueHelper.MENU_SETTINGS);
        list.add(map);

        map = new HashMap<String, String>();
        map.put("menu_item", getResources().getString(R.string.exit));
        map.put("menu_item_id", ValueHelper.MENU_EXIT);
        list.add(map);

        return list;
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
//        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        mDrawerToggle.onConfigurationChanged(newConfig);
    }


    public void onGlobalLayout() {

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e("u mainu sam", "OK");
        super.onActivityResult(requestCode, resultCode, data);
    }


    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("LocationUpdates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            Log.e("LocationUpdates",
                    "Google Play services is not available.");

        }
        return false;
    }


    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        ValueHelper.CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        }

    }


    public void onConnected(Bundle bundle) {
        MainActivity.this.getLocation();

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onRestart() {


        Log.i("on restart", "restart");

        super.onRestart();

    }

    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {

        super.onStop();

        Log.e("ON", "stooooop");


    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e("ON", "DESTROY");

    }


    /*
     * Called when the Activity is going into the background.
     * Parts of the UI may be visible, but the Activity is inactive.
     */
    @Override
    public void onPause() {

        super.onPause();


        if (isFinishing()) {
            Log.e("zelim izac", "gotov");
        } else {
            Log.e("stani, nisam gotov", "gotov");
        }
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        Log.i("uso", "on start");

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
    }

    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    public void onResume() {
        super.onResume();

        Log.i("uso", "u resume");

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setTitle(R.string.enable_location_access);
            builder.setMessage(R.string.you_must_enable_location_access_in_your_settings_in_order_to_continue);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    if (Build.VERSION.SDK_INT >= 19) {
                        //LOCATION_MODE
                    } else {
                        //LOCATION_PROVIDERS_ALLOWED

                        String locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                        if (locationProviders == null || locationProviders.equals("")) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    }


                    dialog.dismiss();
                }

            });


            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    finish();

                    dialog.dismiss();
                }

            });


            AlertDialog alert = builder.create();
            alert.setCancelable(false);
            alert.setCanceledOnTouchOutside(false);
            alert.show();

        }


//        if (mUpdatesRequested && mLocationClient.isConnected()) {
//
//            startPeriodicUpdates();
//        }

        if (mSharedPreferences.getBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false)) {

            Bundle bundle = getIntent().getExtras();

//            fragment = new HomeFragment();
            fragment = new MapsFragment();
            fragment.setArguments(bundle);

            fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction();
            fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.MAP_FRAGMENT).commit();

            if (bundle == null)
                bundle = new Bundle();

            bundle.putBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN, true);


            fragment = new DataFragment();
            fragment.setArguments(bundle);

            fragmentTransaction = getSupportFragmentManager()
                    .beginTransaction();
            fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();

        }

    }


    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        //mLocationClient.removeLocationUpdates(this);
    }


    public void onLocationChanged(Location location) {
        // In the UI, set the latitude and longitude to the value received
        mCurrentLocation = location;

        int minAccuracy = mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, 0);

        TextView mapGpsAccuracy = ((TextView) findViewById(R.id.fragment_map_gps_accuracy));
        TextView mapGpsAccuracyValue = ((TextView) findViewById(R.id.fragment_map_gps_accuracy_value));


        if (mapGpsAccuracy != null) {
            if (mCurrentLocation != null) {
                if (mCurrentLocation.hasAccuracy() && (mCurrentLocation.getAccuracy() < minAccuracy)) {
                    mapGpsAccuracy.setTextColor(Color.GREEN);
                    mapGpsAccuracyValue.setTextColor(Color.GREEN);
                    mapGpsAccuracyValue.setText(Integer.toString(Math.round(mCurrentLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
                    MainActivity.mAllowNewTreeOrUpdate = true;
                } else {
                    mapGpsAccuracy.setTextColor(Color.RED);
                    MainActivity.mAllowNewTreeOrUpdate = false;

                    if (mCurrentLocation.hasAccuracy()) {
                        mapGpsAccuracyValue.setTextColor(Color.RED);
                        mapGpsAccuracyValue.setText(Integer.toString(Math.round(mCurrentLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
                    } else {
                        mapGpsAccuracyValue.setTextColor(Color.RED);
                        mapGpsAccuracyValue.setText("N/A");
                    }
                }

                if (mCurrentLocation.hasAccuracy()) {
                    TextView newTreeGpsAccuracy = (TextView) findViewById(R.id.fragment_new_tree_gps_accuracy);

                    if (newTreeGpsAccuracy != null) {
                        newTreeGpsAccuracy.setText(Integer.toString(Math.round(mCurrentLocation.getAccuracy())) + " " + getResources().getString(R.string.meters));
                    }
                }
            } else {
                mapGpsAccuracy.setTextColor(Color.RED);
                mapGpsAccuracyValue.setTextColor(Color.RED);
                mapGpsAccuracyValue.setText("N/A");
                MainActivity.mAllowNewTreeOrUpdate = false;
            }

            // For Debugging
            // MainActivity.mAllowNewTreeOrUpdate = true;


            if (mCurrentTreeLocation != null && MainActivity.mCurrentLocation != null) {
                float[] results = {0, 0, 0};
                Location.distanceBetween(MainActivity.mCurrentLocation.getLatitude(), MainActivity.mCurrentLocation.getLongitude(),
                        MainActivity.mCurrentTreeLocation.getLatitude(), MainActivity.mCurrentTreeLocation.getLongitude(), results);

                TextView newTreeDistance = (TextView) findViewById(R.id.fragment_new_tree_distance);
                if (newTreeDistance != null) {
                    newTreeDistance.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
                }

                TextView treePreviewDistance = (TextView) findViewById(R.id.fragment_tree_preview_distance);
                if (treePreviewDistance != null) {
                    treePreviewDistance.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
                }

                TextView updateTreeDistance = (TextView) findViewById(R.id.fragment_update_tree_distance);
                if (updateTreeDistance != null) {
                    updateTreeDistance.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
                }

                TextView updateTreeDetailsDistance = (TextView) findViewById(R.id.fragment_update_tree_details_distance);
                if (updateTreeDetailsDistance != null) {
                    updateTreeDetailsDistance.setText(Integer.toString(Math.round(results[0])) + " " + getResources().getString(R.string.meters));
                }


//				Toast.makeText(MainActivity.this, "distance  " + Float.toString(results[0]), Toast.LENGTH_LONG).show();

            }
        }

    }

    /**
     *
     * Calls getLastLocation() to get the current location
     *
     */
    public void getLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
//            mCurrentLocation = mLocationClient.getLastLocation();
//
//            onLocationChanged(mLocationClient.getLastLocation());

            if (mCurrentLocation != null) {
                Log.e("latlong ", Double.toString(mCurrentLocation.getLatitude()) + " " + Double.toString(mCurrentLocation.getLongitude()));
            }

        } else {
            //mLocationClient.connect();
        }
    }


    /**
     * Called when the authentication process completes (see attemptLogin()).
     */
    public void onAuthenticationResult(boolean result) {
        Log.i("MainActivity", "onAuthenticationResult(" + result + ")");
        // Hide the progress dialog

        if (result) {
        } else {
            Log.e("MainActivity", "onAuthenticationResult: failed to authenticate");
        }
    }


    /**
     * Called when the signup process completes
     * @param httpResponseCode
     */
    public void onSignupResult(boolean result, int httpResponseCode, String responseBody) {
        Log.i("MainActivity", "onSignupResult(" + result + ")");
        Log.i("MainActivity", "httpResponseCode(" + Integer.toString(httpResponseCode) + ")");
        // Hide the progress dialog

        if (MainActivity.progressDialog != null) {
            MainActivity.progressDialog.dismiss();
        }

        if (result) {

            JSONObject jsonReponse;
            switch (httpResponseCode) {
                case HttpStatus.SC_OK:
                    //successfull signup, save the token and continue


                    try {
                        jsonReponse = new JSONObject(responseBody);

                        mSharedPreferences.edit().putString(ValueHelper.TOKEN, jsonReponse.getString("token")).commit();
                        //Below code unsets the login and signup fragments because user successfully loged in


                        SQLiteDatabase db = dbHelper.getWritableDatabase();


                        ContentValues values = new ContentValues();
                        values.put("main_db_id", jsonReponse.getString("id"));
                        values.put("first_name", jsonReponse.getString("first_name"));
                        values.put("last_name", jsonReponse.getString("last_name"));
                        values.put("email", jsonReponse.getString("email"));
                        values.put("is_main_user", "Y");
                        values.put("organization", jsonReponse.getString("organization"));

                        long userId = db.insert("users", null, values);

                        mSharedPreferences.edit().putLong(ValueHelper.MAIN_USER_ID, userId).commit();
                        mSharedPreferences.edit().putString(ValueHelper.MAIN_DB_USER_ID, jsonReponse.getString("id")).commit();
                        mSharedPreferences.edit().putString(ValueHelper.MAIN_USER_FIRST_NAME, jsonReponse.getString("first_name")).commit();
                        mSharedPreferences.edit().putString(ValueHelper.MAIN_USER_LAST_NAME, jsonReponse.getString("last_name")).commit();

                        mSharedPreferences.edit().putInt(ValueHelper.MAIN_DB_NEXT_UPDATE, Integer.parseInt(jsonReponse.getString("next_update"))).commit();
                        mSharedPreferences.edit().putInt(ValueHelper.MAIN_DB_MIN_ACCURACY, Integer.parseInt(jsonReponse.getString("min_gps_accuracy"))).commit();

                        mSharedPreferences.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, Integer.parseInt(jsonReponse.getString("next_update"))).commit();
                        mSharedPreferences.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, Integer.parseInt(jsonReponse.getString("min_gps_accuracy"))).commit();

                        db.close();

                        mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit();
                        mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit();

//                        fragment = new HomeFragment();
                        fragment = new MapsFragment();
                        fragment.setArguments(getIntent().getExtras());

                        fragmentTransaction = getSupportFragmentManager()
                                .beginTransaction();
                        fragmentTransaction.replace(R.id.container_fragment, fragment).commit();


                        Toast.makeText(MainActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();

                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;


                default:
                    break;
            }

        } else {
            Log.e("MainActivity", "onSignupResult: failed to signup");
            switch (httpResponseCode) {
                case -1:
                    Toast.makeText(MainActivity.this, "Please check your internet connection and try again.", Toast.LENGTH_SHORT).show();
                    break;

                case HttpStatus.SC_CONFLICT:
                    Log.e("conflict", "alert should display");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle(R.string.user_already_exists);
                    builder.setMessage(R.string.user_with_that_email);

                    builder.setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            fragment = new LoginFragment();
                            fragment.setArguments(getIntent().getExtras());

                            fragmentTransaction = getSupportFragmentManager()
                                    .beginTransaction();
                            fragmentTransaction.replace(R.id.container_fragment, fragment).commit();

                            dialog.dismiss();
                        }

                    });


                    builder.setNegativeButton(R.string.reset_password, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            fragment = new ForgotPasswordFragment();
                            fragment.setArguments(getIntent().getExtras());

                            fragmentTransaction = getSupportFragmentManager()
                                    .beginTransaction();
                            fragmentTransaction.replace(R.id.container_fragment, fragment)
                                    .addToBackStack(ValueHelper.FORGOT_PASSWORD_FRAGMENT).commit();


                            dialog.dismiss();
                        }

                    });


                    AlertDialog alert = builder.create();
                    alert.show();
                    break;


                default:
                    break;
            }
        }
    }


    /**
     * Called when the signup process completes
     * @param httpResponseCode
     */
    public void onLoginResult(boolean result, int httpResponseCode, String responseBody) {
        Log.i("MainActivity", "onLoginResult(" + result + ")");
        Log.i("MainActivity", "httpResponseCode(" + Integer.toString(httpResponseCode) + ")");
        // Hide the progress dialog


        if (MainActivity.progressDialog != null) {
            MainActivity.progressDialog.dismiss();
        }


        if (result) {

            JSONObject jsonReponse;
            switch (httpResponseCode) {
                case HttpStatus.SC_OK:
                    //successfull login, save the token and continue

                    try {
                        jsonReponse = new JSONObject(responseBody);

                        mSharedPreferences.edit().putString(ValueHelper.TOKEN, jsonReponse.getString("token")).commit();
                        //Below code unsets the login and signup fragments because user successfully loged in


                        SQLiteDatabase db = dbHelper.getWritableDatabase();


                        ContentValues values = new ContentValues();
                        values.put("main_db_id", jsonReponse.getString("id"));
                        values.put("first_name", jsonReponse.getString("first_name"));
                        values.put("last_name", jsonReponse.getString("last_name"));
                        values.put("email", jsonReponse.getString("email"));
                        values.put("is_main_user", "Y");
                        values.put("organization", jsonReponse.getString("organization"));

                        long userId = db.insert("users", null, values);
                        Log.d("MainActivity", "onLoginResult userId: " + userId);

                        mSharedPreferences.edit().putLong(ValueHelper.MAIN_USER_ID, userId).commit();
                        mSharedPreferences.edit().putString(ValueHelper.MAIN_DB_USER_ID, jsonReponse.getString("id")).commit();
                        mSharedPreferences.edit().putString(ValueHelper.MAIN_USER_FIRST_NAME, jsonReponse.getString("first_name")).commit();
                        mSharedPreferences.edit().putString(ValueHelper.MAIN_USER_LAST_NAME, jsonReponse.getString("last_name")).commit();

                        mSharedPreferences.edit().putInt(ValueHelper.MAIN_DB_NEXT_UPDATE, Integer.parseInt(jsonReponse.getString("next_update"))).commit();
                        mSharedPreferences.edit().putInt(ValueHelper.MAIN_DB_MIN_ACCURACY, Integer.parseInt(jsonReponse.getString("min_gps_accuracy"))).commit();

                        mSharedPreferences.edit().putInt(ValueHelper.TIME_TO_NEXT_UPDATE_GLOBAL_SETTING, Integer.parseInt(jsonReponse.getString("next_update"))).commit();
                        mSharedPreferences.edit().putInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, Integer.parseInt(jsonReponse.getString("min_gps_accuracy"))).commit();


                        db.close();

                        mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit();
                        mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit();

//                        HomeFragment homeFragment = new HomeFragment();
                        MapsFragment homeFragment = new MapsFragment();
                        homeFragment.setArguments(getIntent().getExtras());

                        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                                .beginTransaction();

                        fragmentTransaction.replace(R.id.container_fragment, homeFragment).addToBackStack(ValueHelper.MAP_FRAGMENT);

                        fragmentTransaction.commit();

                        if (
                                (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                            ||  (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                            ||  (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)

                            ) {
                            ActivityCompat.requestPermissions(this, new String[]{
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                            android.Manifest.permission.CAMERA,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                                    Permissions.NECESSARY_PERMISSIONS);
                        } else {
                            startPeriodicUpdates();
//                            getMyTreesTask = new GetMyTreesTask().execute(new String[]{});
                            getMyTrees();

                        }

                        Log.i("MainActivity", "token(" + mSharedPreferences.getString(ValueHelper.TOKEN, "") + ")");
//						Below code unsets the login and signup fragments because user successfully loged in 
                        mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_SIGNUP_FRAGMENT, false).commit();
                        mSharedPreferences.edit().putBoolean(ValueHelper.SHOW_LOGIN_FRAGMENT, false).commit();
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    break;


                default:
                    break;
            }

        } else {
            Log.e("MainActivity", "onLoginResult: failed to login");
            switch (httpResponseCode) {
                case HttpStatus.SC_UNAUTHORIZED:
                    Toast.makeText(MainActivity.this, "Incorrect username or password.", Toast.LENGTH_SHORT).show();
                    break;

                case -1:
                    Toast.makeText(MainActivity.this, "Please check your internet connection and try again.", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            if (requestCode == Permissions.NECESSARY_PERMISSIONS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPeriodicUpdates();
//                getMyTreesTask = new GetMyTreesTask().execute(new String[]{});
                getMyTrees();
            }
        }
    }

    /**
     * Called when the tree sync process completes (for one tree)
     * @param httpResponseCode
     */
    public void onTreeSyncResult(boolean result, int httpResponseCode, String responseBody) {
        Log.i("MainActivity", "onTreeSyncedResult(" + result + ")");
        Log.i("MainActivity", "httpResponseCode(" + Integer.toString(httpResponseCode) + ")");
        // Hide the progress dialog

        if (result) {

            JSONObject jsonReponse;
            switch (httpResponseCode) {
                case HttpStatus.SC_OK:
                    //successfull sync, save the token and continue

                    try {
                        jsonReponse = new JSONObject(responseBody);

                        Log.e("response body", jsonReponse.toString());

                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                        Log.e("response body", responseBody);
                    }

                    break;


                default:
                    break;
            }

        } else {
            Log.e("MainActivity", "onLoginResult: failed to login");
            switch (httpResponseCode) {
                case HttpStatus.SC_UNAUTHORIZED:
                    Toast.makeText(MainActivity.this, "Incorrect username or password.", Toast.LENGTH_SHORT).show();
                    break;

                case -1000:
                    Toast.makeText(MainActivity.this, "Please check your internet connection and try again.", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void refreshMap() {
        startPeriodicUpdates();
    }

    private void getMyTrees() {

        mDataManager = new DataManager<List<UserTree>>() {
            @Override
            public void onDataLoaded(List<UserTree> data) {
                mUserTreeList = data;
                mDatabaseManager.openDatabase();
                long userId = mSharedPreferences.getLong(ValueHelper.MAIN_USER_ID, -1);
                for (UserTree userTree : data) {
                    ContentValues values = new ContentValues();
                    values.put("tree_id", userTree.getId());
                    values.put("user_id", Long.toString(userId));
                    mDatabaseManager.insert("pending_updates", null, values);
                }
                mDatabaseManager.closeDatabase();

                if (data.size() > 0) {
                    Log.d("MainActivity", "GetMyTreesTask onPostExecute jsonReponseArray.length() > 0");
                    mSharedPreferences.edit().putBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, true).commit();

                    Bundle bundle = getIntent().getExtras();

                    if (bundle == null)
                        bundle = new Bundle();

                    bundle.putBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN, true);

                    fragment = new DataFragment();
                    fragment.setArguments(bundle);

                    fragmentTransaction = getSupportFragmentManager()
                            .beginTransaction();
                    fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();
                    Log.d("MainActivity", "click back, back stack count: " + getSupportFragmentManager().getBackStackEntryCount());
                    for (int entry = 0; entry < getSupportFragmentManager().getBackStackEntryCount(); entry++) {
                        Log.d("MainActivity", "Found fragment: " + getSupportFragmentManager().getBackStackEntryAt(entry).getName());
                    }
                }
            }

            @Override
            public void onRequestFailed(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };
        long userId = Long.parseLong(mSharedPreferences.getString(ValueHelper.MAIN_DB_USER_ID, "-1"));
        Log.d("MainActivity", "getMyTrees userId: " + userId);
        mDataManager.loadUserTrees(userId);
    }

    public List<UserTree> getUserTrees() {
        return mUserTreeList;
    }

    /**
     * Deprecated. Replaced by API trees/details/user/{id}
     */
    class GetMyTreesTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

	        HttpResponse resp = null;
	        
			String rsp = null;
	        
	        HttpGet post = null;
			post = new HttpGet(NetworkUtilities.TREE_FOR_USER_URI + mSharedPreferences.getString(ValueHelper.MAIN_DB_USER_ID, "-1")
					+ "?token=" + mSharedPreferences.getString(ValueHelper.TOKEN, ""));
			
			Log.d("URL", NetworkUtilities.TREE_FOR_USER_URI + mSharedPreferences.getString(ValueHelper.MAIN_DB_USER_ID, "-1") 
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
                    Log.d("MainActivity", "GetMyTreesTask onPostExecute response != null");
                    Log.d("MainActivity", "response: " + response);

                    try {
						jsonReponseArray = new JSONArray(response);
						
						for (int i = 0; i < jsonReponseArray.length(); i++) {
				  			SQLiteDatabase db = MainActivity.dbHelper.getWritableDatabase();
				  			
				  			 
				  		    ContentValues values = new ContentValues();
				  		    values.put("tree_id", (String) jsonReponseArray.get(i));
				  		    values.put("user_id", Long.toString(mSharedPreferences.getLong(ValueHelper.MAIN_USER_ID, -1)));
							
							
							db.insert("pending_updates", null, values);
							db.close();
						}
						
						if (jsonReponseArray.length() > 0) {
                            Log.d("MainActivity", "GetMyTreesTask onPostExecute jsonReponseArray.length() > 0");
							mSharedPreferences.edit().putBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, true).commit();
							
							
							Bundle bundle = getIntent().getExtras();
							
							if (bundle == null)
								bundle = new Bundle();
							
							bundle.putBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN, true);

							fragment = new DataFragment();
							fragment.setArguments(bundle);

							fragmentTransaction = getSupportFragmentManager()
									.beginTransaction();
							fragmentTransaction.replace(R.id.container_fragment, fragment).addToBackStack(ValueHelper.DATA_FRAGMENT).commit();
                            Log.d("MainActivity", "click back, back stack count: " + getSupportFragmentManager().getBackStackEntryCount());
                            for(int entry = 0; entry < getSupportFragmentManager().getBackStackEntryCount(); entry++){
                                Log.d("MainActivity", "Found fragment: " + getSupportFragmentManager().getBackStackEntryAt(entry).getName());
                            }
						}

					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

		  			
		        } else {
	        		
		        }
		 }
		
		
	}

}

