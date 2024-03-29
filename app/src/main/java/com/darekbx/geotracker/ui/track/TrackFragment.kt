package com.darekbx.geotracker.ui.track

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.FragmentTrackBinding
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.ui.MapStyle
import com.darekbx.geotracker.ui.tracks.SaveTrackDialog
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Polyline
import javax.inject.Inject

@AndroidEntryPoint
class TrackFragment : Fragment(R.layout.fragment_track) {

    private var _binding: FragmentTrackBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val DEFAULT_MAP_ZOOM = 18.0
        const val TRACK_ID_KEY = "track_id_key"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val tracksViewModel: TrackViewModel by viewModels()

    @Inject
    lateinit var mapStyle: MapStyle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tracksViewModel.updateResult.observe(viewLifecycleOwner) {
            loadTrack()
        }
        tracksViewModel.pointsDeleteResult.observe(viewLifecycleOwner) {
            showDeleteTrackPointsSuccessDialog()
        }
        tracksViewModel.fixResult.observe(viewLifecycleOwner) {
            notifyFixed()
        }

        loadTrack()
        initializeMap()

        binding.imageLabelEdit.setOnClickListener { editLabel() }
        binding.overlappingButton.setOnClickListener { displayOverlappingMap() }
        binding.clearPointsButton.setOnClickListener { confirmDeleteTrackPoints() }
        binding.fixDataButton.setOnClickListener { fixDate() }
        binding.editTrackButton.setOnClickListener { openTrackEditor() }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
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
            tracksViewModel.fetchTrack(trackId).observe(viewLifecycleOwner) { track ->
                displayTrack(track)
            }
        }
    }

    private fun initializeMap() {
        val context = activity?.applicationContext
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        mapStyle.applyMapStyle(binding.map)
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
        binding.map.setMultiTouchControls(true)
    }

    @SuppressLint("SetTextI18n")
    private fun displayTrack(track: Track) {
        val label = when (TextUtils.isEmpty(track.label)) {
            true -> getString(R.string.empty)
            else -> track.label
        }

        val date = track.startTimestamp!!.split(" ")[0]
        val startTime = track.startTimestamp.split(" ")[1]
        val endTime = when (track.endTimestamp) {
            null -> getString(R.string.empty)
            else -> track.endTimestamp.split(" ")[1]
        }

        binding.valueLabel.text = label
        binding.valueLabelId.text = "id: ${track.id}"
        binding.valueDate.text = date
        binding.valueStartTime.text = startTime
        binding.valueEndTime.text = endTime
        binding.valueTime.text = "(${track.timeDifference})"
        binding.valueDistance.text = getString(R.string.distance_format, track.distance)
        binding.fixDataButton.isVisible = track.isTimeBroken

        if (track.points.isNotEmpty()) {
            displayFullTrackDetails(track)
        } else {
            binding.valuePoints.text = getString(R.string.points_deleted)
            binding.speedView.values = emptyList()
            binding.altitudeView.values = emptyList()
            binding.map.visibility = View.INVISIBLE
            binding.editTrackButton.isVisible = false
        }
    }

    private fun displayFullTrackDetails(track: Track) {
        binding.valuePoints.text = getString(R.string.points, track.points.size)
        binding.speedView.values = track.points.map { it.speed }
        binding.altitudeView.values = track.points.map { it.altitude.toFloat() }

        track.points.firstOrNull()?.let { point ->
            binding.map.controller.apply {
                setZoom(DEFAULT_MAP_ZOOM)
                val startPoint = GeoPoint(point.latitude, point.longitude)
                setCenter(startPoint)
            }
        }

        val polyline = Polyline().apply {
            outlinePaint.color = Color.RED
            outlinePaint.strokeWidth = 6.0F
        }

        binding.map.overlays.add(polyline)

        val mapPoints = track.points.map { point -> GeoPoint(point.latitude, point.longitude) }
        polyline.setPoints(mapPoints)
    }

    private fun editLabel() {
        val currentLabel = binding.valueLabel.text.toString()
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
