package com.darekbx.geotracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeoTrackerApplication : Application() {

    companion object {
        val LOG_TAG = "GeoTracker"
    }
}