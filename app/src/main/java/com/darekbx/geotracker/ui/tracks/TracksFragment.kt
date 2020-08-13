package com.darekbx.geotracker.ui.tracks

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.utils.LocationUtils
import com.darekbx.geotracker.utils.PermissionRequester
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracks.*
import javax.inject.Inject

@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks) {

    @Inject
    lateinit var appPreferences: AppPreferences

    private val tracksViewModel: TrackViewModel by viewModels()

    private var currentTrackId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerViewModel()
        handleStopRecordActions()

        tracks_list.adapter = trackAdapter
        tracks_list.layoutManager = LinearLayoutManager(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTrackingService()
    }

    override fun onResume() {
        super.onResume()
        context?.let { context ->
            checkLocationEnabled(context)
        }
    }

    private fun checkLocationEnabled(context: Context) {
        val locationEnabled = LocationUtils.isLocationEnabled(context)
        button_record.isEnabled = locationEnabled
        if (!locationEnabled) {
            AlertDialog.Builder(context)
                .setMessage(R.string.location_is_not_enabled)
                .setPositiveButton(R.string.button_ok, null)
                .show()
        }
    }

    private fun registerViewModel() {
        tracksViewModel.tracks.observe(viewLifecycleOwner, Observer { tracks ->
            displayTracks(tracks)
        })

        tracksViewModel.newTrackid.observe(viewLifecycleOwner, Observer { trackId ->
            startTracking(trackId)
            tracksViewModel.subscribeToPoints(trackId)
            currentTrackId = trackId

            observePointsChanges()
        })

        tracksViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            setUIMode(isRecording = false)
        })
    }

    private fun handleStopRecordActions() {
        button_record.setOnClickListener {
            fineLocation.runWithPermission {
                tracksViewModel.createNewTrack()
                setUIMode(isRecording = true)
            }
        }

        button_stop.setOnClickListener {
            stopTracking()
        }

        button_stop.hide()
    }

    private fun saveTrack(label: String?) {
        currentTrackId?.let { currentTrackId ->
            tracksViewModel.updateTrack(currentTrackId, label, 0.0F)
        }
    }

    private fun observePointsChanges() {
        tracksViewModel.recordStatus?.observe(viewLifecycleOwner, Observer { recordStatus ->
            recording_status.setText(
                getString(
                    R.string.points_format,
                    recordStatus.pointsCount,
                    recordStatus.distance
                )
            )
        })
    }

    private fun displayTracks(tracks: List<Track>) {
        trackAdapter.items = tracks
    }

    private fun startTracking(trackId: Long) {
        val intent = Intent(context, ForegroundTracker::class.java).apply {
            putExtra(ForegroundTracker.TRACK_ID_KEY, trackId)
        }
        context?.startForegroundService(intent)
    }

    private fun stopTracking() {
        stopTrackingService()
        SaveTrackDialog().apply {
            saveCallback = { label ->
                saveTrack(label)
            }
        }.show(parentFragmentManager, SaveTrackDialog::class.java.simpleName)
    }

    private fun stopTrackingService() {
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

    private fun openTrack(trackId: Long) {
        val arguments = Bundle(1).apply {
            putLong(TrackFragment.TRACK_ID_KEY, trackId)
        }
        findNavController()
            .navigate(R.id.action_tracksFragment_to_trackFragment, arguments)
    }

    private fun setUIMode(isRecording: Boolean) {
        when {
            isRecording -> {
                button_stop.show()
                button_record.hide()
                status_container.visibility = View.VISIBLE
            }
            else -> {
                button_stop.hide()
                button_record.show()
                status_container.visibility = View.GONE
            }
        }
    }

    private val fineLocation by lazy {
        PermissionRequester(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            onDenied = { showPermissionsDeniedDialog() }
        )
    }

    private val trackAdapter by lazy {
        TrackAdapter(context).apply {
            onTrackClick = { track ->
                track.id?.let { trackId ->
                    openTrack(trackId)
                }
            }
        }
    }
}