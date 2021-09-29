package com.darekbx.geotracker.ui.placestovisit

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import kotlinx.android.synthetic.main.dialog_add_place_to_visit.*
import org.osmdroid.util.GeoPoint

class AddPlaceDialog : DialogFragment(R.layout.dialog_add_place_to_visit) {

    var location: GeoPoint? = null
    var saveCallback: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_MinWidth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        save_button.setOnClickListener { validateLabelAndSave() }

        location?.run {
            position_text.text = getString(R.string.location_format, latitude, longitude)
        }
    }

    private fun validateLabelAndSave() {
        val label = input_label.text.toString()
        when (TextUtils.isEmpty(label)) {
            true -> input_label.error = getString(R.string.empty_label_validation)
            else -> {
                saveCallback?.invoke(input_label.text.toString())
                dismiss()
            }
        }
    }
}
