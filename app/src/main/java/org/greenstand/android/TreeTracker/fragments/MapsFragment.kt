package org.greenstand.android.TreeTracker.fragments


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.*
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.greenstand.android.TreeTracker.viewmodels.MapViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber


class MapsFragment : androidx.fragment.app.Fragment(), OnClickListener, OnMarkerClickListener, OnMapReadyCallback,
    View.OnLongClickListener {

    private val vm: MapViewModel by viewModel()

    private val userLocationManager: UserLocationManager by inject()

    private var mSharedPreferences: SharedPreferences? = null

    private var mapFragment: SupportMapFragment? = null
    private var map: GoogleMap? = null

    private val database: AppDatabase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.checkInStatusLiveData.observe(this, Observer<Boolean> { planterIsCheckedIn ->
            if (planterIsCheckedIn) {
                activity!!.toolbarTitle.text = "User Logged In"

                GlobalScope.launch(Dispatchers.Main) {
//                    val planterInfo = withContext(Dispatchers.IO) { dao.getPlanterInfoById(planterInfoId) }
//
//                    val planter = planterInfo
//                    val title = "${planter.firstName} ${planter.lastName}"
//                    activity?.toolbarTitle?.text = title

                    val photoPath = mSharedPreferences!!.getString(ValueHelper.PLANTER_PHOTO, null)
                    val profileImageView = mapUserImage
                    if (photoPath != null) {
                        val rotatedBitmap = ImageUtils.decodeBitmap(photoPath, resources.displayMetrics.density)
                        if (rotatedBitmap != null) {
                            profileImageView.setImageBitmap(rotatedBitmap)
                            profileImageView.visibility = View.VISIBLE
                        }
                    } else {
                        profileImageView.visibility = View.GONE
                    }
                }


            } else {
                activity!!.toolbarTitle.text = resources.getString(R.string.user_not_identified)
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()

        map?.isMyLocationEnabled = false
    }

    override fun onResume() {
        super.onResume()

        GlobalScope.launch {
            vm.checkForValidUser()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (mapFragment == null) {
            mapFragment = SupportMapFragment()
            childFragmentManager.beginTransaction().apply {
                add(R.id.map, mapFragment!!)
                commit()
            }
        }

        mSharedPreferences = activity!!.getSharedPreferences(
            "org.greenstand.android", Context.MODE_PRIVATE
        )

        if (!(activity as AppCompatActivity).supportActionBar!!.isShowing) {
            Timber.d("toolbar hide")
            (activity as AppCompatActivity).supportActionBar!!.show()
        }

        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        addTreeButton.setOnClickListener(this)

        if (FeatureFlags.DEBUG_ENABLED) {
            addTreeButton.setOnLongClickListener(this)
        }

        mapFragment!!.getMapAsync(this)

        val minAccuracy = mSharedPreferences!!.getInt(
            ValueHelper.MIN_ACCURACY_GLOBAL_SETTING,
            ValueHelper.MIN_ACCURACY_DEFAULT_SETTING
        )

        if (fragmentMapGpsAccuracy != null) {
            if (userLocationManager.currentLocation != null) {
                if (userLocationManager.currentLocation!!.hasAccuracy() && userLocationManager.currentLocation!!.accuracy < minAccuracy) {
                    fragmentMapGpsAccuracy.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyValue.setTextColor(Color.GREEN)
                    val fragmentMapGpsAccuracyValueString1 = Integer.toString(
                        Math.round(
                            MainActivity
                                .currentLocation!!.accuracy
                        )
                    ) + " " + resources.getString(R.string.meters)
                    fragmentMapGpsAccuracyValue.text = fragmentMapGpsAccuracyValueString1
                    MainActivity.allowNewTreeOrUpdate = true
                } else {
                    fragmentMapGpsAccuracy.setTextColor(Color.RED)
                    MainActivity.allowNewTreeOrUpdate = false

                    if (userLocationManager.currentLocation!!.hasAccuracy()) {
                        fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                        val fragmentMapGpsAccuracyValueString2 = Integer.toString(
                            Math.round(
                                MainActivity
                                    .currentLocation!!.accuracy
                            )
                        ) + " " + resources.getString(R.string.meters)
                        fragmentMapGpsAccuracyValue.text = fragmentMapGpsAccuracyValueString2
                    } else {
                        fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyValue.text = "N/A"
                    }
                }
            } else {
                if (ActivityCompat.checkSelfPermission(activity!!, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        activity!!,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                        Permissions.MY_PERMISSION_ACCESS_COURSE_LOCATION
                    )
                }
                fragmentMapGpsAccuracy.setTextColor(Color.RED)
                fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                fragmentMapGpsAccuracyValue.text = "N/A"
                MainActivity.allowNewTreeOrUpdate = false
            }

        }
    }

    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.addTreeButton -> {
                Timber.d("fab click")

                if (MainActivity.allowNewTreeOrUpdate || !FeatureFlags.HIGH_GPS_ACCURACY) {

                    val currentTimestamp = System.currentTimeMillis() / 1000
                    val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_PLANTER_CHECK_IN_SECONDS, 0)
                    if (currentTimestamp - lastTimeStamp > ValueHelper.CHECK_IN_TIMEOUT) {
                        findNavController().navigate(MapsFragmentDirections.actionGlobalLoginFlowGraph())
                    } else {
                        findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToNewTreeGraph())
                    }
                } else {
                    Toast.makeText(activity, "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // For debug analysis purposes only
    @SuppressLint("SimpleDateFormat")
    override fun onLongClick(view: View): Boolean {
        GlobalScope.launch {
            Toast.makeText(activity, "Adding 500 trees", Toast.LENGTH_LONG).show()
            val didSuceed = vm.createFakeTrees()
            if (didSuceed) {
                Toast.makeText(activity, "500 trees added", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, "Error adding test trees", Toast.LENGTH_LONG).show()
            }
        }

        return true
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToTreePreviewFragment(marker.title))
        return true
    }

    override fun onMapReady(map: GoogleMap) {

        this.map = map

        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        map.isMyLocationEnabled = true

        runBlocking {
            val trees = GlobalScope.async {
                return@async database.treeDao().getTreesToDisplay()
            }.await()

            if (trees.isNotEmpty()) {
                Timber.d("Adding markers")

                var latLng: LatLng? = null

                for (tree in trees) {
                    latLng = LatLng(tree.latitude, tree.longitude)

                    val markerOptions = MarkerOptions()
                        .title(tree.tree_id.toString())// set Id instead of title
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin))
                        .position(latLng)
                    map.addMarker(markerOptions)
                }

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))

            } else {
                if (userLocationManager.currentLocation != null) {
                    val myLatLng =
                        LatLng(userLocationManager.currentLocation!!.latitude, userLocationManager.currentLocation!!.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10f))
                }
            }

        }

        map.setOnMarkerClickListener(this@MapsFragment)

        map.mapType = GoogleMap.MAP_TYPE_NORMAL
    }
}
