package com.darekbx.geotracker.ui.tracks

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.DialogTrackLabelBinding

class SaveTrackDialog : DialogFragment(R.layout.dialog_track_label) {

    private var _binding: DialogTrackLabelBinding? = null
    private val binding get() = _binding!!

    var saveCallback: ((String?) -> Unit)? = null
    var discardCallback: (() -> Unit)? = null
    var initialLabel: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTrackLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

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
        binding.saveButton.setOnClickListener { validateLabelAndSave() }
        binding.discardButton.setOnClickListener {
            discardCallback?.invoke()
            dismiss()
        }
        binding.inputLabel.setText(initialLabel ?: getString(R.string.default_track_name))
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
