package com.darekbx.geotracker.ui.tracks

import android.Manifest
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.utils.DateTimeUtils
import com.darekbx.geotracker.utils.LocationUtils
import com.darekbx.geotracker.utils.PermissionRequester
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracks.*
import javax.inject.Inject

@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks) {

    companion object {
        val STOP_ACTION = "stop_action"
    }

    @Inject
    lateinit var appPreferences: AppPreferences

    private val tracksViewModel: TrackViewModel by viewModels()

    private var currentTrackId: Long? = null

    private val stopBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            data?.takeIf { it.action == STOP_ACTION }?.let {
                stopTracking()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collapsing_toolbar.setCollapsedTitleTextColor(Color.WHITE)
        collapsing_toolbar.setExpandedTitleColor(Color.WHITE)
        toolbar.setTitle(R.string.app_name)

        registerViewModel()
        handleStopRecordActions()

        tracksViewModel.fetchTracks()

        button_all_tracks.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_allTracksFragment)
        }

        button_settings.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_settingsFragment)
        }

        tracks_list.setAdapter(trackAdapter)

        activity?.registerReceiver(stopBroadcast, IntentFilter(STOP_ACTION))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTrackingService()

        try {
            stopBroadcast?.let {
                activity?.unregisterReceiver(stopBroadcast)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            displaySummary(tracks.flatMap { it.value })
        })

        tracksViewModel.newTrackid.observe(viewLifecycleOwner, Observer { trackId ->
            if (status_container.isVisible) {
                startTracking(trackId)
                tracksViewModel.subscribeToPoints(trackId)
                currentTrackId = trackId

                observePointsChanges()
            }
        })

        tracksViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            setUIMode(isRecording = false)
        })
    }

    private fun handleStopRecordActions() {
        button_record.setOnClickListener {
            fineLocation.runWithPermission {
                setUIMode(isRecording = true)
                tracksViewModel.createNewTrack()
            }
        }

        button_stop.setOnClickListener {
            stopTracking()
        }

        button_stop.hide()
    }

    private fun saveTrack(label: String?) {
        currentTrackId?.let { currentTrackId ->
            tracksViewModel.updateTrack(currentTrackId, label)
            tracksViewModel.fetchTracks()
        }
    }

    private fun observePointsChanges() {
        tracksViewModel.recordStatus?.observe(viewLifecycleOwner, Observer { recordStatus ->
            recording_status.setText(
                getString(
                    R.string.points_format,
                    recordStatus.pointsCount,
                    recordStatus.distance,
                    recordStatus.averageSpeed,
                    DateTimeUtils.getFormattedTime(recordStatus.time.toInt())
                )
            )
        })
    }

    private fun displayTracks(tracks: Map<String?, List<Track>>) {
        trackAdapter.items = tracks
        tracks_list.expandGroup(0)
    }

    private fun displaySummary(tracks: List<Track>) {
        val count = tracks.size
        val totalDistance = tracks.sumByDouble { it.distance.toDouble() }
        val totalTime = tracks.sumBy {
            it.timeDifference?.takeIf { it.isNotEmpty() }?.let { difference ->
                val chunks = difference.split(" ")
                val hours = chunks[0].removeSuffix("h").toIntOrNull() ?: 0
                val minutes = chunks[1].removeSuffix("m").toIntOrNull() ?: 0
                val seconds = chunks[2].removeSuffix("s").toIntOrNull() ?: 0
                hours * 60 * 60 + minutes * 60 + seconds
            } ?: 0
        }
        sum_count.setText("$count")
        sum_distance.setText(getString(R.string.distance_format, totalDistance))
        sum_time.setText("${DateTimeUtils.getFormattedTime(totalTime)}")
    }

    private fun startTracking(trackId: Long) {
        val intent = Intent(context, ForegroundTracker::class.java).apply {
            putExtra(ForegroundTracker.TRACK_ID_KEY, trackId)
        }
        context?.startForegroundService(intent)
    }

    private fun stopTracking() {
        stopTrackingService()

        when (isStateSaved) {
            true -> saveTrack("Unknown")
            else -> {
                SaveTrackDialog().apply {
                    saveCallback = { label ->
                        saveTrack(label)
                    }
                }.show(parentFragmentManager, SaveTrackDialog::class.java.simpleName)
            }
        }
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

    private fun deleteTrackConfirmation(track: Track) {
        context?.let { context ->
            AlertDialog.Builder(context)
                .setMessage(getString(R.string.delete_message, track.label))
                .setNegativeButton(R.string.delete_no, null)
                .setPositiveButton(R.string.delete_yes, object: DialogInterface.OnClickListener{
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        track.id?.let { trackId ->
                            tracksViewModel.deleteTrack(trackId)
                            tracksViewModel.fetchTracks()
                        }
                    }
                })
                .show()
        }
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
            onTrackLongClick = { track ->
                deleteTrackConfirmation(track)
            }
        }
    }
}
