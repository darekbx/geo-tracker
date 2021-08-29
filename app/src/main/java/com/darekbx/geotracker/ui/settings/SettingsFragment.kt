package com.darekbx.geotracker.ui.settings

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.darekbx.geotracker.R
import com.darekbx.geotracker.repository.AppDatabase
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.utils.PermissionRequester
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

        findPreference<Preference>(getString(R.string.settings_backup_button_key))?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                storagePermission.runWithPermission {
                    makeBackup()
                }
                false
            }

        findPreference<Preference>(getString(R.string.settings_restore_button_key))
            ?.setOnPreferenceClickListener {
                storagePermission.runWithPermission {
                    restoreDataFromBackup()
                }
                false
            }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
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
            AppPreferences.ACTIVITY_DETECTION_KEY -> {
                val defaultValue = resources.getBoolean(R.bool.default_activity_detection)
                appPreferences.activityDetection =
                    sharedPreferences?.getBoolean(key, defaultValue) ?: defaultValue
            }
            AppPreferences.NTH_POINTS_TO_SKIP -> {
                val defaultValue = resources.getInteger(R.integer.default_nth_points_to_skip)
                appPreferences.nthPointsToSkip =
                    sharedPreferences?.getString(key, "$defaultValue")?.toInt() ?: defaultValue
            }
            AppPreferences.LIVE_TRACKING_KEY -> {
                val defaultValue = resources.getBoolean(R.bool.default_live_tracking)
                appPreferences.liveTracking =
                    sharedPreferences?.getBoolean(key, defaultValue) ?: defaultValue
            }
        }
    }

    private fun makeBackup() {
        AppDatabase.makeBackup(requireContext()) { backupFilePath ->
            CoroutineScope(Dispatchers.Main).launch {
                val message = when(backupFilePath) {
                    null -> getString(R.string.settings_backup_error)
                    else -> getString(R.string.settings_backup_success, backupFilePath)
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun restoreDataFromBackup() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "file/*"
        }
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.path?.let { filePath ->
                doRestoreData(filePath)
            }
        }.launch(intent)
    }

    private fun doRestoreData(filePath: String) {
        AppDatabase.restoreDataFromBackup(requireContext(), filePath) { result ->
            CoroutineScope(Dispatchers.Main).launch {
                val message = when {
                    result -> R.string.settings_restore_success
                    else -> R.string.settings_restore_error
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermissionsDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.permissions_are_required)
            .setPositiveButton(R.string.button_ok, null)
            .show()
    }

    private val storagePermission by lazy {
        PermissionRequester(
            activity,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            onDenied = { showPermissionsDeniedDialog() }
        )
    }
}
