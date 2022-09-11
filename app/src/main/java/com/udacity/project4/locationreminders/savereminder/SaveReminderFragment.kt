package com.udacity.project4.locationreminders.savereminder

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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import org.koin.android.ext.android.inject
import com.udacity.project4.R
import kotlinx.android.synthetic.main.fragment_save_reminder.*
import kotlinx.android.synthetic.main.it_reminder.*

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var _binding: FragmentSaveReminderBinding
    private lateinit var reminder: ReminderDataItem

    private var _snackBar: Snackbar? = null

    private val _geofenceClient: GeofencingClient by lazy {LocationServices.getGeofencingClient(requireActivity())}

    private lateinit var _requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var _requestLocationSettings: ActivityResultLauncher<IntentSenderRequest>

    private val _geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

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

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                _snackBar?.dismiss()
                activity?.onBackPressed()
            }
        })

        askPermissions()
        requestPermissionsResult()

        snackBarAction()

        locationTitle()

        _binding.saveReminder.setOnClickListener {

            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminder = ReminderDataItem(title, description, location, latitude, longitude)

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
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    _requestLocationSettings.launch(IntentSenderRequest.Builder(exception.resolution.intentSender).build())
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
                if (_viewModel.validateEnteredData(reminder)) {
                    startGeofence()
                }
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
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (runningQOrLater) {
            permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
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

    private fun requestLocationPermissions() {

        _requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    //Geofence
    @SuppressLint("MissingPermission")
    private fun startGeofence() {

        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(reminder.latitude!!, reminder.longitude!!, GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        _geofenceClient.addGeofences(geofenceRequest, _geofencePendingIntent).run {
            addOnSuccessListener {
                Snackbar.make(
                    _binding.fragmentSaveReminder,
                    getString(R.string.geofence_entered),
                    Snackbar.LENGTH_SHORT
                ).show()
                _binding.viewModel?.saveReminder(reminder)
            }

            addOnFailureListener {
                Toast.makeText(requireContext(), R.string.geofences_not_added,
                    Toast.LENGTH_SHORT).show()
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

    //Snackbar
    private fun snackBarAction() {

        _viewModel.showSnackBarInt.observe(viewLifecycleOwner, Observer {
            Snackbar.make(
                _binding.fragmentSaveReminder,
                getString(it!!),
                Snackbar.LENGTH_LONG
            )
                .show()
        })

        _viewModel.showSnackBar.observe(viewLifecycleOwner, Observer{
            val snackBar = Snackbar.make(
                _binding.fragmentSaveReminder,
                it,
                Snackbar.LENGTH_INDEFINITE
            )

            snackBar.setAction("enable", View.OnClickListener {
                requestLocationPermissions()
            })

            snackBar.show()
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            // We don't rely on the result code, but just check the location setting again
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED))
        {
            // Permission denied.
            _snackBar = Snackbar.make(
                fragment_save_reminder,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
            _snackBar?.setAction(R.string.settings) {
                // Displays App settings screen.
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            _snackBar?.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

}

private const val GEOFENCE_RADIUS_IN_METERS = 100.0f

private const val TAG = "SaveReminderActivity"
private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
