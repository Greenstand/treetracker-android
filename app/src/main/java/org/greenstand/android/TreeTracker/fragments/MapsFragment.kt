package org.greenstand.android.TreeTracker.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenstand.android.TreeTracker.R
import org.greenstand.android.TreeTracker.database.TreeTrackerDAO
import org.greenstand.android.TreeTracker.databinding.FragmentMapBinding
import org.greenstand.android.TreeTracker.map.TreeMapMarker
import org.greenstand.android.TreeTracker.models.FeatureFlags
import org.greenstand.android.TreeTracker.models.LocationUpdateManager
import org.greenstand.android.TreeTracker.models.Planter
import org.greenstand.android.TreeTracker.utilities.ImageUtils
import org.greenstand.android.TreeTracker.utilities.TreeClusterRenderer
import org.greenstand.android.TreeTracker.utilities.mainActivity
import org.greenstand.android.TreeTracker.utilities.vibrate
import org.greenstand.android.TreeTracker.viewmodels.MapViewModel
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber

class MapsFragment :
    androidx.fragment.app.Fragment(),
    OnClickListener,
    OnMapReadyCallback,
    View.OnLongClickListener {

    private lateinit var bindings: FragmentMapBinding

    private val vm: MapViewModel by viewModel()
    private val locationUpdateManager: LocationUpdateManager by inject()
    private val dao: TreeTrackerDAO by inject()
    private val user: Planter by inject()

    private var mapFragment: SupportMapFragment? = null
    private var map: GoogleMap? = null

    private lateinit var convergenceProgressView: Group
    private lateinit var clusterManager: ClusterManager<TreeMapMarker>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindings = FragmentMapBinding.inflate(inflater)
        return bindings.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        convergenceProgressView = view.findViewById<Group>(R.id.convergenceProgressGroup)
        convergenceProgressView.visibility = View.GONE

        vm.checkInStatusLiveData.observe(
            this,
            Observer<Boolean> { planterIsCheckedIn ->
                if (planterIsCheckedIn) {
                    GlobalScope.launch(Dispatchers.Main) {

                       mainActivity().bindings.toolbarTitle.text = vm.getPlanterName()

                        val photoPath = user.profilePhotoPath
                        val profileImageView = bindings.mapUserImage

                        if (photoPath != null) {
                            val rotatedBitmap =
                                ImageUtils.decodeBitmap(photoPath, resources.displayMetrics.density)
                            if (rotatedBitmap != null) {
                                profileImageView.setImageBitmap(rotatedBitmap)
                                profileImageView.visibility = View.VISIBLE
                            }
                        } else {
                            profileImageView.visibility = View.GONE
                        }
                    }
                }
            }
        )

        if (mapFragment == null) {
            mapFragment = SupportMapFragment()
            childFragmentManager.beginTransaction().apply {
                add(R.id.map, mapFragment!!)
                commit()
            }
        }

        if (!(activity as AppCompatActivity).supportActionBar!!.isShowing) {
            Timber.d("toolbar hide")
            (activity as AppCompatActivity).supportActionBar!!.show()
        }

        (activity as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        bindings.addTreeButton.setOnClickListener(this)

        bindings.goToUploadsButton.setOnClickListener(this)

        if (FeatureFlags.DEBUG_ENABLED) {
            bindings.addTreeButton.setOnLongClickListener(this)
        }

        mapFragment!!.getMapAsync(this)
    }

    override fun onClick(v: View) {
        v.vibrate()
        when (v.id) {
            R.id.addTreeButton -> {
                Timber.d("fab click")
                // Disable the addTreeButton below to avoid triggering the onClick listener
                // more than one once.
                bindings.addTreeButton.isEnabled = false
                lifecycleScope.launch {
                    if (vm.requiresLogin()) {
                        findNavController().navigate(
                            MapsFragmentDirections.actionGlobalLoginFlowGraph()
                        )
                    } else {
                        vm.turnOnTreeCaptureMode()
                        convergenceProgressView.visibility = View.VISIBLE
                        vm.resolveLocationConvergence()
                        convergenceProgressView.visibility = View.GONE
                        findNavController()
                            .navigate(MapsFragmentDirections.actionMapsFragmentToNewTreeGraph())
                    }
                }
            }
            R.id.goToUploadsButton -> {
//                findNavController().navigate(
//                    MapsFragmentDirections.actionMapsFragmentToDataFragment(
//                        true
//                    )
//                )
            }
        }
    }

    // For debug analysis purposes only
    @SuppressLint("SimpleDateFormat")
    override fun onLongClick(view: View): Boolean {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(activity, "Adding 500 trees", Toast.LENGTH_LONG).show()
            val didSuceed = withContext(Dispatchers.IO) { vm.createFakeTrees() }
            if (didSuceed) {
                Toast.makeText(activity, "500 trees added", Toast.LENGTH_LONG).show()
                renderTrees()
            } else {
                Toast.makeText(activity, "Error adding test trees", Toast.LENGTH_LONG).show()
            }
        }
        return true
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

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isMapToolbarEnabled = false
        }

        clusterManager = ClusterManager(context, map)
        clusterManager.renderer = TreeClusterRenderer(requireContext(), googleMap, clusterManager)
        clusterManager.setOnClusterItemClickListener {
            findNavController().navigate(
                MapsFragmentDirections.actionMapsFragmentToTreePreviewFragment(
                    it.title
                )
            )
            true
        }
        map!!.setOnCameraIdleListener(clusterManager)
        map!!.setOnMarkerClickListener(clusterManager)

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
        map!!.isMyLocationEnabled = true

        map!!.mapType = GoogleMap.MAP_TYPE_NORMAL
        renderTrees()
    }

    private fun renderTrees() {

        map ?: return

        runBlocking {
            val trees = withContext(Dispatchers.IO) { dao.getTreeDataForMap() }

            clusterManager.clearItems()

            if (trees.isNotEmpty()) {
                Timber.d("Adding markers")
                for (tree in trees) {
                    val treeMapAnnotation = TreeMapMarker(
                        tree.latitude,
                        tree.longitude,
                        _title = tree.treeCaptureId.toString()
                    )
                    clusterManager.addItem(treeMapAnnotation)
                }
            }
            if (locationUpdateManager.currentLocation != null) {
                val myLatLng = LatLng(
                    locationUpdateManager.currentLocation!!.latitude,
                    locationUpdateManager.currentLocation!!.longitude
                )
                map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 10f))
            }
        }
    }
}
