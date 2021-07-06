package com.darekbx.geotracker.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.darekbx.geotracker.R
import com.darekbx.geotracker.databinding.DialogDelayedActionBinding
import java.util.*
import kotlin.concurrent.timerTask

class DelayedActionDialog : DialogFragment() {

    companion object {

        private const val TITLE_KEY = "titleKey"
        private const val DELAY = 5L // [s]
        private const val ONE_SECOND = 1L // [s]

        fun newInstance(titleResId: Int): DelayedActionDialog {
            return DelayedActionDialog().apply {
                arguments = Bundle(1).apply {
                    putInt(TITLE_KEY, titleResId)
                }
            }
        }
    }

    private var _binding: DialogDelayedActionBinding? = null
    private val binding get() = _binding!!

    private val timer = Timer()

    var onConfirm: () -> Unit = { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDelayedActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        val back = ColorDrawable(Color.WHITE)
        val inset = InsetDrawable(back, 20)
        dialog.window?.setBackgroundDrawable(inset)
        dialog.setCancelable(false)
        isCancelable = false

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getInt(TITLE_KEY)?.run {
            binding.message.text = getString(this)
        }
        binding.cancel.setOnClickListener { dismiss() }
        binding.confirm.setOnClickListener {
            dismiss()
            onConfirm()
        }
        countdown()
    }

    private fun countdown() {
        var time = 0L
        timer.scheduleAtFixedRate(timerTask {
            time += ONE_SECOND
            updateConfirmButton(getString(R.string.wait_for, time), false)

            if (time >= DELAY) {
                updateConfirmButton(getString(R.string.button_confirm), true)
                timer.cancel()
            }
        }, 0L, ONE_SECOND * 1000L)
    }

    private fun updateConfirmButton(title: String, enable: Boolean) {
        binding.confirm.post {
            binding.confirm.text = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT)
            binding.confirm.isEnabled = enable
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
        _binding = null
    }
}
