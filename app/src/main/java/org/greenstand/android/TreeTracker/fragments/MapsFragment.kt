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
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.amazonaws.util.IOUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.fragment_map.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.database.AppDatabase
import org.greenstand.android.TreeTracker.database.entity.LocationEntity
import org.greenstand.android.TreeTracker.database.entity.PhotoEntity
import org.greenstand.android.TreeTracker.database.entity.TreeEntity
import org.greenstand.android.TreeTracker.database.entity.TreePhotoEntity
import org.greenstand.android.TreeTracker.managers.FeatureFlags
import org.greenstand.android.TreeTracker.managers.UserLocationManager
import org.greenstand.android.TreeTracker.map.TreeMapAnnotation
import org.greenstand.android.TreeTracker.usecases.CreateTreeParams
import org.greenstand.android.TreeTracker.usecases.CreateTreeUseCase
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.Utils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import org.koin.android.ext.android.getKoin
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class MapsFragment : androidx.fragment.app.Fragment(), OnClickListener, OnMarkerClickListener, OnMapReadyCallback,
    View.OnLongClickListener {

    private val userLocationManager: UserLocationManager = getKoin().get()

    private var mSharedPreferences: SharedPreferences? = null

    private var mapFragment: SupportMapFragment? = null
    private var map: GoogleMap? = null

    lateinit var database: AppDatabase

    private lateinit var clusterManager : ClusterManager<TreeMapAnnotation>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = getKoin().get()
    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()

        map?.isMyLocationEnabled = false
    }

    override fun onResume() {
        super.onResume()

        val currentTimestamp = System.currentTimeMillis() / 1000
        val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
        if (FeatureFlags.AUTOMATIC_SIGN_OUT_FEATURE_ENABLED
            && currentTimestamp - lastTimeStamp > ValueHelper.IDENTIFICATION_TIMEOUT) {
            activity!!.toolbarTitle.text = resources.getString(R.string.user_not_identified)
            //reset all sharedPreferences
            val editor = mSharedPreferences!!.edit()
            editor.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
            editor.putString(ValueHelper.PLANTER_IDENTIFIER, null)
            editor.putString(ValueHelper.PLANTER_PHOTO, null)
            editor.apply()
        } else {
            val identifier = mSharedPreferences!!.getString(
                ValueHelper.PLANTER_IDENTIFIER,
                resources.getString(R.string.user_not_identified)
            )
            //
            runBlocking {
                val planterList = GlobalScope.async {
                    database.planterDao().getPlantersByIdentifier(identifier)
                }.await()
                if (planterList.isEmpty()) {
                    activity!!.toolbarTitle.text = resources.getString(R.string.user_not_identified)
                    // And time them out
                    val editor = mSharedPreferences!!.edit()
                    editor.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                    editor.putString(ValueHelper.PLANTER_IDENTIFIER, null)
                    editor.putString(ValueHelper.PLANTER_PHOTO, null)
                    editor.apply()
                } else {
                    val planter = planterList.first()
                    val title = "${planter.firstName} ${planter.lastName}"
                    activity?.toolbarTitle?.text = title

                    val photoPath = mSharedPreferences!!.getString(ValueHelper.PLANTER_PHOTO, null)
                    val imageView = view!!.mapUserImage
                    if (photoPath != null) {
                        val rotatedBitmap = ImageUtils.decodeBitmap(photoPath, resources.displayMetrics.density)
                        if (rotatedBitmap != null) {
                            imageView.setImageBitmap(rotatedBitmap)
                            imageView.visibility = View.VISIBLE

                        }
                    } else {
                        imageView.visibility = View.GONE
                    }
                }
            }
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

        goToUploadsButton.setOnClickListener(this)

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
                    val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                    if (currentTimestamp - lastTimeStamp > ValueHelper.IDENTIFICATION_TIMEOUT) {
                        findNavController().navigate(MapsFragmentDirections.actionGlobalLoginFlowGraph())
                    } else {
                        findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToNewTreeGraph())
                    }
                } else {
                    Toast.makeText(activity, "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.goToUploadsButton -> {
                findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToDataFragment())
            }
        }
    }

    // For debug analysis purposes only
    @SuppressLint("SimpleDateFormat")
    override fun onLongClick(view: View): Boolean {

        Toast.makeText(activity, "Adding 500 trees", Toast.LENGTH_LONG).show()
        // programmatically add 500 trees, for analysis only
        // this is on the main thread for ease, in Kotlin just make a Coroutine

        userLocationManager.currentLocation ?: return true

        GlobalScope.launch {
            val userId = -1

            for (i in 0..499) {

                val myInput = activity!!.assets.open("testtreeimage.jpg")
                val f = ImageUtils.createImageFile(activity!!)
                val fos = FileOutputStream(f)
                fos.write(IOUtils.toByteArray(myInput))
                fos.close()

                val createTreeParams = CreateTreeParams(
                    userId = userId.toLong(),
                    photoPath = f.absolutePath,
                    content = "My Note",
                    planterIdentifierId = mSharedPreferences!!.getLong(ValueHelper.PLANTER_IDENTIFIER_ID, 0)
                )

                getKoin().get<CreateTreeUseCase>().execute(createTreeParams)

            }
        }
        Toast.makeText(activity, "500 trees added", Toast.LENGTH_LONG).show()
        return true
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if(marker.title == null){
            return true;
        }
        findNavController().navigate(MapsFragmentDirections.actionMapsFragmentToTreePreviewFragment(marker.title))
        return true
    }


    override fun onMapReady(map: GoogleMap) {

        this.map = map

        this.clusterManager = ClusterManager(this.context, this.map)
        this.map!!.setOnCameraIdleListener(this.clusterManager)
        this.map!!.setOnMarkerClickListener(this.clusterManager)


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
        map.getUiSettings().setMyLocationButtonEnabled(false)

        clusterManager = this.clusterManager

        runBlocking {
            val trees = GlobalScope.async {
                return@async database.treeDao().getTreesToDisplay()
            }.await()

            if (trees.isNotEmpty()) {
                Timber.d("Adding markers")

                var latLng: LatLng? = null

                for (tree in trees) {

                    val treeMapAnnotation = TreeMapAnnotation(tree.latitude, tree.longitude)
                    clusterManager.addItem(treeMapAnnotation)

                }


            }
            if (userLocationManager.currentLocation != null) {
                val myLatLng =
                        LatLng(userLocationManager.currentLocation!!.latitude, userLocationManager.currentLocation!!.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10f))
            }

        }


        map.mapType = GoogleMap.MAP_TYPE_NORMAL
    }
}
