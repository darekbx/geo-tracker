package com.darekbx.geotracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.utils.AppPreferences
import com.google.android.gms.location.ActivityTransitionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class ActivityTranstionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "OnBicycleActivityAction"
        const val TAG = "ActivityTranstionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isEnabled = AppPreferences(context).activityDetection
        if (isEnabled && ActivityTransitionResult.hasResult(intent)) {
            Log.d(TAG, "Transition received")
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            if (result.transitionEvents.isNotEmpty() && !ForegroundTracker.IS_RUNNING) {
                context.startForegroundService(Intent(context, ForegroundTracker::class.java))
            }
        }
    }
}
