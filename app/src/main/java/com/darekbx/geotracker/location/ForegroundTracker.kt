package com.darekbx.geotracker.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.darekbx.geotracker.GeoTrackerApplication
import com.darekbx.geotracker.R
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.ui.tracks.TracksFragment
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
        val TRACK_ID_KEY = "track_id_key"
        val NOTIFICATION_ID = 100
        val NOTIFICATION_CHANNEL_ID = "location_channel_id"
    }

    private val viewModelJob = SupervisorJob()

    protected val ioScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var pointDao: PointDao

    @Inject
    lateinit var trackDao: TrackDao

    private var trackId: Long? = null
    private var lastLocation: Location? = null

    override fun onBind(p0: Intent?) = null

    override fun onCreate() {
        super.onCreate()

        val notification = showNotification(
            getString(R.string.app_name),
            getString(R.string.notification_text)
        )
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
    }

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
                trackId?.let { trackId ->
                    addPoint(trackId, location)
                    appendDistance(location, trackId)
                }
            }
        }
    }

    private fun appendDistance(location: Location, trackId: Long) {
        lastLocation?.distanceTo(location)?.let { distance ->
            trackDao.appendDistance(trackId, distance)
        }
        lastLocation = location
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

    private fun showNotification(title: String, text: String): Notification {

        val stopIntent = Intent(TracksFragment.STOP_ACTION)
        val stopPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_stop, getString(R.string.button_stop), stopPendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        var channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (channel == null) {
            channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                title,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        return builder.build()
    }

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
}