package com.darekbx.geotracker.ui

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.darekbx.geotracker.ActivityTranstionReceiver
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transitions = listOf<ActivityTransition>(
            ActivityTransition.Builder()
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .build()
        )

        val transitionRequest = ActivityTransitionRequest(transitions)

        val intent = Intent(applicationContext, ActivityTranstionReceiver::class.java).apply {
            action = ActivityTranstionReceiver.ACTION
        }
        val pendingIntent = PendingIntent
            .getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val task = ActivityRecognition.getClient(applicationContext)
            .requestActivityTransitionUpdates(transitionRequest, pendingIntent)
        task.addOnSuccessListener { Log.d("-------", "Transitions reqested") }
        task.addOnFailureListener { Log.e("-------", "Unable to request transitions $it") }
    }

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
