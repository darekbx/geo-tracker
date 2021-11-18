package com.darekbx.geotracker.ui.tracks

import android.Manifest
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.location.ForegroundTracker.Companion.TRACK_ID_KEY
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.entities.SimplePointDto
import com.darekbx.geotracker.ui.track.TrackFragment
import com.darekbx.geotracker.utils.DateTimeUtils
import com.darekbx.geotracker.utils.LocationUtils
import com.darekbx.geotracker.utils.PermissionRequester
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracks.*
import kotlinx.android.synthetic.main.fragment_tracks.loading_view
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks) {

    companion object {
        const val STOP_ACTION = "stop_action"
        const val DEFAULT_MAP_ZOOM = 16.0
    }

    private val tracksViewModel: TrackViewModel by viewModels()
    private var currentTrackId: Long? = null
    private var miniMapMarker: Marker? = null

    private val stopBroadcast = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            data?.takeIf { it.action == STOP_ACTION }?.let {
                stopTracking()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        button_calendar.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_activityCalendarFragment)
        }

        button_places_to_visit.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_placesToVisitFragment)
        }

        tracks_list.setAdapter(trackAdapter)

        activity?.registerReceiver(stopBroadcast, IntentFilter(STOP_ACTION))

        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_record_scale)
        recording_animation.startAnimation(animation)

        subscribeToActiveTrack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTrackingService()

        try {
            activity?.unregisterReceiver(stopBroadcast)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocationEnabled(requireContext())
        mini_map.onResume()
    }

    override fun onPause() {
        super.onPause()
        mini_map.onPause()
    }

    private fun subscribeToActiveTrack() {
        activity?.intent?.extras?.getLong(TRACK_ID_KEY)?.let { trackId ->
            setUIMode(isRecording = true)
            tracksViewModel.subscribeToPoints(trackId)
            currentTrackId = trackId
            observePointsChanges()
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

                startMiniMap()
                observePointsChanges()
            }
        })

        tracksViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            setUIMode(isRecording = false)
        })

        tracksViewModel.trackLoadingStatus.observe(viewLifecycleOwner, Observer { isLoading ->
            loading_view.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
    }

    private fun handleStopRecordActions() {
        button_record.setOnClickListener {
            fineLocation.runWithPermission {
                backgroundLocation.runWithPermission {
                    setUIMode(isRecording = true)
                    tracksViewModel.createNewTrack()
                }
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

    private fun discardTrack() {
        currentTrackId?.let { currentTrackId ->
            stopTrackingService()
            setUIMode(false)
            deleteTrackConfirmation(
                Track(currentTrackId, "Not saved track", "", "", "", 1F)
            )
        }
    }

    private fun startMiniMap() {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        mini_map.setTileSource(TileSourceFactory.MAPNIK)
        mini_map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        mini_map.controller.setZoom(DEFAULT_MAP_ZOOM)
        mini_map.setMultiTouchControls(true)

        miniMapMarker = Marker(mini_map).apply {
            icon = resources.getDrawable(R.drawable.bg_seekbar_thumb, context?.theme)
        }

        fetchTracksWithPoints()
    }

    private fun fetchTracksWithPoints() {
        mini_map.overlays.clear()
        tracksViewModel.fetchAllPoints().observe(viewLifecycleOwner, Observer { grouppedPoints ->
            for (points in grouppedPoints) {
                displayTrack(points.value)
            }
            mini_map.invalidateMapCoordinates(mini_map.projection.screenRect)
            mini_map.overlays.add(miniMapMarker)
        })
    }

    private fun displayTrack(points: List<SimplePointDto>, color: Int = Color.RED) {
        val polyline = Polyline().apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = 6.0F
        }

        val mapPoints = points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
        mini_map.overlays.add(polyline)
    }

    private fun observePointsChanges() {
        tracksViewModel.recordStatus?.observe(viewLifecycleOwner, Observer { recordStatus ->
            distance_value.text = getString(R.string.distance_format, recordStatus.distance)
            speed_value.text = getString(R.string.speed_format, recordStatus.speed * 3.6F)
            avg_speed_value.text = getString(R.string.speed_format, recordStatus.averageSpeed * 3.6F)
            time_value.text = DateTimeUtils.getFormattedTime(recordStatus.time.toInt())

            if (miniMapMarker != null && recordStatus.location != null) {
                mini_map.controller.apply {
                    miniMapMarker?.position = recordStatus?.location
                    miniMapMarker?.icon = resources.getDrawable(R.drawable.bg_seekbar_thumb, context?.theme)
                    miniMapMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setCenter(recordStatus.location)
                }
            }
        })
    }

    private fun displayTracks(tracks: Map<String?, List<Track>>) {
        trackAdapter.items = tracks
        tracks_list.expandGroup(0)
    }

    private fun displaySummary(tracks: List<Track>) {
        var totalDistance = 0.0F
        for (track in tracks) {
            totalDistance += track.distance
        }
        val totalTime = tracks.sumBy { track ->
            track.timeDifference?.takeIf { it.isNotEmpty() }?.let { difference ->
                val chunks = difference.split(" ")
                val hours = chunks[0].removeSuffix("h").toIntOrNull() ?: 0
                val minutes = chunks[1].removeSuffix("m").toIntOrNull() ?: 0
                val seconds = chunks[2].removeSuffix("s").toIntOrNull() ?: 0
                hours * 60 * 60 + minutes * 60 + seconds
            } ?: 0
        }
        sum_distance.text = getString(R.string.distance_format, totalDistance)
        sum_time.text = DateTimeUtils.getFormattedTime(totalTime, showSeconds = true)
    }

    private fun startTracking(trackId: Long) {
        val intent = Intent(context, ForegroundTracker::class.java).apply {
            putExtra(TRACK_ID_KEY, trackId)
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
                    discardCallback = {
                        discardTrack()
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
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.permissions_are_required)
            .setPositiveButton(R.string.button_ok, null)
            .show()
    }

    private fun openTrack(trackId: Long) {
        if (status_container.visibility == View.VISIBLE) {
            return
        }
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
                .setNegativeButton(R.string.delete_no, { _, _ -> tracksViewModel.fetchTracks() })
                .setPositiveButton(R.string.delete_yes) { _, _ ->
                    track.id?.let { trackId ->
                        tracksViewModel.deleteTrack(trackId) {
                            tracksViewModel.fetchTracks()
                        }
                    }
                }
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
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            onDenied = { showPermissionsDeniedDialog() }
        )
    }

    private val backgroundLocation by lazy {
        PermissionRequester(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE
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
