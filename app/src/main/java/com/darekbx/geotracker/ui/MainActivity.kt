package com.darekbx.geotracker.ui

import androidx.appcompat.app.AppCompatActivity
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onBackPressed() {
        if (ForegroundTracker.IS_RUNNING) {
            DelayedActionDialog.newInstance(R.string.exit_while_recording)
                .apply {
                    onConfirm = {
                        super.onBackPressed()
                    }
                }
                .show(supportFragmentManager, "DelayedActionDialog")

        } else {
            super.onBackPressed()
        }
    }
}
