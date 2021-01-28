package com.darekbx.geotracker.livetracking

import android.util.Log
import com.darekbx.geotracker.BuildConfig
import com.darekbx.geotracker.GeoTrackerApplication
import com.darekbx.geotracker.model.LiveLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HerokuLiveTracker : ILiveTracker {

    private val httpClient by lazy { OkHttpClient() }
    private val updateInterval by lazy { TimeUnit.SECONDS.toMillis(30) }
    private var lastLiveLocation = 0L

    override fun notifyLocation(liveLocation: LiveLocation) {
        if (canUpload(liveLocation)) {
            Log.d(GeoTrackerApplication.LOG_TAG, "Skipping location update")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val postBody = liveLocation.asFormBody()
            val request: Request = Request.Builder()
                .url(BuildConfig.LIVE_TRACKER_URL)
                .header("Authorization", "Basic ${BuildConfig.LIVE_TRACKER_TOKEN}")
                .post(postBody)
                .build()
            try {
                httpClient.newCall(request).execute().use({ response ->
                    Log.d(GeoTrackerApplication.LOG_TAG, "Live tracker response: HTTP ${response.code}")
                    saveLastLiveLocationTime(liveLocation)
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun canUpload(liveLocation: LiveLocation) =
        liveLocation.timestamp - lastLiveLocation < updateInterval

    private fun saveLastLiveLocationTime(liveLocation: LiveLocation) {
        lastLiveLocation = liveLocation.timestamp
    }

}
