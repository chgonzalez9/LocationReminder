package com.chgonzalez.locationreminder.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.chgonzalez.locationreminder.R
import com.chgonzalez.locationreminder.base.BaseFragment
import com.chgonzalez.locationreminder.base.NavigationCommand
import com.chgonzalez.locationreminder.databinding.FragmentSelectLocationBinding
import com.chgonzalez.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.chgonzalez.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var _binding: FragmentSelectLocationBinding
    private lateinit var _map: GoogleMap
    private lateinit var _requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val _locationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(requireActivity()) }
    private var _marker: Marker? = null

    private val TAG = SelectLocationFragment::class.java.simpleName

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        _binding.viewModel = _viewModel
        _binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        snackbar()
        askPermissions()

        saveLocationAction()

        return _binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        _map = googleMap

        setMapStyle(_map)

        enableMyLocation()

        setMapLongClick(_map)
        setPoiClick(_map)
    }


//      Location Permissions
    private fun enableMyLocation() {

        if (ActivityCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            _requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
        else {
            _map.isMyLocationEnabled = true

            _locationClient.lastLocation.addOnCompleteListener {
                if (it.isSuccessful && it.result != null) {
                    val location = LatLng(it.result.latitude, it.result.longitude)
                    _map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        location,
                        zoomLevel
                    ))

                    _marker = _map.addMarker(
                        MarkerOptions()
                            .position(location)
                            .title(getString(R.string.current_location_title))
                    )
                }
            }
        }
    }

    private fun askPermissions() {

        _requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->

            if(permissions.all { permission -> permission.value }){
                enableMyLocation()
            }else{
                _viewModel.showSnackBar.value = getString(R.string.determine_location_error)
            }
        }
    }

    private fun snackbar() {

        _viewModel.showSnackBar.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            Snackbar.make(
                _binding.activityMaps,
                it!!,
                Snackbar.LENGTH_LONG
            )
                .show()
        })
    }

//    Maps Utils
    private fun onLocationSelected() {

        _viewModel.reminderSelectedLocationStr.value = _marker?.title
        _viewModel.latitude.value = _marker?.position?.latitude
        _viewModel.longitude.value = _marker?.position?.longitude
        _viewModel.navigationCommand.value =
            NavigationCommand.To(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
    }

    private fun saveLocationAction() {

        _binding.mapButton.setOnClickListener {

            if (_marker == null) {
                _viewModel.showSnackBar.value = "Please select a location on the map"
            }else {
                onLocationSelected()
            }
        }
    }


    @Deprecated("Deprecated in Java", ReplaceWith(
        "inflater.inflate(R.menu.map_options, menu)",
        "com.chgonzalez.locationreminder.R"
    )
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.map_options, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.normal_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            _map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
    //    Maps Implementation
    private fun setMapStyle(map: GoogleMap) {

        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.map_style
                )
            )
            if (!success) {
                Log.e( TAG,"Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e( TAG,"Can't find style. Error: ", e)
        }
    }

    private fun setMapLongClick(map: GoogleMap) {

        map.setOnMapLongClickListener { latLng ->
            _marker?.remove()
            _marker = _map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.custom_location_title))
            )
        }
    }

    private fun setPoiClick(map: GoogleMap) {

        map.setOnPoiClickListener { poi ->
            _marker?.remove()
            _marker = _map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            _marker?.showInfoWindow()
        }
    }

}

private const val zoomLevel = 15f
