package org.greenstand.android.TreeTracker.fragments


import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.HapticFeedbackConstants
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
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
import org.greenstand.android.TreeTracker.BuildConfig
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.activities.MainActivity
import org.greenstand.android.TreeTracker.application.Permissions
import org.greenstand.android.TreeTracker.application.TreeTrackerApplication
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.ValueHelper
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class MapsFragment : androidx.fragment.app.Fragment(), OnClickListener, OnMarkerClickListener, OnMapReadyCallback, View.OnLongClickListener {

    private var mSettingCallback: LocationDialogListener? = null

    private var mSharedPreferences: SharedPreferences? = null
    private var paused = false

    private var fragment: androidx.fragment.app.Fragment? = null

    private var bundle: Bundle? = null

    private var fragmentTransaction: androidx.fragment.app.FragmentTransaction? = null
    private var mapFragment: SupportMapFragment? = null


    interface LocationDialogListener {
        fun refreshMap()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            mSettingCallback = context as LocationDialogListener?
        } catch (e: ClassCastException) {
            throw ClassCastException(context!!.toString() + " must implement LocationDialogListener")
        }

    }

    override fun onPause() {
        super.onPause()

        paused = true
    }

    override fun onResume() {
        super.onResume()

        TreeTrackerApplication.getDatabaseManager().openDatabase()

        if (paused) {
            (childFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
        }
        paused = false

        val currentTimestamp = System.currentTimeMillis() / 1000
        val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
        if(currentTimestamp - lastTimeStamp > ValueHelper.IDENTIFICATION_TIMEOUT){
            activity!!.toolbarTitle.text = resources.getString(R.string.user_not_identified)
            //reset all sharedPreferences
            val editor = mSharedPreferences!!.edit()
            editor.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
            editor.putString(ValueHelper.PLANTER_IDENTIFIER, null)
            editor.putString(ValueHelper.PLANTER_PHOTO, null)
            editor.apply()
        } else {
            val identifier = mSharedPreferences!!.getString(ValueHelper.PLANTER_IDENTIFIER, resources.getString(R.string.user_not_identified))
            //
            val cursor = TreeTrackerApplication.getDatabaseManager().queryCursor("SELECT * FROM planter_details WHERE identifier = '$identifier'", null)
            if(cursor.count == 0){
                activity!!.toolbarTitle.text = resources.getString(R.string.user_not_identified)
                // And time them out
                val editor = mSharedPreferences!!.edit()
                editor.putLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                editor.putString(ValueHelper.PLANTER_IDENTIFIER, null)
                editor.putString(ValueHelper.PLANTER_PHOTO, null)
                editor.commit()
            } else {
                cursor.moveToFirst()
                val title = cursor.getString(cursor.getColumnIndex("first_name")) + " " + cursor.getString(cursor.getColumnIndex("last_name"))
                activity!!.toolbarTitle.text = title

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

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            val fragment = activity!!
                    .supportFragmentManager.findFragmentById(
                    R.id.map) as SupportMapFragment?
            if (fragment != null)
                activity!!.supportFragmentManager.beginTransaction().remove(fragment).commit()

        } catch (e: IllegalStateException) {
            //handle this situation because you are necessary will get
            //an exception here :-(
        }

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        var v : View? = view

        try {
            v = inflater.inflate(R.layout.fragment_map, container, false)
        } catch (e: InflateException) {
            Timber.d(e.localizedMessage);
        }

        if(mapFragment == null) {
            mapFragment = SupportMapFragment()
            childFragmentManager.beginTransaction().apply {
                add(R.id.map, mapFragment!!)
                commit()
            }
        }

        mSharedPreferences = activity!!.getSharedPreferences(
                "org.greenstand.android", Context.MODE_PRIVATE)

        if (!(activity as AppCompatActivity).supportActionBar!!.isShowing) {
            Timber.d("toolbar hide")
            (activity as AppCompatActivity).supportActionBar!!.show()
        }

        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        val fab = v!!.addTreeButton
        fab.setOnClickListener(this)
        if (BuildConfig.BUILD_TYPE === "dev") {
            fab.setOnLongClickListener(this)
        }

        mapFragment!!.getMapAsync(this)

        val minAccuracy = mSharedPreferences!!.getInt(ValueHelper.MIN_ACCURACY_GLOBAL_SETTING, ValueHelper.MIN_ACCURACY_DEFAULT_SETTING)

        if (fragmentMapGpsAccuracy != null) {
            if (MainActivity.mCurrentLocation != null) {
                if (MainActivity.mCurrentLocation!!.hasAccuracy() && MainActivity.mCurrentLocation!!.accuracy < minAccuracy) {
                    fragmentMapGpsAccuracy.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyValue.setTextColor(Color.GREEN)
                    fragmentMapGpsAccuracyValue.text = Integer.toString(Math.round(MainActivity.mCurrentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    MainActivity.mAllowNewTreeOrUpdate = true
                } else {
                    fragmentMapGpsAccuracy.setTextColor(Color.RED)
                    MainActivity.mAllowNewTreeOrUpdate = false

                    if (MainActivity.mCurrentLocation!!.hasAccuracy()) {
                        fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyValue.text = Integer.toString(Math.round(MainActivity.mCurrentLocation!!.accuracy)) + " " + resources.getString(R.string.meters)
                    } else {
                        fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                        fragmentMapGpsAccuracyValue.text = "N/A"
                    }
                }
            } else {
                if (ActivityCompat.checkSelfPermission(activity!!, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(activity!!, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                            Permissions.MY_PERMISSION_ACCESS_COURSE_LOCATION)
                }
                fragmentMapGpsAccuracy.setTextColor(Color.RED)
                fragmentMapGpsAccuracyValue.setTextColor(Color.RED)
                fragmentMapGpsAccuracyValue.text = "N/A"
                MainActivity.mAllowNewTreeOrUpdate = false
            }

        }

        return v
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Permissions.MY_PERMISSION_ACCESS_COURSE_LOCATION) {
            mSettingCallback?.refreshMap()
        }
    }


    override fun onClick(v: View) {


        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)

        val photoCursor: Cursor
        when (v.id) {
            R.id.addTreeButton -> {
                Timber.d("fab click")



                if (MainActivity.mAllowNewTreeOrUpdate || BuildConfig.GPS_ACCURACY == "off") {

                    val currentTimestamp = System.currentTimeMillis() / 1000
                    val lastTimeStamp = mSharedPreferences!!.getLong(ValueHelper.TIME_OF_LAST_USER_IDENTIFICATION, 0)
                    if(currentTimestamp - lastTimeStamp > ValueHelper.IDENTIFICATION_TIMEOUT){

                        fragment = UserIdentificationFragment()
                        fragmentTransaction = activity!!.supportFragmentManager
                                .beginTransaction()
                        fragmentTransaction!!.replace(R.id.containerFragment, fragment as UserIdentificationFragment).addToBackStack(ValueHelper.IDENTIFY_FRAGMENT).commit()

                    } else {

                        fragment = NewTreeFragment()
                        bundle = activity!!.intent.extras
                        fragment!!.arguments = bundle

                        fragmentTransaction = activity!!.supportFragmentManager
                                .beginTransaction()
                        fragmentTransaction!!.replace(R.id.containerFragment, fragment as NewTreeFragment).addToBackStack(ValueHelper.NEW_TREE_FRAGMENT).commit()


                    }
                } else {
                    Toast.makeText(activity, "Insufficient GPS accuracy.", Toast.LENGTH_SHORT).show()
                }
            }
        }//			case R.id.fragment_map_update_tree:
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

    // For debug analysis purposes only
    override fun onLongClick(view: View): Boolean {

        Toast.makeText(activity, "Adding lot of trees", Toast.LENGTH_LONG).show()


        // programmatically add 500 trees, for analysis only
        // this is on the main thread for ease, in Kotlin just make a Coroutine

        val userId = -1

        for (i in 0..499) {

            val locationContentValues = ContentValues()
            locationContentValues.put("accuracy",
                    java.lang.Float.toString(MainActivity.mCurrentLocation!!.accuracy))
            locationContentValues.put("lat",
                    java.lang.Double.toString(MainActivity.mCurrentLocation!!.latitude + (Math.random() - .5) / 1000))
            locationContentValues.put("long",
                    java.lang.Double.toString(MainActivity.mCurrentLocation!!.longitude + (Math.random() - .5) / 1000))
            locationContentValues.put("user_id", userId)

            val locationId = TreeTrackerApplication.getDatabaseManager().insert("location", null, locationContentValues)

            var photoId: Long = -1
            try {
                val myInput = activity!!.assets.open("testtreeimage.jpg")
                val f = ImageUtils.createImageFile(activity!!)
                val fos = FileOutputStream(f)
                fos.write(IOUtils.toByteArray(myInput))
                fos.close()

                val photoContentValues = ContentValues()
                photoContentValues.put("user_id", userId)
                photoContentValues.put("location_id", locationId)
                photoContentValues.put("name", f.absolutePath)

                photoId = TreeTrackerApplication.getDatabaseManager().insert("photo", null, photoContentValues)
                //Timber.d("photoId " + Long.toString(photoId));

            } catch (e: IOException) {
                e.printStackTrace()
            }


            val treeContentValues = ContentValues()
            treeContentValues.put("user_id", userId)
            treeContentValues.put("location_id", locationId)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            treeContentValues.put("time_created", dateFormat.format(Date()))
            treeContentValues.put("time_updated", dateFormat.format(Date()))

            val treeId = TreeTrackerApplication.getDatabaseManager().insert("tree", null, treeContentValues)


            val treePhotoContentValues = ContentValues()
            treePhotoContentValues.put("tree_id", treeId)
            treePhotoContentValues.put("photo_id", photoId)
            val treePhotoId = TreeTrackerApplication.getDatabaseManager().insert("tree_photo", null, treePhotoContentValues)
            //Timber.d("treePhotoId " + Long.toString(treePhotoId));
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
        fragmentTransaction!!.replace(R.id.containerFragment, fragment as TreePreviewFragment).addToBackStack(ValueHelper.TREE_PREVIEW_FRAGMENT).commit()
        return true
    }


    override fun onMapReady(map: GoogleMap) {


        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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


        val treeCursor = TreeTrackerApplication.getDatabaseManager().queryCursor("select *, tree._id as tree_id from tree left outer join location on location_id = location._id where is_missing = 'N'", null)
        treeCursor.moveToFirst()

        if (treeCursor.count > 0) {
            Timber.d("Adding markers")

            var latLng: LatLng? = null

            do {

                latLng = LatLng(java.lang.Double.parseDouble(treeCursor.getString(treeCursor.getColumnIndex("lat"))),
                        java.lang.Double.parseDouble(treeCursor.getString(treeCursor.getColumnIndex("long"))))


                val markerOptions = MarkerOptions()
                        .title(java.lang.Long.toString(treeCursor.getLong(treeCursor.getColumnIndex("tree_id"))))// set Id instead of title
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.green_pin))
                        .position(latLng)
                map.addMarker(markerOptions)

            } while (treeCursor.moveToNext())


            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20f))

        } else {
            if (MainActivity.mCurrentLocation != null) {
                val myLatLng = LatLng(MainActivity.mCurrentLocation!!.latitude, MainActivity.mCurrentLocation!!.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10f))
            }
        }

        map.setOnMarkerClickListener(this@MapsFragment)

        map.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    companion object {
        private val TAG = "MapsFragment"
        private var view: View? = null
    }
}
