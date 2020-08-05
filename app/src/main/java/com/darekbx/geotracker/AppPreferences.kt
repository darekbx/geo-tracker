package com.darekbx.geotracker

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppPreferences @Inject constructor(@ApplicationContext val context: Context) {

    fun doYourWork() {
        Log.d("Hilt", "Do something")
    }
}