package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofenceConstants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    lateinit var reminder: ReminderDataItem

    private val isRunningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private var permissionsLauncher: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (
            permissions.values.all {
                it == true
            }
        ) {
            checkLocationSettingsThenAddReminderGeofence()
        } else {
            Snackbar.make(
                requireView(),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        }
    }
    private var locationSettingsLauncher : ActivityResultLauncher<IntentSenderRequest> =  registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            addReminderGeofence()
        }
    }

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceConstants.ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val reminderData = ReminderDataItem(
                title = _viewModel.reminderTitle.value,
                description = _viewModel.reminderDescription.value,
                location = _viewModel.reminderSelectedLocationStr.value,
                latitude = _viewModel.latitude.value,
                longitude = _viewModel.longitude.value
            )

            if (_viewModel.validateEnteredData(reminderData)) {
                reminder = reminderData
                checkLocationSettingsThenAddReminderGeofence()
            }

        }
    }

    private fun isForegroundLocationPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundLocationPermissionGranted(): Boolean {
        return if (isRunningQOrLater) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestLocationPermissions() {
        if (!isForegroundLocationPermissionsGranted()) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (!isBackgroundLocationPermissionGranted()) {
            permissionsLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            )
            return
        }
    }

    private fun checkLocationSettingsThenAddReminderGeofence() {
        if ( isForegroundLocationPermissionsGranted()
            && isBackgroundLocationPermissionGranted()
        ) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_LOW_POWER
            }

            val locationSettingsRequest = LocationSettingsRequest.Builder().apply {
                addLocationRequest(locationRequest)
            }.build()

            val settingClient = LocationServices.getSettingsClient(requireContext())

            settingClient.checkLocationSettings(locationSettingsRequest).apply {
                addOnSuccessListener {
                    addReminderGeofence()
                }

                addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try {
                            locationSettingsLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build())
                        } catch (e: IntentSender.SendIntentException) {
                            e.message?.let { message ->
                                Log.e(TAG, message)
                            }
                        }
                    } else {
                        Snackbar.make(
                            requireView(),
                            R.string.location_required_error,
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction(android.R.string.ok) {
                            checkLocationSettingsThenAddReminderGeofence()
                        }.show()
                    }
                }
            }
        } else {
            requestLocationPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addReminderGeofence() {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                GeofenceConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.saveReminder(reminder)
                Log.d(TAG, "Added geofence for reminder with id ${reminder.id}")
            }
            addOnFailureListener { exception ->
                Snackbar.make(
                    requireView(),
                    R.string.geofences_not_added,
                    Snackbar.LENGTH_INDEFINITE
                ).show()
                exception.message?.let { message ->
                    Log.e(TAG, message)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        private const val TAG = "SaveReminderFragment"
    }
}
