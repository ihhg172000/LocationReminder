package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private var marker: Marker? = null

    private var permissionsLauncher: ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (
            permissions.values.all {
                it == true
            }
        ) {
            enableMyLocation()
            if (
                isForegroundLocationPermissionsGranted()
                && marker == null
            ) {
                checkLocationSettingsThenZoomToLastLocation()
            }
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
    private var locationSettingsLauncher :ActivityResultLauncher<IntentSenderRequest> =  registerForActivityResult(StartIntentSenderForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            zoomToLastLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.selectButton.setOnClickListener {
            if (marker == null) {
                Snackbar.make(
                    requireView(),
                    R.string.select_location,
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        initMapStyle()
        initMapView()
        initMapListeners()
        enableMyLocation()

    }

    private fun initMapStyle() {
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_retro_style)
            )
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "map style resource not found")
        }
    }

    private fun initMapView() {
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value
        val snippet = _viewModel.reminderSelectedLocationStr.value

        if (latitude != null && longitude != null && snippet != null) {
            val latLng = LatLng(latitude, longitude)
            marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .snippet(snippet)
            )

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }

        if (
            isForegroundLocationPermissionsGranted()
            && marker == null
        ) {
            checkLocationSettingsThenZoomToLastLocation()
        }
    }

    private fun initMapListeners() {
        map.setOnPoiClickListener { poi ->
            marker?.remove()

            val snippet = poi.name

            marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title("Selected Location")
                    .snippet(snippet)
            )

            _viewModel.selectedPOI.value = poi
            _viewModel.reminderSelectedLocationStr.value = snippet
            _viewModel.latitude.value = poi.latLng.latitude
            _viewModel.longitude.value = poi.latLng.longitude
        }

        map.setOnMapClickListener { position ->
            marker?.remove()

            val snippet = "${position.latitude.toFloat()} , ${position.longitude.toFloat()}"

            marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("Selected Location")
                    .snippet(snippet)
            )

            _viewModel.reminderSelectedLocationStr.value = snippet
            _viewModel.latitude.value = position.latitude
            _viewModel.longitude.value = position.longitude

        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (
            isForegroundLocationPermissionsGranted()
        ) {
            map.isMyLocationEnabled = true
            map.setOnMyLocationButtonClickListener {
                checkLocationSettingsThenZoomToLastLocation()
                true
            }
        } else {
            requestForegroundLocationPermissions()
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

    private fun requestForegroundLocationPermissions() {
        if (!isForegroundLocationPermissionsGranted()) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
    }

    private fun checkLocationSettingsThenZoomToLastLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val locationSettingsRequest = LocationSettingsRequest.Builder().apply {
            addLocationRequest(locationRequest)
        }.build()

        val settingClient = LocationServices.getSettingsClient(requireContext())

        settingClient.checkLocationSettings(locationSettingsRequest).apply {
            addOnSuccessListener {
                zoomToLastLocation()
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
                        checkLocationSettingsThenZoomToLastLocation()
                    }.show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun zoomToLastLocation() {
        LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
            if (it == null) {
                zoomToLastLocation()
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
            }
        }
    }

    companion object {
        private const val TAG = "SelectLocationFragment"
    }
}
