package com.darekbx.geotracker.ui.tracks

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_track.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Polyline

@AndroidEntryPoint
class AllTracksFragment : Fragment(R.layout.fragment_all_tracks) {

    companion object {
        const val DEFAULT_MAP_ZOOM = 18.0
    }

    private val tracksViewModel: TrackViewModel by viewModels()
    private var trackToOverlap: Track? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.tracksWithPoints.observe(viewLifecycleOwner, Observer { tracks ->
            displayTracks(tracks)
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
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)
    }

    private fun displayTracks(tracks: List<Track>) {
        zoomToFirstPoint(tracks)
        val overlapTrackId = trackToOverlap?.id
        if (trackToOverlap != null) {
            displayOverlappedTrack(tracks, overlapTrackId)
        } else {
            displayAllTracks(tracks)
        }
    }

    private fun displayAllTracks(tracks: List<Track>) {
        val newestTrack = tracks.maxBy { it.id!! }
        for (track in tracks) {
            val color = if (track.id == newestTrack?.id)
                Color.BLUE
            else
                Color.RED
            displayTrack(track, color)
        }
    }

    private fun displayOverlappedTrack(tracks: List<Track>, overlapTrackId: Long?) {
        displayTrack(trackToOverlap!!, Color.RED)
        for (track in tracks) {
            if (overlapTrackId == track.id) continue
            displayTrack(track, Color.GRAY)
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

    private fun zoomToFirstPoint(tracks: List<Track>) {
        tracks
            .lastOrNull()
            ?.points
            ?.firstOrNull()
            ?.let { point ->
                map.controller.apply {
                    setZoom(DEFAULT_MAP_ZOOM)
                    val startPoint = GeoPoint(point.latitude, point.longitude)
                    setCenter(startPoint)
                }
            }
    }
}
