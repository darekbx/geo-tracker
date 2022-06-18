package com.darekbx.geotracker.ui.placestovisit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.DialogPlaceToVisitBinding
import org.osmdroid.util.GeoPoint

class PlaceDialog : DialogFragment(R.layout.dialog_place_to_visit) {

    private var _binding: DialogPlaceToVisitBinding? = null
    private val binding get() = _binding!!

    var placeLocation: GeoPoint? = null
    var placeLabel: String? = null
    var deleteCallback: () -> Unit = { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlaceToVisitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_MinWidth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.closeButton.setOnClickListener { dismiss() }
        binding.deleteButton.setOnClickListener {
            deleteCallback()
            dismiss()
        }

        placeLocation?.run {
            binding.positionText.text = getString(R.string.location_format, latitude, longitude)
        }
        binding.placeLabel.text = placeLabel
    }
}
