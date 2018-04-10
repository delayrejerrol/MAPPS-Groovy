package com.delayre.jerrol.mappsgroovy.google

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.delayre.jerrol.mappsgroovy.BuildConfig
import com.delayre.jerrol.mappsgroovy.R
import com.delayre.jerrol.mappsgroovy.tools.Logs
import com.delayre.jerrol.mappsgroovy.ui.MapsActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.text.DateFormat
import java.util.*
import com.google.android.gms.location.LocationRequest




class LocationService(private val activity: AppCompatActivity) {

    companion object {
        private val TAG = LocationService::class.java.simpleName

        /**
         * Code used in requesting runtime permissions.
         */
        private const val REQUEST_PERMISSION_REQUEST_CODE = 42

        /**
         * Constant used in the location settings dialog.
         */
        private const val REQUEST_CHECK_SETTINGS = 0x1

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECODS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

        // Keys for storing activity state in the Bundle.
        private const val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
        private const val KEY_LOCATION = "location"
        private const val KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string"
    }

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

    /**
     * Provides access to the Location Settings API.
     */
    private var mSettingsClient: SettingsClient = LocationServices.getSettingsClient(activity)

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private lateinit var mLocationRequest: LocationRequest

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determin if the device has optimal location settings.
     */
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest

    /**
     * Callback for Location events.
     */
    private var mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return

            mCurrentLocation = locationResult.lastLocation
            mLastUpdateTime = DateFormat.getDateTimeInstance().format(Date())
            updateLocationData()

            if (activity is MapsActivity) {
                if (mCurrentLocation != null) {
                    activity.updateMarker(mCurrentLocation?.latitude!!, mCurrentLocation?.longitude!!)
                    stopLocationUpdate()
                }
            }
        }
    }

    /**
     * Represents a geographical location.
     */
    private var mCurrentLocation: Location? = null

    // Labels
    private var mLatitudeLabel: String = activity.getString(R.string.latitude_label)
    private var mLongitudeLabel: String = activity.getString(R.string.longitude_label)
    private var mLastUpdateTimeLabel: String = activity.getString(R.string.last_update_time_label)

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    private var mRequestingLocationUpdates: Boolean = false

    /**
     * Time when the location was updated represented as a String.
     */
    private var mLastUpdateTime: String = ""

    init {
        createLocationRequest()
        buildLocationSettingsRequest()
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
        // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
        if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES)
        }

        KEY_REQUESTING_LOCATION_UPDATES.let {
            if (savedInstanceState.keySet().contains(it)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(KEY_REQUESTING_LOCATION_UPDATES)
            }
        }

        // Update the value of mCurrentLocation from the Bundle and update the UI to show the
        // corrent latitude and longitude.
        if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
            // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
            // is not null.
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        }

        // Update the value of mLastUpdateTime from the Bundle and update the UI.
        if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
            mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING)
        }
        updateLocationData()
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECODS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mLocationRequest = locationRequest
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest)
        mLocationSettingsRequest = builder.build()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        when (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    Logs.i(TAG, "User agreed to make required location settings changes.")
                    // Nothing to do. startLocationupdates() gets called in onResume again.
                }
                Activity.RESULT_CANCELED -> {
                    Logs.i(TAG, "User chose not to make required location settings changes.")
                    mRequestingLocationUpdates = false
                    updateLocationData()
                }
            }
        }
    }


    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true

            // Begin by checking if the device has the necessary location settings.
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(activity) {
                        Logs.i(TAG, "All location settings are satisfied.")

                        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, /*Looper: Looper.myLooper()*/ null)
                    }
                    .addOnFailureListener(activity) {
                        val statusCode = (it as ApiException).statusCode
                        when (statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                Logs.i(TAG, "Location settings are not satisfied. Attempting to upgrade location settings")
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    val rae = it as ResolvableApiException
                                    rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS)
                                } catch (sie: IntentSender.SendIntentException) {
                                    Logs.i(TAG, "PendingIntent unable to execute request.")
                                }
                            }
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                val errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings."
                                Logs.e(TAG, errorMessage)
                                mRequestingLocationUpdates = false
                            }
                        }

                        updateLocationData()
                    }
        }
    }

    fun updateLocationData() {
        if (mRequestingLocationUpdates) {
            Logs.i(TAG, "Location update start...")
        } else {
            Logs.i(TAG, "Location update stopped...")
        }

        if (mCurrentLocation != null) {
            Logs.i(TAG, "$mLatitudeLabel: ${mCurrentLocation?.latitude}")
            Logs.i(TAG, "$mLongitudeLabel: ${mCurrentLocation?.longitude}")
            Logs.i(TAG, "$mLastUpdateTimeLabel: $mLastUpdateTime")
        }
    }

    fun stopLocationUpdate() {
        if (!mRequestingLocationUpdates) {
            Logs.d(TAG, "stopLocationUpdate: updates never requested, no-op.")
            return
        }

        // It is a good practice to remove location requests when the activityis in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(activity, {
                    mRequestingLocationUpdates = false
                })
    }

    fun onResume() {
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates and checkPermission() ) {
            startLocationUpdates()
        } else if(!checkPermission()) {
            requestPermissions()
        }

        updateLocationData()
    }

    fun onPause() {
        // Remove location updates to save battery
        stopLocationUpdate()
    }

    fun onSaveInstanceState(outState: Bundle?) {
        outState?.putBoolean(KEY_REQUESTING_LOCATION_UPDATES,
                mRequestingLocationUpdates)
        outState?.putParcelable(KEY_LOCATION, mCurrentLocation)
        outState?.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime)
    }

    private fun showSnackbar(mainTextStringId: Int, actionStringId: Int, onClickListener: View.OnClickListener) {
        Snackbar.make(activity.findViewById(android.R.id.content),
                activity.getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(activity.getString(actionStringId), onClickListener)
                .show()
    }

    /**
     * Return the current state of the permissions needed.
     */
    private fun checkPermission(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION)

        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)

        // Provide an additional rationale to the user. This would happen if the user denied
        // the request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Logs.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, View.OnClickListener {
                ActivityCompat.requestPermissions(activity,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSION_REQUEST_CODE)
            })
        } else {
            Logs.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(activity,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_REQUEST_CODE)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        Logs.i(TAG, "onRequestPermissionsResult")
        if (requestCode == REQUEST_PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                // If user interaction was interrupted, the permission request is cancelled
                // and you receive empty arrays.
                Logs.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates.")
                    startLocationUpdates()
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. I a real app, core permission would
                // typically be best requested during a welcome-screen flow.

                // Additionally it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, View.OnClickListener {
                    // Build intent that displays the App settings screen
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package",
                            BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    activity.startActivity(intent)
                })
            }
        }
    }
}