package com.darekbx.geotracker.ui.trackeditor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.FragmentTrackEditorBinding
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.ui.track.TrackFragment
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Polyline
import java.util.*

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class TrackEditorFragment: Fragment(R.layout.fragment_track_editor) {

    private var _binding: FragmentTrackEditorBinding? = null
    private val binding get() = _binding!!

    private val tracksViewModel: TrackViewModel by viewModels()
    private var trackToEdit: Track? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.progress.observe(viewLifecycleOwner, Observer { progress ->
            binding.progressView.progress = progress.value
            binding.progressView.max = progress.max

            if (progress.isCompleted) {
                hideProgress()
            }
        })

        tracksViewModel.pointsDeleteResult.observe(viewLifecycleOwner, Observer {
            hideProgress()
            Toast.makeText(requireContext(), R.string.points_deleted, Toast.LENGTH_SHORT).show()
        })

        loadAllTracks()
        initializeMap()
        zoomToCurrentLocation()
        handleSeekBars()

        binding.saveButton.setOnClickListener { save() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    private fun handleSeekBars() {
        binding.seekStart.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                lockProgress(seekBar, binding.seekEnd, progress)
                binding.seekStartValueLabel.text = "${seekBar.calculatePointsCount()}"
                updateSummary()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                subTrack(currentTrackStyle())
            }
        })

        binding.seekEnd.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                lockProgress(seekBar, binding.seekStart, progress)
                val endValue = (trackToEdit?.points?.size ?: 0) - seekBar.calculatePointsCount()
                binding.seekEndValueLabel.text = "$endValue"
                updateSummary()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                subTrack(currentTrackStyle())
            }
        })
    }

    private fun lockProgress(seekBar: SeekBar, oppositeSeekBar: SeekBar, progress: Int) {
        val maxProgress = seekBar.max - oppositeSeekBar.progress
        if (progress >= maxProgress) {
            seekBar.progress = maxProgress
        }
    }

    private fun updateSummary() {
        val overallCount = trackToEdit?.points?.size ?: return
        val cutCount = overallCount - (
                binding.seekStart.calculatePointsCount() + binding.seekEnd.calculatePointsCount()
                )
        binding.summaryLabel.text = getString(R.string.seek_summary, overallCount, cutCount)
    }

    private fun loadAllTracks() {
        showProgress()
        val trackId = arguments?.getLong(TrackFragment.TRACK_ID_KEY)
        if (trackId != null) {
            tracksViewModel.fetchTrack(trackId).observe(viewLifecycleOwner, Observer { track ->
                trackToEdit = track
                displayTrack(track, currentTrackStyle(), CUT_OVERLAY_ID)
                fetchTracksWithPoints()
                updateSummary()
            })
        }
    }

    private fun save() {

        showProgress()
        val trackId = arguments?.getLong(TrackFragment.TRACK_ID_KEY)
        if (trackId != null) {
            val points = trackToEdit?.points ?: return
            val start = binding.seekStart.calculatePointsCount()
            val end = binding.seekEnd.calculatePointsCount()
            val startPoints = points.take(start)
            val endPoints = points.takeLast(end)
            tracksViewModel.deleteTrackPoints(trackId, startPoints, endPoints)
        }
    }

    private fun showProgress() {
        binding.loadingView.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.loadingView.visibility = View.GONE
    }

    private fun fetchTracksWithPoints() {
        tracksViewModel.fetchTracksWithPoints(
            nthPointsToSkip = POINTS_TO_SKIP
        ).observe(viewLifecycleOwner, Observer { track ->
            if (trackToEdit?.id != track.id) {
                displayTrack(track, otherTracksStyle())
            }
        })
    }

    private fun initializeMap() {
        val context = activity?.applicationContext
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        binding.map.setMultiTouchControls(true)
    }

    private fun displayTrack(track: Track, trackStyle: TrackStyle, overlayId: String? = null) {
        val polyline = Polyline().apply {
            id = overlayId
            outlinePaint.color = trackStyle.color
            outlinePaint.strokeWidth = trackStyle.width
        }

        val mapPoints = track.points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)

        binding.map.overlays.add(polyline)
        binding.map.invalidateMapCoordinates(binding.map.projection.screenRect)
    }

    private fun subTrack(trackStyle: TrackStyle) {
        val polyline = Polyline().apply {
            id = CUT_OVERLAY_ID
            outlinePaint.color = trackStyle.color
            outlinePaint.strokeWidth = trackStyle.width
        }

        val trackPointsCount = trackToEdit?.points?.size ?: return
        val start = binding.seekStart.calculatePointsCount()
        val end = binding.seekEnd.calculatePointsCount()

        val mapPoints = trackToEdit?.points
            ?.map { point -> GeoPoint(point.latitude, point.longitude) }
            ?.subList(start, trackPointsCount - end)
            ?: return
        polyline.setPoints(mapPoints)

        binding.map.overlays.removeAll { it is Polyline && it.id == CUT_OVERLAY_ID }
        binding.map.overlays.add(0, polyline)
        binding.map.invalidateMapCoordinates(binding.map.projection.screenRect)
    }

    private fun SeekBar.calculatePointsCount() =
        (trackToEdit?.points?.size ?: 0) * progress / 100

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            val lastKnownLocation = this@TrackEditorFragment.lastKnownLocation
            if (lastKnownLocation != null) {
                withContext(Dispatchers.Main) {
                    binding.map.controller.apply {
                        setZoom(DEFAULT_MAP_ZOOM)
                        val startPoint =
                            GeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        setCenter(startPoint)
                    }
                }
            }
        }
    }

    private fun currentTrackStyle() = TrackStyle.Current(COLOR_RED, 12F)
    private fun otherTracksStyle() = TrackStyle.Shadowed(COLOR_GRAY, 6F)

    private val lastKnownLocation by lazy {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }

    companion object {
        private val COLOR_RED = Color.parseColor("#f44336")
        private val COLOR_GRAY = Color.parseColor("#666666")
        private const val POINTS_TO_SKIP = 1
        private const val CUT_OVERLAY_ID = "3gwa98t23nq9cm"
        const val DEFAULT_MAP_ZOOM = 13.0
    }
}