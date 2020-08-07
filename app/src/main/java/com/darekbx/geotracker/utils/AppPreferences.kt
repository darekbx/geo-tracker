package com.darekbx.geotracker.utils

import android.content.Context
import com.darekbx.geotracker.GeoTrackerApplication
import com.darekbx.geotracker.location.ForegroundTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppPreferences @Inject constructor(@ApplicationContext val context: Context) {

    companion object {
        private val GPS_UPDATE_INTERVAL_KEY = "gps_update_interval_key"
        private val GPS_MIN_DISTANCE_KEY = "gps_min_distance_key"
    }

    var gpsUpdateInterval: Long
        get() = preferences.getLong(GPS_UPDATE_INTERVAL_KEY, ForegroundTracker.DEFAULT_UPDATES_INTERVAL)
        set(value) {
            preferences.edit().putLong(GPS_UPDATE_INTERVAL_KEY, value).apply()
        }

    var gpsMinDistance: Float
        get() = preferences.getFloat(GPS_MIN_DISTANCE_KEY, ForegroundTracker.DEFAULT_MIN_DISTANCE)
        set(value) {
            preferences.edit().putFloat(GPS_MIN_DISTANCE_KEY, value).apply()
        }

    private val preferences by lazy {
        context.getSharedPreferences(
            GeoTrackerApplication.LOG_TAG, Context.MODE_PRIVATE
        )
    }
}