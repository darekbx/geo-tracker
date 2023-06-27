package com.darekbx.geotracker.ui.tracks

import android.Manifest
import android.content.*
import android.database.Cursor
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.graphics.StrokeJoin
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.FragmentTracksBinding
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.location.ForegroundTracker.Companion.TRACK_ID_KEY
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.entities.SimplePointDto
import com.darekbx.geotracker.ui.track.TrackFragment
import com.darekbx.geotracker.utils.DateTimeUtils
import com.darekbx.geotracker.utils.LocationUtils
import com.darekbx.geotracker.utils.PermissionRequester
import com.darekbx.geotracker.viewmodels.PlacesToVisitViewModel
import com.darekbx.geotracker.viewmodels.RoutesViewModel
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.InputStream

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class TracksFragment : Fragment(R.layout.fragment_tracks) {

    private var _binding: FragmentTracksBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val STOP_ACTION = "stop_action"
        const val DEFAULT_MAP_ZOOM = 16.0
        const val PICK_GPX_FILE = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTracksBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val tracksViewModel: TrackViewModel by viewModels()
    private val placesToVisitViewModel: PlacesToVisitViewModel by viewModels()
    private val routesViewModel: RoutesViewModel by viewModels()
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

        binding.buttonAllTracks.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_allTracksFragment)
        }

        binding.buttonSettings.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_settingsFragment)
        }

        binding.buttonCalendar.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_activityCalendarFragment)
        }

        binding.buttonPlacesToVisit.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_placesToVisitFragment)
        }

        binding.buttonRoutes.setOnClickListener {
            findNavController()
                .navigate(R.id.action_tracksFragment_to_routesFragment)
        }

        binding.tracksList.setAdapter(trackAdapter)

        activity?.registerReceiver(stopBroadcast, IntentFilter(STOP_ACTION))

        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_record_scale)
        binding.recordingAnimation.startAnimation(animation)

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
        binding.miniMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.miniMap.onPause()
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
        binding.buttonRecord.isEnabled = locationEnabled
        binding.buttonRecordGpx.isEnabled = locationEnabled
        if (!locationEnabled) {
            AlertDialog.Builder(context)
                .setMessage(R.string.location_is_not_enabled)
                .setPositiveButton(R.string.button_ok, null)
                .show()
        }
    }

    private fun registerViewModel() {
        placesToVisitViewModel.countAll().observe(viewLifecycleOwner) {
            binding.placesToVisitCount.text = "$it"
        }

        routesViewModel.countAll().observe(viewLifecycleOwner) {
            binding.routesCount.text = "$it"
        }

        tracksViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            displayTracks(tracks)
            displaySummary(tracks.flatMap { it.value })
        }

        tracksViewModel.newTrackid.observe(viewLifecycleOwner) { trackId ->
            if (binding.statusContainer.isVisible) {
                startTracking(trackId)
                tracksViewModel.subscribeToPoints(trackId)
                currentTrackId = trackId

                startMiniMap()
                observePointsChanges()
            }
        }

        tracksViewModel.updateResult.observe(viewLifecycleOwner) {
            setUIMode(isRecording = false)
        }

        tracksViewModel.trackLoadingStatus.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun handleStopRecordActions() {
        binding.buttonRecord.setOnClickListener {
            fineLocation.runWithPermission {
                backgroundLocation.runWithPermission {
                    setUIMode(isRecording = true)
                    tracksViewModel.createNewTrack()
                }
            }
        }
        binding.buttonRecordGpx.setOnClickListener {
            fineLocation.runWithPermission {
                backgroundLocation.runWithPermission {
                    openFile()
                }
            }
        }

        binding.buttonStop.setOnClickListener {
            stopTracking()
        }

        binding.buttonStop.hide()
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

        binding.miniMap.setTileSource(TileSourceFactory.MAPNIK)
        binding.miniMap.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        binding.miniMap.controller.setZoom(DEFAULT_MAP_ZOOM)
        binding.miniMap.setMultiTouchControls(true)

        miniMapMarker = Marker(binding.miniMap).apply {
            icon = resources.getDrawable(R.drawable.bg_seekbar_thumb, context?.theme)
        }

        fetchTracksWithPoints()
    }

    private fun fetchTracksWithPoints() {
        binding.miniMap.overlays.clear()

        // Load GPX track
        tracksViewModel.gpxTrack?.let { gpx ->
            val polyline = Polyline().apply {
                outlinePaint.color = Color.BLUE
                outlinePaint.strokeWidth = 8.0F
                outlinePaint.isAntiAlias = true
                outlinePaint.strokeJoin = Paint.Join.ROUND
            }

            polyline.setPoints(gpx.points)
            binding.miniMap.overlays.add(polyline)
        }
        // Or display all routes
            ?: run {
                tracksViewModel.fetchAllPoints().observe(viewLifecycleOwner) { grouppedPoints ->
                    for (points in grouppedPoints) {
                        displayTrack(points.value)
                    }
                    binding.miniMap.invalidateMapCoordinates(binding.miniMap.projection.screenRect)
                    binding.miniMap.overlays.add(miniMapMarker)
                }
            }
    }

    private fun displayTrack(points: List<SimplePointDto>, color: Int = Color.RED) {
        val polyline = Polyline().apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = 6.0F
        }

        val mapPoints = points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
        binding.miniMap.overlays.add(polyline)
    }

    private fun observePointsChanges() {
        tracksViewModel.recordStatus?.observe(viewLifecycleOwner) { recordStatus ->
            binding.distanceValue.text = getString(R.string.distance_format, recordStatus.distance)
            binding.speedValue.text = getString(R.string.speed_format, recordStatus.speed * 3.6F)
            binding.avgSpeedValue.text = getString(R.string.speed_format, recordStatus.averageSpeed * 3.6F)
            binding.timeValue.text = DateTimeUtils.getFormattedTime(recordStatus.time.toInt())

            if (miniMapMarker != null && recordStatus.location != null) {
                binding.miniMap.controller.apply {
                    miniMapMarker?.position = recordStatus?.location
                    miniMapMarker?.icon = resources.getDrawable(R.drawable.bg_seekbar_thumb, context?.theme)
                    miniMapMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setCenter(recordStatus.location)
                }
            }
        }
    }

    private fun displayTracks(tracks: Map<String?, List<Track>>) {
        trackAdapter.items = tracks
        binding.tracksList.expandGroup(0)
    }

    private fun displaySummary(tracks: List<Track>) {
        var totalDistance = 0.0F
        for (track in tracks) {
            totalDistance += track.distance
        }
        val totalTime = tracks.sumOf { track ->
            track.timeDifference?.takeIf { it.isNotEmpty() }?.let { difference ->
                val chunks = difference.split(" ")
                val hours = chunks[0].removeSuffix("h").toIntOrNull() ?: 0
                val minutes = chunks[1].removeSuffix("m").toIntOrNull() ?: 0
                val seconds = chunks[2].removeSuffix("s").toIntOrNull() ?: 0
                hours * 60 * 60 + minutes * 60 + seconds
            } ?: 0
        }
        binding.sumDistance.text = getString(R.string.distance_format, totalDistance)
        binding.sumTime.text = DateTimeUtils.getFormattedTime(totalTime, showSeconds = true)
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
        if (binding.statusContainer.visibility == View.VISIBLE) {
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
                .setNegativeButton(R.string.delete_no) { _, _ -> tracksViewModel.fetchTracks() }
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
                binding.buttonStop.show()
                binding.buttonRecord.hide()
                binding.buttonRecordGpx.hide()
                binding.statusContainer.visibility = View.VISIBLE
            }
            else -> {
                binding.buttonStop.hide()
                binding.buttonRecord.show()
                binding.buttonRecordGpx.show()
                binding.statusContainer.visibility = View.GONE
            }
        }
    }

    private fun openFile() {
        openFile.launch(arrayOf("*/*"))
    }

    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
        result?.let { uri ->
            val result = tracksViewModel.readGpx(uri, requireContext().contentResolver)
            if (result) {
                setUIMode(isRecording = true)
                tracksViewModel.createNewTrack()
            } else {
                Toast.makeText(requireContext(), "Invalid file!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val fineLocation =
        PermissionRequester(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            onDenied = { showPermissionsDeniedDialog() }
        )

    private val backgroundLocation =
        PermissionRequester(
            this,
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE
            ),
            onDenied = { showPermissionsDeniedDialog() }
        )

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
