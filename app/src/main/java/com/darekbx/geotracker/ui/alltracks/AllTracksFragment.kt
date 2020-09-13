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
        val DEFAULT_MAP_ZOOM = 18.0
    }

    private val tracksViewModel: TrackViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAlTracks()
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

    private fun loadAlTracks() {
        tracksViewModel.allTracks.observe(viewLifecycleOwner, Observer { tracks ->
            displayTracks(tracks)
        })
        tracksViewModel.fetchAllTracks()
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

        tracks.forEach { track ->
            val polyline = Polyline().apply {
                outlinePaint.setColor(Color.RED)
                outlinePaint.strokeWidth = 6.0F
            }

            map.getOverlays().add(polyline)

            val mapPoints = track.points.map { point -> GeoPoint(point.latitude, point.longitude) }
            polyline.setPoints(mapPoints)
        }
    }

    private fun zoomToFirstPoint(tracks: List<Track>) {
        tracks
            .firstOrNull()
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
