package org.greenstand.android.TreeTracker.fragments


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

    private var mSettingCallback: LocationDialogListener? = null

    private var mSharedPreferences: SharedPreferences? = null
    private var paused = false

    private var fragment: androidx.fragment.app.Fragment? = null

    private var bundle: Bundle? = null

    private var fragmentTransaction: androidx.fragment.app.FragmentTransaction? = null
    private var mapFragment: SupportMapFragment? = null
    private var map: GoogleMap? = null

    interface LocationDialogListener {
        fun refreshMap()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            mSettingCallback = context as LocationDialogListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement LocationDialogListener")
        }

    }

    lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = getKoin().get()
    }

    override fun onPause() {
        super.onPause()

        if(this.map != null){
            this.map?.isMyLocationEnabled = false
        }

        paused = true
    }

    override fun onResume() {
        super.onResume()

        if (paused) {
            (childFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        }
        paused = false

        val currentTimestamp = System.currentTimeMillis() / 1000
        val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
        if (currentTimestamp - lastTimeStamp > ValueHelper.IDENTIFICATION_TIMEOUT) {
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

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            val fragment = activity!!
                .supportFragmentManager.findFragmentById(
                R.id.map
            ) as SupportMapFragment?
            if (fragment != null)
                activity!!.supportFragmentManager.beginTransaction().remove(fragment).commit()

        } catch (e: IllegalStateException) {
            //handle this situation because you are necessary will get
            //an exception here :-(
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
            if (MainActivity.mCurrentLocation != null) {
                if (MainActivity.mCurrentLocation!!.hasAccuracy() && MainActivity.mCurrentLocation!!.accuracy < minAccuracy) {
                    fragmentMapGpsAccuracy.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyValue.setTextColor(Color.GREEN)
                    val fragmentMapGpsAccuracyValueString1 = Integer.toString(
                        Math.round(
                            MainActivity
                                .mCurrentLocation!!.accuracy
                        )
                    ) + " " + resources.getString(R.string.meters)
                    fragmentMapGpsAccuracyValue.text = fragmentMapGpsAccuracyValueString1
                    MainActivity.mAllowNewTreeOrUpdate = true
                } else {
                    fragmentMapGpsAccuracy.setTextColor(Color.RED)
                    MainActivity.mAllowNewTreeOrUpdate = false

                    if (MainActivity.mCurrentLocation!!.hasAccuracy()) {
                        fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                        val fragmentMapGpsAccuracyValueString2 = Integer.toString(
                            Math.round(
                                MainActivity
                                    .mCurrentLocation!!.accuracy
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
                MainActivity.mAllowNewTreeOrUpdate = false
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Permissions.MY_PERMISSION_ACCESS_COURSE_LOCATION) {
            mSettingCallback?.refreshMap()
        }
    }


    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        when (v.id) {
            R.id.addTreeButton -> {
                Timber.d("fab click")

                if (MainActivity.mAllowNewTreeOrUpdate || !FeatureFlags.HIGH_GPS_ACCURACY) {

                    val currentTimestamp = System.currentTimeMillis() / 1000
                    val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                    if (currentTimestamp - lastTimeStamp > ValueHelper.IDENTIFICATION_TIMEOUT) {

                        fragment = LoginFragment()
                        fragmentTransaction = activity!!.supportFragmentManager
                            .beginTransaction()
                        fragmentTransaction?.replace(R.id.containerFragment, fragment as LoginFragment)
                            ?.addToBackStack(ValueHelper.IDENTIFY_FRAGMENT)?.commit()

                    } else {
                        fragment = NewTreeFragment()
                        bundle = activity!!.intent.extras
                        fragment?.arguments = bundle

                        fragmentTransaction = activity?.supportFragmentManager
                            ?.beginTransaction()
                        fragmentTransaction?.replace(R.id.containerFragment, fragment as NewTreeFragment)
                            ?.addToBackStack(ValueHelper.NEW_TREE_FRAGMENT)?.commit()

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

        Toast.makeText(activity, "Adding lot of trees", Toast.LENGTH_LONG).show()
        // programmatically add 500 trees, for analysis only
        // this is on the main thread for ease, in Kotlin just make a Coroutine

        MainActivity.mCurrentLocation ?: return true

        GlobalScope.launch {
            val userId = -1


            for (i in 0..499) {

                val location = LocationEntity(
                    MainActivity.mCurrentLocation!!.accuracy.toInt(),
                    MainActivity.mCurrentLocation!!.latitude + (Math.random() - .5) / 1000,
                    MainActivity.mCurrentLocation!!.longitude + (Math.random() - .5) / 1000,
                    userId.toLong()
                )

                val locationId = database.locationDao().insert(location)

                var photoId: Long = -1
                try {
                    val myInput = activity!!.assets.open("testtreeimage.jpg")
                    val f = ImageUtils.createImageFile(activity!!)
                    val fos = FileOutputStream(f)
                    fos.write(IOUtils.toByteArray(myInput))
                    fos.close()

                    val photo =
                        PhotoEntity(
                            f.absolutePath,
                            locationId.toInt(),
                            0,
                            false,
                            Utils.dateFormat.format(Date()),
                            userId.toLong()
                        )
                    photoId = database.photoDao().insertPhoto(photo)

                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val treeEntity = TreeEntity(
                    UUID.randomUUID().toString(),
                    0,
                    Utils.dateFormat.format(Date()),
                    Utils.dateFormat.format(Date()),
                    "",
                    null,
                    locationId.toInt(),
                    false,
                    false,
                    false,
                    null,
                    null,
                    null,
                    userId.toLong(),
                    null
                )

                val treeId = database.treeDao().insert(treeEntity)

                val treePhotoEntity = TreePhotoEntity(treeId, photoId)
                database.photoDao().insert(treePhotoEntity)
                //Timber.d("treePhotoId " + Long.toString(treePhotoId));
            }
        }
        Toast.makeText(activity, "Lots of trees added", Toast.LENGTH_LONG).show()
        return true
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        fragment = TreePreviewFragment()
        bundle = activity!!.intent.extras

        if (bundle == null)
            bundle = Bundle()

        bundle!!.putString(ValueHelper.TREE_ID, marker.title)
        fragment!!.arguments = bundle

        fragmentTransaction = activity!!.supportFragmentManager
            .beginTransaction()
        fragmentTransaction?.replace(R.id.containerFragment, fragment as TreePreviewFragment)
            ?.addToBackStack(ValueHelper.TREE_PREVIEW_FRAGMENT)?.commit()
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
                if (MainActivity.mCurrentLocation != null) {
                    val myLatLng =
                        LatLng(MainActivity.mCurrentLocation!!.latitude, MainActivity.mCurrentLocation!!.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10f))
                }
            }

        }

        map.setOnMarkerClickListener(this@MapsFragment)

        map.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    companion object {
        private val TAG = "MapsFragment"
        @SuppressLint("StaticFieldLeak")
        private var view: View? = null
    }
}
