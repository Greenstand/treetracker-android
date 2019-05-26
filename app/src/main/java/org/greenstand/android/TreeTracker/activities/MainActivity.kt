package org.greenstand.android.TreeTracker.activities


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.fragment_new_tree.*
import kotlinx.android.synthetic.main.fragment_tree_preview.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.api.models.responses.UserTree
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.fragments.*
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback,
    MapsFragment.LocationDialogListener {

    private var mSharedPreferences: SharedPreferences? = null

    private var fragment: Fragment? = null

    private var fragmentTransaction: FragmentTransaction? = null

    private var locationManager: LocationManager? = null
    private var mLocationListener: android.location.LocationListener? = null
    private var mLocationUpdatesStarted: Boolean = false

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle). **Note: Otherwise it is null.**
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSharedPreferences = this.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)


        if (mSharedPreferences!!.getBoolean(ValueHelper.FIRST_RUN, true)) {

            if (mSharedPreferences!!.getBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)) {
                mSharedPreferences?.edit()?.putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)?.apply()
            }

            mSharedPreferences?.edit()?.putBoolean(ValueHelper.FIRST_RUN, false)?.apply()
        }

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        val userIdentifier = mSharedPreferences?.getString(ValueHelper.PLANTER_IDENTIFIER, null)
        if (userIdentifier == null) {
            val editor = mSharedPreferences?.edit()
            editor?.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
            editor?.putString(ValueHelper.PLANTER_IDENTIFIER, null)
            editor?.putString(ValueHelper.PLANTER_PHOTO, null)
            editor?.apply()

            toolbarTitle.text = resources.getString(R.string.user_not_identified)

            fragment = LoginFragment()
            fragmentTransaction = supportFragmentManager
                .beginTransaction()
            fragmentTransaction?.replace(R.id.containerFragment, fragment as LoginFragment)
                ?.addToBackStack(ValueHelper.IDENTIFY_FRAGMENT)?.commit()

        } else if (mSharedPreferences!!.getBoolean(ValueHelper.TREES_TO_BE_DOWNLOADED_FIRST, false)) {
            Timber.d("TREES_TO_BE_DOWNLOADED_FIRST is true")
            var bundle = intent.extras

            fragment = MapsFragment()
            fragment?.arguments = bundle

            fragmentTransaction = supportFragmentManager
                    .beginTransaction()
            fragmentTransaction?.replace(R.id.containerFragment, fragment as MapsFragment)
                ?.addToBackStack(ValueHelper.MAP_FRAGMENT)?.commit()

            if (bundle == null)
                bundle = Bundle()

            bundle.putBoolean(ValueHelper.RUN_FROM_HOME_ON_LOGIN, true)


            fragment = DataFragment()
            fragment?.arguments = bundle

            fragmentTransaction = supportFragmentManager
                    .beginTransaction()
            fragmentTransaction?.replace(R.id.containerFragment, fragment as DataFragment)
                ?.addToBackStack(ValueHelper.DATA_FRAGMENT)?.commit()

        } else {
            if (userIdentifier != getString(R.string.user_not_identified)) {
                Timber.d("MainActivity" + " startDataSync is false")
                val homeFragment = MapsFragment()
                homeFragment.arguments = intent.extras

                val fragmentTransaction = supportFragmentManager
                        .beginTransaction()
                fragmentTransaction.replace(R.id.containerFragment, homeFragment).addToBackStack(ValueHelper.MAP_FRAGMENT)
                fragmentTransaction.commit()
            } else {
                val editor = mSharedPreferences?.edit()
                editor?.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                editor?.putString(ValueHelper.PLANTER_IDENTIFIER, null)
                editor?.putString(ValueHelper.PLANTER_PHOTO, null)
                editor?.commit()

                toolbarTitle.text = resources.getString(R.string.user_not_identified)


                fragment = LoginFragment()
                fragmentTransaction = supportFragmentManager
                        .beginTransaction()
                fragmentTransaction?.replace(R.id.containerFragment,
                        fragment as LoginFragment)?.addToBackStack(ValueHelper.IDENTIFY_FRAGMENT)?.commit()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val bundle: Bundle?
        val fm = supportFragmentManager
        when (item.itemId) {
            android.R.id.home -> {

                if (fm.backStackEntryCount > 0) {
                    fm.popBackStack()
                }
                return true
            }
            R.id.action_data -> {
                fragment = DataFragment()
                bundle = intent.extras
                fragment?.arguments = bundle

                fragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction?.replace(R.id.containerFragment, fragment as DataFragment)
                    ?.addToBackStack(ValueHelper.DATA_FRAGMENT)?.commit()
                for (entry in 0 until fm.backStackEntryCount) {
                    Timber.d("MainActivity " + "Found fragment: " + fm.getBackStackEntryAt(entry).name)
                }
                return true
            }
        /*
            case R.id.action_settings:
                fragment = new SettingsFragment();
                bundle = getIntent().getExtras();
                fragment.setArguments(bundle);

                fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.container_fragment, fragment)
                .addToBackStack(ValueHelper.SETTINGS_FRAGMENT).commit();
                for(int entry = 0; entry < fm.getBackStackEntryCount(); entry++){
                    Timber.d("MainActivity", "Found fragment: " + fm.getBackStackEntryAt(entry).getName());
                }
                return true;
                */
            R.id.action_about -> {
                val someFragment = supportFragmentManager.findFragmentById(R.id.containerFragment)

                var aboutIsRunning = false

                if (someFragment != null) {
                    if (someFragment is AboutFragment) {
                        aboutIsRunning = true
                    }
                }

                if (!aboutIsRunning) {
                    fragment = AboutFragment()
                    fragment?.arguments = intent.extras

                    fragmentTransaction = supportFragmentManager
                            .beginTransaction()
                    fragmentTransaction?.replace(R.id.containerFragment, fragment as AboutFragment)
                        ?.addToBackStack(ValueHelper.ABOUT_FRAGMENT)?.commit()
                }
                for (entry in 0 until fm.backStackEntryCount) {
                    Timber.d("MainActivity " + "Found fragment: " + fm.getBackStackEntryAt(entry).name)
                }
                return true
            }

            R.id.action_change_user -> {
                val editor = mSharedPreferences?.edit()
                editor?.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                editor?.putString(ValueHelper.PLANTER_IDENTIFIER, null)
                editor?.putString(ValueHelper.PLANTER_PHOTO, null)
                editor?.commit()

                toolbarTitle.text = resources.getString(R.string.user_not_identified)


                fragment = LoginFragment()
                fragmentTransaction = supportFragmentManager
                        .beginTransaction()
                fragmentTransaction?.replace(R.id.containerFragment, fragment as LoginFragment)
                    ?.addToBackStack(ValueHelper.IDENTIFY_FRAGMENT)?.commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onPause() {
        super.onPause()

        stopPeriodicUpdates()
    }

    public override fun onResume() {
        super.onResume()

        if(necessaryPermissionsGranted()) {
            requestNecessaryPermissions()
        } else {
            startPeriodicUpdates()
        }
    }

    private fun necessaryPermissionsGranted() : Boolean {
        return ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestNecessaryPermissions(){
        ActivityCompat.requestPermissions(this, arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION),
                Permissions.NECESSARY_PERMISSIONS)
    }

    fun onLocationChanged(location: Location) {
        // In the UI, set the latitude and longitude to the value received
        mCurrentLocation = location

        //int minAccuracy = mSharedPreferences.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, 0);
        val minAccuracy = 10

        if (fragmentMapGpsAccuracy != null) {
            if (mCurrentLocation != null) {
                if (mCurrentLocation!!.hasAccuracy() && mCurrentLocation!!.accuracy < minAccuracy) {
                    fragmentMapGpsAccuracy.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyValue?.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyValue?.text = Integer.toString(
                        Math.round(mCurrentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    MainActivity.mAllowNewTreeOrUpdate = true
                } else {
                    fragmentMapGpsAccuracy.setTextColor(Color.RED)
                    MainActivity.mAllowNewTreeOrUpdate = false

                    if (mCurrentLocation!!.hasAccuracy()) {
                        fragmentMapGpsAccuracyValue?.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyValue?.text = Integer.toString(
                            Math.round(mCurrentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    } else {
                        fragmentMapGpsAccuracyValue?.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyValue?.text = "N/A"
                    }
                }

                if (mCurrentLocation!!.hasAccuracy()) {
                    if (fragmentNewTreeGpsAccuracy != null) {
                        fragmentNewTreeGpsAccuracy?.text = Integer.toString(
                            Math.round(mCurrentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    }
                }
            } else {
                fragmentMapGpsAccuracy.setTextColor(Color.RED)
                fragmentMapGpsAccuracyValue?.setTextColor(Color.RED)
                fragmentMapGpsAccuracyValue?.text = "N/A"
                MainActivity.mAllowNewTreeOrUpdate = false
            }


            if (mCurrentTreeLocation != null && MainActivity.mCurrentLocation != null) {
                val results = floatArrayOf(0f, 0f, 0f)
                Location.distanceBetween(MainActivity.mCurrentLocation!!.latitude, MainActivity.mCurrentLocation!!.longitude,
                        MainActivity.mCurrentTreeLocation!!.latitude, MainActivity.mCurrentTreeLocation!!.longitude, results)

                if (fragmentNewTreeDistance != null) {
                    fragmentNewTreeDistance.text = Integer.toString(Math.round(results[0])) + " " + resources.getString(R.string.meters)
                }

                if (fragmentTreePreviewDistance != null) {
                    fragmentTreePreviewDistance.text = Integer.toString(Math.round(results[0])) + " " + resources.getString(R.string.meters)
                }
            }
        } else {
            Timber.d("fragmentMapGpsAccuracy NULL" );
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.size > 0) {
            if (requestCode == Permissions.NECESSARY_PERMISSIONS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPeriodicUpdates()
            }
        }
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    @SuppressLint("MissingPermission")
    private fun startPeriodicUpdates() {


        if (necessaryPermissionsGranted()) {
            Toast.makeText(this, "GPS Permissions Not Enabled", Toast.LENGTH_LONG).show()
            requestNecessaryPermissions()
            return
        }


        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // TODO this check may not longer be necessary
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle(R.string.enable_location_access)
            builder.setMessage(R.string.you_must_enable_location_access_in_your_settings_in_order_to_continue)

            builder.setPositiveButton(R.string.ok) { dialog, which ->
                if (Build.VERSION.SDK_INT >= 19) {
                    //LOCATION_MODE
                    //Sollution for problem 25 added the ability to pop up location start activity
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } else {
                    //LOCATION_PROVIDERS_ALLOWED

                    val locationProviders = Settings.Secure.getString(contentResolver,
                        Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
                    if (locationProviders == null || locationProviders == "") {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }


                dialog.dismiss()
            }


            builder.setNegativeButton(R.string.cancel) { dialog, which ->
                finish()

                dialog.dismiss()
            }


            val alert = builder.create()
            alert.setCancelable(false)
            alert.setCanceledOnTouchOutside(false)
            alert.show()

            return
        }

        if (mLocationUpdatesStarted) {
            return
        }

        mLocationListener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                this@MainActivity.onLocationChanged(location)
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        // Register the listener with Location Manager's network provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, mLocationListener)

        mLocationUpdatesStarted = true
    }


    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private fun stopPeriodicUpdates() {
        if (locationManager != null) {
            locationManager?.removeUpdates(mLocationListener)
            mLocationListener = null
        }
        mLocationUpdatesStarted = false

    }


    override fun refreshMap() {
        startPeriodicUpdates()
    }

    // TODO: implementing this as a static companion object is not necessarily a good design
    companion object {

        var mCurrentLocation: Location? = null
        var mCurrentTreeLocation: Location? = null

        var mAllowNewTreeOrUpdate = false

    }

}

