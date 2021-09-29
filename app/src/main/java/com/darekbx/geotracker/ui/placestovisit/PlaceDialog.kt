package com.darekbx.geotracker.ui.placestovisit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import kotlinx.android.synthetic.main.dialog_place_to_visit.*
import org.osmdroid.util.GeoPoint

class PlaceDialog : DialogFragment(R.layout.dialog_place_to_visit) {

    var placeLocation: GeoPoint? = null
    var placeLabel: String? = null
    var deleteCallback: () -> Unit = { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_MinWidth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        close_button.setOnClickListener { dismiss() }
        delete_button.setOnClickListener {
            deleteCallback()
            dismiss()
        }

        placeLocation?.run {
            position_text.text = getString(R.string.location_format, latitude, longitude)
        }
        place_label.text = placeLabel
    }
}
