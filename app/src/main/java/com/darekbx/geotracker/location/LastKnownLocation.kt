package com.darekbx.geotracker.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

@SuppressLint("MissingPermission")
class LastKnownLocation(private val context: Context) {

    companion object {
        private const val DUMMY_LOCATION = true
    }

    fun getLocation(): Location? {
        if (DUMMY_LOCATION) {
            return Location("test_provider").apply {
                latitude = 52.327
                longitude = 21.017
            }
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }
}