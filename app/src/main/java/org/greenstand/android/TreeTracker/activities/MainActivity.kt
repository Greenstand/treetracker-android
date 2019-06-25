package org.greenstand.android.TreeTracker.activities


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_new_tree.*
import kotlinx.android.synthetic.main.fragment_tree_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.analytics.Analytics
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.fragments.DataFragment
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.managers.UserManager
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.koin.android.ext.android.getKoin
import timber.log.Timber

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val userManager: UserManager = getKoin().get()

    private val analytics: Analytics = getKoin().get()

    private val userLocationManager: UserLocationManager = getKoin().get()

    private var sharedPreferences: SharedPreferences? = null

    private var fragment: Fragment? = null

    private var locationUpdateJob: Job? = null

    /**
     * Called when the activity is first created.
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most
     * recently supplied in onSaveInstanceState(Bundle). **Note: Otherwise it is null.**
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = this.getSharedPreferences("org.greenstand.android", Context.MODE_PRIVATE)


        if (sharedPreferences!!.getBoolean(ValueHelper.FIRST_RUN, true)) {

            if (sharedPreferences!!.getBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)) {
                sharedPreferences?.edit()?.putBoolean(ValueHelper.TREE_TRACKER_SETTINGS_USED, true)?.apply()
            }

            sharedPreferences?.edit()?.putBoolean(ValueHelper.FIRST_RUN, false)?.apply()
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        findViewById<View>(R.id.appbar_layout).visibility = View.GONE

        val navController = findNavController(R.id.nav_host_fragment)

        val listener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id != R.id.splashFragment2) {
                findViewById<View>(R.id.appbar_layout).visibility = View.VISIBLE
            }

            analytics.tagScreen(this, controller.currentDestination?.label.toString())
        }

        navController.addOnDestinationChangedListener(listener)

        toolbar.setNavigationOnClickListener {
            navController.popBackStack()
        }

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        if (!userManager.isLoggedIn) {
            userManager.clearUser()
            toolbarTitle.text = resources.getString(R.string.user_not_identified)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val bundle: Bundle?
        when (item.itemId) {
            R.id.action_data -> {
                fragment = DataFragment()
                bundle = intent.extras
                fragment?.arguments = bundle

                findNavController(R.id.nav_host_fragment).navigate(R.id.action_mapsFragment_to_dataFragment)
                return true
            }
            R.id.action_about -> {
                findNavController(R.id.nav_host_fragment).navigate(R.id.action_mapsFragment_to_aboutFragment)
                return true
            }

            R.id.action_change_user -> {
                userManager.clearUser()

                toolbarTitle.text = resources.getString(R.string.user_not_identified)

                findNavController(R.id.nav_host_fragment).navigate(R.id.action_global_login_flow_graph)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onPause() {
        super.onPause()

        userLocationManager.stopLocationUpdates()
        locationUpdateJob?.cancel()
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
        currentLocation = location

        val minAccuracy = 10

        val fragmentMapGpsAccuracyView : TextView? = findViewById(R.id.fragmentMapGpsAccuracy)
        val fragmentMapGpsAccuracyViewValue : TextView? = findViewById(R.id.fragmentMapGpsAccuracyValue)
        if (fragmentMapGpsAccuracyView != null) {
            if (currentLocation != null) {
                if (currentLocation!!.hasAccuracy() && currentLocation!!.accuracy < minAccuracy) {
                    fragmentMapGpsAccuracyView.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyViewValue?.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyViewValue?.text = Integer.toString(
                        Math.round(currentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    MainActivity.allowNewTreeOrUpdate = true
                } else {
                    fragmentMapGpsAccuracyView.setTextColor(Color.RED)
                    MainActivity.allowNewTreeOrUpdate = false

                    if (currentLocation!!.hasAccuracy()) {
                        fragmentMapGpsAccuracyViewValue?.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyViewValue?.text = Integer.toString(
                            Math.round(currentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    } else {
                        fragmentMapGpsAccuracyViewValue?.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyViewValue?.text = "N/A"
                    }
                }

                if (currentLocation!!.hasAccuracy()) {
                    if (fragmentNewTreeGpsAccuracy != null) {
                        fragmentNewTreeGpsAccuracy?.text = Integer.toString(
                            Math.round(currentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    }
                }
            } else {
                fragmentMapGpsAccuracyView.setTextColor(Color.RED)
                fragmentMapGpsAccuracyViewValue?.setTextColor(Color.RED)
                fragmentMapGpsAccuracyViewValue?.text = "N/A"
                MainActivity.allowNewTreeOrUpdate = false
            }


            if (currentTreeLocation != null && userLocationManager.currentLocation != null) {
                val results = floatArrayOf(0f, 0f, 0f)
                Location.distanceBetween(userLocationManager.currentLocation!!.latitude, userLocationManager.currentLocation!!.longitude,
                                         MainActivity.currentTreeLocation!!.latitude, MainActivity.currentTreeLocation!!.longitude, results)

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


        // TODO this check may not longer be necessary
        if (!userLocationManager.isLocationEnabled()) {
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

        // Register the listener with Location Manager's network provider
        locationUpdateJob = GlobalScope.launch(Dispatchers.Main) {
            for (location in userLocationManager.locationUpdatesChannel) {
                onLocationChanged(location)
            }
        }

        userLocationManager.startLocationUpdates()
    }

    // TODO: implementing this as a static companion object is not necessarily a good design
    companion object {
        var currentLocation: Location? = null
        var currentTreeLocation: Location? = null
        var allowNewTreeOrUpdate = false
    }
}

