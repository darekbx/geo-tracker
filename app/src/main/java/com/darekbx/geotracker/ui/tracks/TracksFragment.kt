package com.darekbx.geotracker.ui.tracks

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.utils.PermissionRequester
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracks.*
import javax.inject.Inject

@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks) {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_record.setOnClickListener {
            fineLocation.runWithPermission {
                startTracking()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        stopTracking()
    }

    private fun startTracking() {
        val intent = Intent(context, ForegroundTracker::class.java).apply {
            putExtra(ForegroundTracker.TRACK_ID_KEY, 10)
        }
        context?.startService(intent)
    }

    private fun stopTracking() {
        val intent = Intent(context, ForegroundTracker::class.java)
        context?.stopService(intent)
    }

    private fun showPermissionsDeniedDialog() {
        context?.let { context ->
            AlertDialog.Builder(context)
                .setMessage(R.string.permissions_are_required)
                .setPositiveButton(R.string.button_ok, null)
                .show()
        }
    }

    private val fineLocation by lazy {
        PermissionRequester(activity, Manifest.permission.ACCESS_FINE_LOCATION,
            onDenied = { showPermissionsDeniedDialog() },
            onRationale = { showPermissionsDeniedDialog() })
    }
}