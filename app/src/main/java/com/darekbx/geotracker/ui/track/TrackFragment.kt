package com.darekbx.geotracker.ui.track

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.ui.tracks.SaveTrackDialog
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
        const val DEFAULT_MAP_ZOOM = 18.0
        const val TRACK_ID_KEY = "track_id_key"
    }

    private val tracksViewModel: TrackViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.updateResult.observe(viewLifecycleOwner, Observer {
            loadTrack()
        })
        tracksViewModel.pointsDeleteResult.observe(viewLifecycleOwner, Observer {
            showDeleteTrackPointsSuccessDialog()
        })
        tracksViewModel.fixResult.observe(viewLifecycleOwner, Observer {
            notifyFixed()
        })

        loadTrack()
        initializeMap()

        image_label_edit.setOnClickListener { editLabel() }
        overlapping_button.setOnClickListener { displayOverlappingMap() }
        clear_points_button.setOnClickListener { confirmDeleteTrackPoints() }
        clear_points_button.setOnClickListener { confirmDeleteTrackPoints() }
        fix_data_button.setOnClickListener { fixDate() }
        edit_track_button.setOnClickListener { openTrackEditor() }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun confirmDeleteTrackPoints() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_points_title)
            .setMessage(R.string.delete_points_message)
            .setPositiveButton(R.string.delete_yes) { _, _ -> deleteTrackPoints() }
            .setNegativeButton(R.string.delete_no, null)
            .show()
    }

    private fun notifyFixed() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.track_was_fixed)
            .setPositiveButton(R.string.button_ok) { _, _ -> loadTrack() }
            .show()
    }

    private fun openTrackEditor() {
        findNavController()
            .navigate(R.id.action_trackFragment_to_trackEditorFragment, arguments)
    }

    private fun fixDate() {
        arguments?.let { arguments ->
            val trackId = arguments.getLong(TRACK_ID_KEY)
            tracksViewModel.fixDate(trackId)
        }
    }

    private fun showDeleteTrackPointsSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_points_title)
            .setMessage(R.string.delete_points_success)
            .setPositiveButton(R.string.button_ok) { _, _ -> loadTrack() }
            .show()
    }

    private fun deleteTrackPoints() {
        val trackId = arguments?.getLong(TRACK_ID_KEY)
        if (trackId != null) {
            tracksViewModel.deleteTrackPoints(trackId)
        }
    }

    private fun displayOverlappingMap() {
        val trackId = arguments?.getLong(TRACK_ID_KEY)
        val arguments = Bundle(1).apply {
            putLong(TRACK_ID_KEY, trackId!!)
        }
        findNavController()
            .navigate(R.id.action_trackFragment_to_allTracksFragment, arguments)
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
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        map.setMultiTouchControls(true)
    }

    @SuppressLint("SetTextI18n")
    private fun displayTrack(track: Track) {
        val label = when (TextUtils.isEmpty(track.label)) {
            true -> getString(R.string.empty)
            else -> track.label
        }

        val date = track.startTimestamp!!.split(" ")[0]
        val startTime = track.startTimestamp.split(" ")[1]
        val endTime = when {
            track.endTimestamp == null -> getString(R.string.empty)
            else -> track.endTimestamp.split(" ")[1]
        }

        value_label.text = label
        value_label_id.text = "id: ${track.id}"
        value_date.text = date
        value_start_time.text = startTime
        value_end_time.text = endTime
        value_time.text = "(${track.timeDifference})"
        value_distance.text = getString(R.string.distance_format, track.distance)
        fix_data_button.isVisible = track.isTimeBroken

        if (track.points.isNotEmpty()) {
            displayFullTrackDetails(track)
        } else {
            value_points.text = getString(R.string.points_deleted)
            speed_view.values = emptyList()
            altitude_view.values = emptyList()
            map.visibility = View.INVISIBLE
            edit_track_button.isVisible = false
        }
    }

    private fun displayFullTrackDetails(track: Track) {
        value_points.text = getString(R.string.points, track.points.size)
        speed_view.values = track.points.map { it.speed }
        altitude_view.values = track.points.map { it.altitude.toFloat() }

        track.points.firstOrNull()?.let { point ->
            map.controller.apply {
                setZoom(DEFAULT_MAP_ZOOM)
                val startPoint = GeoPoint(point.latitude, point.longitude)
                setCenter(startPoint)
            }
        }

        val polyline = Polyline().apply {
            outlinePaint.color = Color.RED
            outlinePaint.strokeWidth = 6.0F
        }

        map.overlays.add(polyline)

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
            discardCallback = { /* Do nothing */ }
        }.show(parentFragmentManager, SaveTrackDialog::class.java.simpleName)
    }
}
