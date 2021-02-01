package com.darekbx.geotracker.ui.tracks

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.darekbx.geotracker.R
import com.darekbx.geotracker.repository.AppDatabase
import com.darekbx.geotracker.utils.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        findPreference<Preference>(getString(R.string.settings_backup_button_key))
            ?.setOnPreferenceClickListener(object : Preference.OnPreferenceClickListener {
                override fun onPreferenceClick(preference: Preference?): Boolean {
                    makeBackup()
                    return false
                }
            })
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
            AppPreferences.LIVE_TRACKING_KEY -> {
                val defaultValue = resources.getBoolean(R.bool.default_live_tracking)
                appPreferences.liveTracking =
                    sharedPreferences?.getBoolean(key, defaultValue) ?: defaultValue
            }
        }
    }

    private fun makeBackup() {
        AppDatabase.makeBackup(requireContext()) { result ->
            CoroutineScope(Dispatchers.Main).launch {
                val message = when {
                    result -> R.string.settings_backup_success
                    else -> R.string.settings_backup_error
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
