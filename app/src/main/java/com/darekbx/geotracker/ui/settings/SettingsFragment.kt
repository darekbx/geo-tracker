package com.darekbx.geotracker.ui.tracks

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.darekbx.geotracker.R
import com.darekbx.geotracker.utils.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey);
    }

    override fun onResume() {
        super.onResume()
        getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            AppPreferences.NOTIFICATION_MIN_DISTANCE_KEY -> {
                val defaultValue = resources.getInteger(R.integer.default_min_notification_distance).toFloat()
                appPreferences.gpsNotificationMinDistance =
                    sharedPreferences?.getString(key, "$defaultValue")?.toFloat() ?: defaultValue
            }
            AppPreferences.GPS_MIN_DISTANCE_KEY -> {
                val defaultValue = resources.getInteger(R.integer.default_gps_distance).toFloat()
                appPreferences.gpsMinDistance =
                    sharedPreferences?.getString(key, "$defaultValue")?.toFloat() ?: defaultValue
            }
            AppPreferences.GPS_UPDATE_INTERVAL_KEY -> {
                val defaultValue = resources.getInteger(R.integer.default_gps_interval).toLong()
                appPreferences.gpsUpdateInterval =
                    sharedPreferences?.getString(key, "$defaultValue")?.toLong() ?: defaultValue
            }
        }
    }
}
