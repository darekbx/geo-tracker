package com.darekbx.geotracker.ui.placestovisit

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.FragmentPlacesToVisitBinding
import com.darekbx.geotracker.location.LastKnownLocation
import com.darekbx.geotracker.model.PlaceToVisit
import com.darekbx.geotracker.ui.MapStyle
import com.darekbx.geotracker.ui.alltracks.AllTracksFragment
import com.darekbx.geotracker.viewmodels.PlacesToVisitViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import javax.inject.Inject

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class PlacesToVisitFragment: Fragment(R.layout.fragment_places_to_visit) {

    private var _binding: FragmentPlacesToVisitBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var lastKnownLocation: LastKnownLocation

    @Inject
    lateinit var mapStyle: MapStyle

    private val placesToVisitViewModel: PlacesToVisitViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesToVisitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeMap()

        placesToVisitViewModel.listAll().observe(viewLifecycleOwner) { places ->
            displayPlaces(places)
            binding.loadingView.visibility = View.GONE
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

    private fun displayPlaces(places: List<PlaceToVisit>) {
        binding.map.overlays.clear()
        binding.map.overlays.add(touchOverlay)

        val geoPoints = mutableListOf<GeoPoint>()

        for (place in places) {
            val geoPoint = GeoPoint(place.latitude, place.longitude)
            val marker = Marker(binding.map).apply {
                id = "${place.id}"
                position = geoPoint
                title = place.label
            }
            marker.setOnMarkerClickListener { clickedMarker, _ ->
                val id = clickedMarker.id.toLong()
                onMarkerClick(id, clickedMarker.title, clickedMarker.position)
                true
            }
            geoPoints.add(geoPoint)
            binding.map.overlays.add(marker)
        }

        when (geoPoints.size) {
            0 -> zoomToCurrentLocation()
            1 -> zoomToSinglePoint(geoPoints.first())
            else -> zoomToManyPoints(geoPoints)
        }
    }

    private fun zoomToManyPoints(geoPoints: MutableList<GeoPoint>) {
        val boundingBox = BoundingBox.fromGeoPointsSafe(geoPoints)
        binding.map.zoomToBoundingBox(boundingBox, true, 100)
    }

    @SuppressLint("MissingPermission")
    private fun zoomToCurrentLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            val location = lastKnownLocation.getLocation()
            if (location != null) {
                withContext(Dispatchers.Main) {
                    binding.map.controller.apply {
                        setZoom(AllTracksFragment.DEFAULT_MAP_ZOOM)
                        val startPoint = GeoPoint(location.latitude, location.longitude)
                        setCenter(startPoint)
                    }
                }
            }
        }
    }

    private fun zoomToSinglePoint(geoPoint: GeoPoint) {
        binding.map.controller.apply {
            setZoom(AllTracksFragment.DEFAULT_MAP_ZOOM)
            setCenter(geoPoint)
        }
    }

    private fun onMarkerClick(placeId: Long, title: String, location: GeoPoint) {
        PlaceDialog().apply {
            placeLabel = title
            placeLocation = location
            deleteCallback = { placesToVisitViewModel.delete(placeId) }
        }.show(parentFragmentManager, PlaceDialog::class.java.simpleName)
    }

    private fun onMapTap(geoPoint: GeoPoint) {
        AddPlaceDialog().apply {
            saveCallback = { label ->
                placesToVisitViewModel.add(label, geoPoint)
            }
            location = geoPoint
        }.show(parentFragmentManager, AddPlaceDialog::class.java.simpleName)
    }

    private val touchOverlay = object : Overlay() {

        override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
            val projection = mapView.projection
            val location = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
            onMapTap(location)

            return super.onSingleTapConfirmed(e, mapView)
        }
    }
}
