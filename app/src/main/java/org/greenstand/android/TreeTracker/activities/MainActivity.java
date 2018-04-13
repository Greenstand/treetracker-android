package org.greenstand.android.TreeTracker.activities;


import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpStatus;
import org.greenstand.android.TreeTracker.R;
import org.greenstand.android.TreeTracker.api.DataManager;
import org.greenstand.android.TreeTracker.api.models.UserTree;
import org.greenstand.android.TreeTracker.application.Permissions;
import org.greenstand.android.TreeTracker.database.DatabaseManager;
import org.greenstand.android.TreeTracker.database.DbHelper;
import org.greenstand.android.TreeTracker.fragments.AboutFragment;
import org.greenstand.android.TreeTracker.fragments.DataFragment;
import org.greenstand.android.TreeTracker.fragments.ForgotPasswordFragment;
import org.greenstand.android.TreeTracker.fragments.LoginFragment;
import org.greenstand.android.TreeTracker.fragments.MapsFragment;
import org.greenstand.android.TreeTracker.fragments.SignupFragment;
import org.greenstand.android.TreeTracker.utilities.ValueHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        MapsFragment.LocationDialogListener {

    private static final String TAG = "MainActivity";

    public static final Handler mHandler = new Handler();

    public Map<String, String> map;

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

    private LocationManager locationManager;
    private android.location.LocationListener mLocationListener;
    private boolean mLocationUpdatesStarted;

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
                "org.greenstand.android", Context.MODE_PRIVATE);


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

        setContentView(R.layout.activity_main);

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
                MapsFragment homeFragment = new MapsFragment();
                homeFragment.setArguments(getIntent().getExtras());

                FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                        .beginTransaction();
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
        FragmentManager fm = getSupportFragmentManager();
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d("MainActivity", "press back button");

                Log.d("MainActivity", "click back, back stack count: " + fm.getBackStackEntryCount());
                for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                    Log.d("MainActivity", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
                }
                if (fm.getBackStackEntryCount() > 0) {
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
            /*
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
                */
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
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onRestart() {
        Log.i(TAG, "onRestart");
        super.onRestart();
    }

    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }


    /*
     * Called when the Activity is going into the background.
     * Parts of the UI may be visible, but the Activity is inactive.
     */
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        stopPeriodicUpdates();
    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setTitle(R.string.enable_location_access);
            builder.setMessage(R.string.you_must_enable_location_access_in_your_settings_in_order_to_continue);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {

                    if (Build.VERSION.SDK_INT >= 19) {
                        //LOCATION_MODE
                        //Sollution for problem 25 added the ability to pop up location start activity
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
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

        //solution for #57 git
        dbHelper = new DbHelper(this, "database", null, 1);
        mDatabaseManager = DatabaseManager.getInstance(MainActivity.dbHelper);

        try {
            dbHelper.createDataBase();
        } catch (IOException e) {
            Log.e("nije mi ", "uspjelo");
        }
        //end of solution for #57 git

        startPeriodicUpdates();

        if (mSharedPreferences.getBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false)) {

            Bundle bundle = getIntent().getExtras();

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


    public void onLocationChanged(Location location) {
        //Log.d("onLocationChanged", location.toString());

        // In the UI, set the latitude and longitude to the value received
        mCurrentLocation = location;

        //int minAccuracy = mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, 0);
        int minAccuracy = 10;

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
            }
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
            Log.e("MainActivity", "onSignupResult: failed to signup " + String.valueOf(httpResponseCode) );
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

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
	    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if(mLocationUpdatesStarted){
            return;
        }

        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location location) {
                MainActivity.this.onLocationChanged(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with Location Manager's network provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

        mLocationUpdatesStarted = true;
    }


    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        if(locationManager != null) {
            locationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
        }
        mLocationUpdatesStarted = false;

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

}

