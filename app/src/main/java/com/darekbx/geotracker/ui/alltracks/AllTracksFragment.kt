package com.darekbx.geotracker.ui.tracks

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
        private val COLOR_GRAY = Color.LTGRAY
        const val DEFAULT_MAP_ZOOM = 18.0
    }

    private val tracksViewModel: TrackViewModel by viewModels()
    private var trackToOverlap: Track? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.tracksWithPoints.observe(viewLifecycleOwner, Observer { tracks ->
            loading_view.visibility = View.GONE
            displayTracks(tracks)
        })
        tracksViewModel.progress.observe(viewLifecycleOwner, Observer { progress->
            progress_view.progress = progress.value
            progress_view.max = progress.max
        })

        loadAllTracks()
        initializeMap()
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
            tracksViewModel.fetchTrack(trackId).observe(viewLifecycleOwner, Observer { track ->
                trackToOverlap = track
                tracksViewModel.fetchTracksWithPoints()
            })
        } else {
            tracksViewModel.fetchTracksWithPoints()
        }
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

    private fun displayTracks(tracks: List<Track>) {
        zoomToCurrentLocation()

        val overlapTrackId = trackToOverlap?.id
        if (trackToOverlap != null) {
            displayOverlappedTrack(tracks, overlapTrackId)
        } else {
            displayAllTracks(tracks)
        }

        loading_view.visibility = View.GONE
    }

    private fun displayAllTracks(tracks: List<Track>) {
        for (track in tracks) {
            displayTrack(track, COLOR_RED)
        }
    }

    private fun displayOverlappedTrack(tracks: List<Track>, overlapTrackId: Long?) {
        displayTrack(trackToOverlap!!, COLOR_RED)
        for (track in tracks) {
            if (overlapTrackId == track.id) continue
            displayTrack(track, COLOR_GRAY)
        }
    }

    private fun displayTrack(track: Track, color: Int) {
        val polyline = Polyline().apply {
            outlinePaint.color = color
            outlinePaint.strokeWidth = 6.0F
        }
        map.overlays.add(polyline)

        val mapPoints = track.points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
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
