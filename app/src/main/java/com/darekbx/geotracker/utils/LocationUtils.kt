package com.darekbx.geotracker.utils

import android.content.Context
import android.location.LocationManager

object LocationUtils {

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isLocationEnabled && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}