package com.darekbx.geotracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.utils.AppPreferences
import com.google.android.gms.location.ActivityTransitionResult

class ActivityTranstionReceiver : BroadcastReceiver() {

    companion object {
        val ACTION = "OnBicycleActivityAction"
        val TAG = "ActivityTranstionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isEnabled = AppPreferences(context).activityDetection
        if (isEnabled && ActivityTransitionResult.hasResult(intent)) {
            Log.d(TAG, "Transition received")
            val result = ActivityTransitionResult.extractResult(intent) ?: return
            if (result.transitionEvents.isNotEmpty() && !ForegroundTracker.IS_RUNNING) {
                val intent = Intent(context, ForegroundTracker::class.java)
                context?.startForegroundService(intent)
            }
        }
    }
}
