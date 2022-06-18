package com.darekbx.geotracker.ui.alltracks

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.FragmentAllTracksBinding
import com.darekbx.geotracker.location.LastKnownLocation
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.entities.SimplePointDto
import com.darekbx.geotracker.ui.track.TrackFragment
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Polyline
import javax.inject.Inject

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class AllTracksFragment : Fragment(R.layout.fragment_all_tracks) {

    companion object {
        private val COLOR_RED = Color.parseColor("#f44336")
        private val COLOR_GRAY = Color.parseColor("#999999")
        const val DEFAULT_MAP_ZOOM = 13.0
    }

    private var _binding: FragmentAllTracksBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var lastKnownLocation: LastKnownLocation
    private val tracksViewModel: TrackViewModel by viewModels()
    private var trackToOverlap: Track? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllTracksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAllTracks()
        initializeMap()
        zoomToCurrentLocation()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
    }

    private fun loadAllTracks() {
        binding.loadingView.visibility = View.VISIBLE
        val trackId = arguments?.getLong(TrackFragment.TRACK_ID_KEY)
        if (trackId != null) {
            tracksViewModel.fetchSimpleTrack(trackId).observe(viewLifecycleOwner) { track ->
                trackToOverlap = track
                displayTrack(trackToOverlap!!.simplePoints, COLOR_RED)
                fetchTracksWithPoints()
            }
        } else {
            fetchTracksWithPoints()
        }
    }

    private fun fetchTracksWithPoints() {
        tracksViewModel.fetchAllPoints().observe(viewLifecycleOwner) { grouppedPoints ->
            for (points in grouppedPoints) {
                if (trackToOverlap?.id != points.key) {
                    displayTrack(points.value)
                }
            }
            binding.loadingView.visibility = View.GONE
            binding.map.invalidateMapCoordinates(binding.map.projection.screenRect)
        }
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

    private fun displayTrack(points: List<SimplePointDto>, color: Int = provideColor()) {
        val polyline = Polyline().apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = 6.0F
        }

        val mapPoints = points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
        binding.map.overlays.add(polyline)
    }

    private fun provideColor() =
        when (trackToOverlap?.id) {
            null -> COLOR_RED
            else -> COLOR_GRAY
        }

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            val location = lastKnownLocation.getLocation()
            if (location != null) {
                withContext(Dispatchers.Main) {
                    binding.map.controller.apply {
                        setZoom(DEFAULT_MAP_ZOOM)
                        val startPoint =
                            GeoPoint(location.latitude, location.longitude)
                        setCenter(startPoint)
                    }
                }
            }
        }
    }
}
