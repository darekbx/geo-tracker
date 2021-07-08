package com.darekbx.geotracker

import android.app.Application
import com.darekbx.cari.sdk.CARI
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeoTrackerApplication : Application() {

    companion object {
        const val LOG_TAG = "GeoTracker"
    }

    override fun onCreate() {
        super.onCreate()
        CARI.initialize(this)
    }
}
