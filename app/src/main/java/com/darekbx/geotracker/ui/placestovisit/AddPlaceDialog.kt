package com.darekbx.geotracker.ui.placestovisit

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.DialogAddPlaceToVisitBinding
import org.osmdroid.util.GeoPoint

class AddPlaceDialog : DialogFragment(R.layout.dialog_add_place_to_visit) {

    private var _binding: DialogAddPlaceToVisitBinding? = null
    private val binding get() = _binding!!

    var location: GeoPoint? = null
    var saveCallback: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddPlaceToVisitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_MinWidth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.saveButton.setOnClickListener { validateLabelAndSave() }

        location?.run {
            binding.positionText.text = getString(R.string.location_format, latitude, longitude)
        }
    }

    private fun validateLabelAndSave() {
        val label = binding.inputLabel.text.toString()
        when (TextUtils.isEmpty(label)) {
            true -> binding.inputLabel.error = getString(R.string.empty_label_validation)
            else -> {
                saveCallback?.invoke(binding.inputLabel.text.toString())
                dismiss()
            }
        }
    }
}
