package com.darekbx.geotracker.ui.tracks

import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
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
class TrackFragment : Fragment(R.layout.fragment_track) {

    companion object {
        val DEFAULT_MAP_ZOOM = 18.0
        val TRACK_ID_KEY = "track_id_key"
    }

    private val tracksViewModel: TrackViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            loadTrack()
        })

        loadTrack()
        initializeMap()

        image_label_edit.setOnClickListener { editLabel() }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun loadTrack() {
        arguments?.let { arguments ->
            val trackId = arguments.getLong(TRACK_ID_KEY)
            tracksViewModel.fetchTrack(trackId).observe(viewLifecycleOwner, Observer { track ->
                displayTrack(track)
            })
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

    private fun displayTrack(track: Track) {
        val label = when (TextUtils.isEmpty(track.label)) {
            true -> getString(R.string.empty)
            else -> track.label
        }
        value_label.setText(label)
        value_start_time.setText(track.startTimestamp)
        value_end_time.setText(track.endTimestamp ?: getString(R.string.empty))
        value_distance.setText(getString(R.string.distance_format, track.distance))
        value_points.setText("${track.points.size}")
        speed_view.values = track.points.map { it.speed }

        track.points.firstOrNull()?.let { point ->
            map.controller.apply {
                setZoom(DEFAULT_MAP_ZOOM)
                val startPoint = GeoPoint(point.latitude, point.longitude)
                setCenter(startPoint)
            }
        }

        val polyline = Polyline().apply {
            outlinePaint.setColor(Color.RED)
            outlinePaint.strokeWidth = 6.0F
        }

        map.getOverlays().add(polyline)

        val mapPoints = track.points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
    }

    private fun editLabel() {
        val currentLabel = value_label.text.toString()
        val trackId = arguments?.getLong(TRACK_ID_KEY)
        SaveTrackDialog().apply {
            initialLabel = currentLabel
            saveCallback = { label ->
                trackId?.let { trackId ->
                    tracksViewModel.updateTrack(trackId, label)
                }
            }
        }.show(parentFragmentManager, SaveTrackDialog::class.java.simpleName)
    }
}