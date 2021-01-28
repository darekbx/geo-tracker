package com.darekbx.geotracker.utils

import android.content.Context
import com.darekbx.geotracker.GeoTrackerApplication
import com.darekbx.geotracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppPreferences @Inject constructor(@ApplicationContext val context: Context) {

    companion object {
        val GPS_UPDATE_INTERVAL_KEY = "gps_update_interval_key"
        val GPS_MIN_DISTANCE_KEY = "gps_min_distance_key"
        val NOTIFICATION_MIN_DISTANCE_KEY = "notification_min_distance"
        val LIVE_TRACKING_KEY = "live_tracking"
    }

    var gpsUpdateInterval: Long
        get() {
            val defaultValue = context.resources.getInteger(R.integer.default_gps_interval)
            return preferences.getLong(GPS_UPDATE_INTERVAL_KEY, defaultValue.toLong())
        }
        set(value) {
            preferences.edit().putLong(GPS_UPDATE_INTERVAL_KEY, value).apply()
        }

    var gpsMinDistance: Float
        get() {
            val defaultValue = context.resources.getInteger(R.integer.default_gps_distance)
            return preferences.getFloat(GPS_MIN_DISTANCE_KEY, defaultValue.toFloat())
        }
        set(value) {
            preferences.edit().putFloat(GPS_MIN_DISTANCE_KEY, value).apply()
        }

    var gpsNotificationMinDistance: Float
        get() {
            val defaultValue = context.resources.getInteger(R.integer.default_min_notification_distance)
            return preferences.getFloat(NOTIFICATION_MIN_DISTANCE_KEY, defaultValue.toFloat())
        }
        set(value) {
            preferences.edit().putFloat(NOTIFICATION_MIN_DISTANCE_KEY, value).apply()
        }

    var liveTracking: Boolean
        get() {
            val defaultValue = context.resources.getBoolean(R.bool.default_live_tracking)
            return preferences.getBoolean(LIVE_TRACKING_KEY, defaultValue)
        }
        set(value) {
            preferences.edit().putBoolean(LIVE_TRACKING_KEY, value).apply()
        }

    private val preferences by lazy {
        context.getSharedPreferences(
            GeoTrackerApplication.LOG_TAG, Context.MODE_PRIVATE
        )
    }
}
