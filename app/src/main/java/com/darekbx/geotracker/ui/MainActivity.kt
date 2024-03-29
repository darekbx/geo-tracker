package com.darekbx.geotracker.ui

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.darekbx.geotracker.ActivityTranstionReceiver
import com.darekbx.geotracker.R
import com.darekbx.geotracker.location.ForegroundTracker
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityRecognition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        registerActivityDetection()
    }

    private fun registerActivityDetection() {
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
            .getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val task = ActivityRecognition.getClient(applicationContext)
            .requestActivityTransitionUpdates(transitionRequest, pendingIntent)
        task.addOnSuccessListener { Log.d(TAG, "Transitions reqested") }
        task.addOnFailureListener { Log.e(TAG, "Unable to request transitions $it") }
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
