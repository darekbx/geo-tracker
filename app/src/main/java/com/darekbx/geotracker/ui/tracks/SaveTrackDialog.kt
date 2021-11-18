package com.darekbx.geotracker.ui.tracks

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import kotlinx.android.synthetic.main.dialog_track_label.*

class SaveTrackDialog : DialogFragment(R.layout.dialog_track_label) {

    var saveCallback: ((String?) -> Unit)? = null
    var discardCallback: (() -> Unit)? = null
    var initialLabel: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        isCancelable = false
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Dialog_MinWidth)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        save_button.setOnClickListener { validateLabelAndSave() }
        discard_button.setOnClickListener {
            discardCallback?.invoke()
            dismiss()
        }
        input_label.setText(initialLabel ?: getString(R.string.default_track_name))
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
