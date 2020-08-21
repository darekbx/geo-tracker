package com.darekbx.geotracker.ui.tracks

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.darekbx.geotracker.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey);
    }
}