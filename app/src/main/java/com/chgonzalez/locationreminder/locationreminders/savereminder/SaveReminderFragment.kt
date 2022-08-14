package com.chgonzalez.locationreminder.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.chgonzalez.locationreminder.BuildConfig
import com.chgonzalez.locationreminder.R
import com.chgonzalez.locationreminder.base.BaseFragment
import com.chgonzalez.locationreminder.base.NavigationCommand
import com.chgonzalez.locationreminder.databinding.FragmentSaveReminderBinding
import com.chgonzalez.locationreminder.locationreminders.geofence.GeofenceBroadcastReceiver
import com.chgonzalez.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.chgonzalez.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.it_reminder.*
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var _binding: FragmentSaveReminderBinding
    private val _geofenceClient: GeofencingClient by lazy {LocationServices.getGeofencingClient(requireContext())}
    private lateinit var _requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var _requestLocationSettings: ActivityResultLauncher<IntentSenderRequest>
    private val _geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
//        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private val title = _viewModel.reminderTitle.value
    private val description = _viewModel.reminderDescription.value
    private val location = _viewModel.reminderSelectedLocationStr.value
    private val latitude = _viewModel.latitude.value
    private val longitude = _viewModel.longitude.value

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        _binding.viewModel = _viewModel

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = this
        _binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        askPermissions()
        requestPermissionsResult()

        locationTitle()

        _binding.saveReminder.setOnClickListener {
            checkPermissionsAndStartGeofence()
        }

    }

//    Permissions
    private fun checkPermissionsAndStartGeofence() {
        if (locationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    _requestLocationSettings.launch(IntentSenderRequest.Builder(exception.resolution).build())
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + exception.message)
                }
            } else {
                Snackbar.make(
                    _binding.fragmentSaveReminder,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                startGeofence()
            }
        }
    }

    @TargetApi(29)
    private fun locationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                return true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (locationPermissionApproved()) return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (runningQOrLater) {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        }

        _requestPermissionLauncher.launch(permissionsArray)
    }

    private fun askPermissions() {

        _requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->

            if(permissions.all { permission -> permission.value }){
                _binding.saveReminder.callOnClick()
            }else{
                _viewModel.showSnackBar.value = getString(R.string.determine_location_error)
            }
        }
    }

    private fun requestPermissionsResult() {

        _requestLocationSettings = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
            try {
                when (it.resultCode) {
                    BACKGROUND_LOCATION_PERMISSION_INDEX -> {
                        _binding.saveReminder.callOnClick()
                    }
                    LOCATION_PERMISSION_INDEX -> {
                        _viewModel.showSnackBar.value = getString(R.string.permission_denied_explanation)
                    }
                    else -> {

                        Log.e("SaveReminderFragment", "Unable to receive location setting.")
                    }
                }
            }catch(exception: Exception){
                Log.e("SaveReminderFragment", exception.localizedMessage!!)
            }
        }
    }

    //Geofence
    @SuppressLint("MissingPermission")
    private fun startGeofence() {

        val data = ReminderDataItem(
            title,
            description,
            location,
            latitude,
            longitude
        )
        if (_viewModel.validateEnteredData(data)) {
            val geofence = Geofence.Builder()
                .setRequestId(data.id)
                .setCircularRegion(data.latitude!!, data.longitude!!, GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofenceRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            _geofenceClient.addGeofences(geofenceRequest, _geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Snackbar.make(
                        _binding.fragmentSaveReminder,
                        getString(R.string.geofence_entered),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    _viewModel.validateAndSaveReminder(data)
                }

                addOnFailureListener {
                    Toast.makeText(requireContext(), R.string.geofences_not_added,
                        Toast.LENGTH_SHORT).show()
                    if ((it.message != null)) {
                        Log.w(TAG, it.message!!)
                    }
                }
            }
        }
    }

    //Location Title
    private fun locationTitle() {

        _viewModel.reminderSelectedLocationStr.observe(viewLifecycleOwner, Observer {
            if(it != null){
                _binding.selectedLocation.text = it
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.locationally.action.ACTION_GEOFENCE_EVENT"
    }

}

private const val GEOFENCE_RADIUS_IN_METERS = 100.0f

private const val TAG = "SaveReminderActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
