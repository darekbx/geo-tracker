package com.darekbx.geotracker.ui.tracks

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.model.Track
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
        stopTracking()
    }

    private fun registerViewModel() {
        tracksViewModel.tracks.observe(viewLifecycleOwner, Observer { tracks ->
            displayTracks(tracks)
        })

        tracksViewModel.newTrackid.observe(viewLifecycleOwner, Observer { trackId ->
            startTracking(trackId)
            tracksViewModel.subscribeToPoints(trackId)
            currentTrackId = trackId


            // TODO: remove
            tracksViewModel.points?.observe(viewLifecycleOwner, Observer { points ->
                log.setText("TrackId: $currentTrackId, points: ${points.size}, speed: ${(points.lastOrNull()?.speed ?: 0F) * 3.6F}")
            })

        })
    }

    private fun handleStopRecordActions() {
        button_record.setOnClickListener {
            fineLocation.runWithPermission {
                tracksViewModel.createNewTrack()
                setFloatingButtonMode(isRecording = true)
            }
        }

        button_stop.setOnClickListener {
            stopTracking()
            setFloatingButtonMode(isRecording = false)
        }

        button_stop.hide()
    }

    private fun displayTracks(tracks: List<Track>) {
        trackAdapter.items = tracks
    }

    private fun startTracking(trackId: Long) {
        val intent = Intent(context, ForegroundTracker::class.java).apply {
            putExtra(ForegroundTracker.TRACK_ID_KEY, trackId)
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

    private fun setFloatingButtonMode(isRecording: Boolean) {
        when {
            isRecording -> {
                button_stop.show()
                button_record.hide()
            }
            else -> {
                button_stop.hide()
                button_record.show()
            }
        }
    }

    private val fineLocation by lazy {
        PermissionRequester(activity, Manifest.permission.ACCESS_FINE_LOCATION,
            onDenied = { showPermissionsDeniedDialog() },
            onRationale = { showPermissionsDeniedDialog() })
    }

    private val trackAdapter by lazy { TrackAdapter(context) }
}