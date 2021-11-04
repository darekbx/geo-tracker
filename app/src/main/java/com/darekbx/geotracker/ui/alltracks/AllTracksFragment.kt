package com.darekbx.geotracker.ui.alltracks

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.entities.SimplePointDto
import com.darekbx.geotracker.ui.track.TrackFragment
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_all_tracks.*
import kotlinx.android.synthetic.main.fragment_track.*
import kotlinx.android.synthetic.main.fragment_track.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Polyline

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class AllTracksFragment : Fragment(R.layout.fragment_all_tracks) {

    companion object {
        private val COLOR_RED = Color.parseColor("#f44336")
        private val COLOR_GRAY = Color.parseColor("#999999")
        const val DEFAULT_MAP_ZOOM = 13.0
    }

    private val tracksViewModel: TrackViewModel by viewModels()
    private var trackToOverlap: Track? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAllTracks()
        initializeMap()
        zoomToCurrentLocation()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun loadAllTracks() {
        loading_view.visibility = View.VISIBLE
        val trackId = arguments?.getLong(TrackFragment.TRACK_ID_KEY)
        if (trackId != null) {
            tracksViewModel.fetchSimpleTrack(trackId).observe(viewLifecycleOwner, Observer { track ->
                trackToOverlap = track
                displayTrackX(trackToOverlap!!.simplePoints, COLOR_RED)
                fetchTracksWithPoints()
            })
        } else {
            fetchTracksWithPoints()
        }
    }

    private fun fetchTracksWithPoints() {
        tracksViewModel.fetchAllPoints().observe(viewLifecycleOwner, Observer { grouppedPoints ->
            for (points in grouppedPoints) {
                if (trackToOverlap?.id != points.key) {
                    displayTrackX(points.value)
                }
            }
            loading_view.visibility = View.GONE
            map.invalidateMapCoordinates(map.projection.screenRect)
        })
    }

    private fun initializeMap() {
        val context = activity?.applicationContext
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)
    }

    private fun displayTrackX(points: List<SimplePointDto>, color: Int = provideColor()) {
        val polyline = Polyline().apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = 6.0F
        }
        polyline.actualPoints.clear()

        val mapPoints = points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
        map.overlays.add(polyline)
    }

    private fun provideColor() =
        when (trackToOverlap?.id) {
            null -> COLOR_RED
            else -> COLOR_GRAY
        }

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            val lastKnownLocation = this@AllTracksFragment.lastKnownLocation
            if (lastKnownLocation != null) {
                withContext(Dispatchers.Main) {
                    map.controller.apply {
                        setZoom(DEFAULT_MAP_ZOOM)
                        val startPoint =
                            GeoPoint(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        setCenter(startPoint)
                    }
                }
            }
        }
    }

    private val lastKnownLocation by lazy {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }
}
