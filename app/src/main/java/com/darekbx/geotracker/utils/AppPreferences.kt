package com.darekbx.geotracker.utils

import android.content.Context
import com.darekbx.geotracker.GeoTrackerApplication
import com.darekbx.geotracker.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppPreferences @Inject constructor(@ApplicationContext val context: Context) {

    companion object {
        const val GPS_UPDATE_INTERVAL_KEY = "gps_update_interval_key"
        const val GPS_MIN_DISTANCE_KEY = "gps_min_distance_key"
        const val NOTIFICATION_MIN_DISTANCE_KEY = "notification_min_distance"
        const val LIVE_TRACKING_KEY = "live_tracking"
        const val ACTIVITY_DETECTION_KEY = "activity_detection"
        const val NTH_POINTS_TO_SKIP = "nth_points_to_skip"
    }

    var nthPointsToSkip: Int
        get() {
            val defaultValue = context.resources.getInteger(R.integer.default_nth_points_to_skip)
            return preferences.getInt(NTH_POINTS_TO_SKIP, defaultValue)
        }
        set(value) {
            preferences.edit().putInt(NTH_POINTS_TO_SKIP, value).apply()
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
            val defaultValue =
                context.resources.getInteger(R.integer.default_min_notification_distance)
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

    var activityDetection: Boolean
        get() {
            val defaultValue = context.resources.getBoolean(R.bool.default_activity_detection)
            return preferences.getBoolean(ACTIVITY_DETECTION_KEY, defaultValue)
        }
        set(value) {
            preferences.edit().putBoolean(ACTIVITY_DETECTION_KEY, value).apply()
        }

    private val preferences by lazy {
        context.getSharedPreferences(
            GeoTrackerApplication.LOG_TAG, Context.MODE_PRIVATE
        )
    }
}
