package com.delayre.jerrol.mappsgroovy.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.delayre.jerrol.mappsgroovy.R
import com.delayre.jerrol.mappsgroovy.google.LocationService
import com.delayre.jerrol.mappsgroovy.tools.BaseActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : BaseActivity(), OnMapReadyCallback, View.OnClickListener {

    companion object {
        private val TAG = MapsActivity::class.java.simpleName
    }

    private lateinit var mMap: GoogleMap
    private var mLocationService: LocationService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mLocationService = LocationService(this)
        mLocationService?.updateValuesFromBundle(savedInstanceState)

        fab_location.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        mLocationService?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mLocationService?.onPause()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        /*val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))*/
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        mLocationService?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mLocationService?.onActivityResult(requestCode, resultCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mLocationService?.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onClick(v: View?) {
        mLocationService?.startLocationUpdates()
    }

    fun updateMarker(latitude: Double, longitude: Double) {
        mMap.clear()

        val latLng = LatLng(latitude, longitude)
        mMap.addMarker(MarkerOptions().position(latLng))
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }
}
