package com.darekbx.geotracker.location

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import com.darekbx.geotracker.GeoTrackerApplication
import com.darekbx.geotracker.repository.AppDatabase
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.utils.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundTracker : Service() {

    companion object {
        val DEFAULT_UPDATES_INTERVAL = TimeUnit.SECONDS.toMillis(15)
        val DEFAULT_MIN_DISTANCE = 10F

        val TRACK_ID_KEY = "track_id_key"
    }

    private val viewModelJob = SupervisorJob()

    protected val ioScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var pointDao: PointDao

    private var trackId: Long? = null

    override fun onBind(p0: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.removeUpdates(locationListener)
        Log.v(GeoTrackerApplication.LOG_TAG, "Service was destroyed!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!locationManager.isLocationEnabled) {
            Log.e(GeoTrackerApplication.LOG_TAG, "Location is not enabled!")
            return super.onStartCommand(intent, flags, startId)
        }

        intent?.let { intent ->
            intent
                ?.getLongExtra(TRACK_ID_KEY, -1L)
                ?.takeIf { it > 0 }
                ?.let { trackId ->
                    this@ForegroundTracker.trackId = trackId
                    startLocationUpdates()
                }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        Log.v(GeoTrackerApplication.LOG_TAG, "Start location updates")
        val minDistance = appPreferences.gpsMinDistance
        val updateInterval = appPreferences.gpsUpdateInterval
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            updateInterval,
            minDistance,
            locationListener
        )
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.v(GeoTrackerApplication.LOG_TAG, "Received location update: $location")

            ioScope.launch {
                trackId?.let { trackId -> addPoint(trackId, location) }
            }
        }
    }

    private fun addPoint(trackId: Long, location: Location) {
        ioScope.launch {
            val point = PointDto(
                null,
                trackId,
                System.currentTimeMillis(),
                location.latitude,
                location.longitude,
                location.speed,
                location.altitude
            )
            pointDao.add(point)
        }
    }

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
}