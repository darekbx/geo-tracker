package com.darekbx.geotracker.location

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.*
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
import com.darekbx.geotracker.livetracking.ILiveTracker
import com.darekbx.geotracker.model.LiveLocation
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.TrackDto
import com.darekbx.geotracker.ui.MainActivity
import com.darekbx.geotracker.ui.tracks.TracksFragment
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.utils.DateTimeUtils
import com.darekbx.geotracker.viewmodels.TrackViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ForegroundTracker : Service() {

    companion object {
        const val TRACK_ID_KEY = "track_id_key"
        const val NOTIFICATION_ID = 100
        const val NOTIFICATION_CHANNEL_ID = "location_channel_id"

        var IS_RUNNING = false
    }

    private val viewModelJob = SupervisorJob()

    private val ioScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var pointDao: PointDao

    @Inject
    lateinit var trackDao: TrackDao

    @Inject
    lateinit var liveTracker: ILiveTracker

    private var trackId: Long? = null
    private var sessionDistance = 0.0F
    private var sessionStartTime = 0L
    private var lastSessionDistance = 0.0F
    private var lastLocation: Location? = null

    override fun onBind(p0: Intent?): Nothing? = null

    override fun onCreate() {
        super.onCreate()

        val notification = createNotification(
            getString(R.string.app_name),
            getString(R.string.notification_text)
        )
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        IS_RUNNING = true
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
        IS_RUNNING = false
        Log.v(GeoTrackerApplication.LOG_TAG, "Service was destroyed!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!locationManager.isLocationEnabled) {
            Log.e(GeoTrackerApplication.LOG_TAG, "Location is not enabled!")
            return super.onStartCommand(intent, flags, startId)
        }

        intent
            ?.getLongExtra(TRACK_ID_KEY, -1L)
            ?.takeIf { it > 0 }
            ?.let { trackId ->
                this@ForegroundTracker.trackId = trackId
                startLocationUpdates()
            } ?: createTrackIdAndStart()

        return START_STICKY
    }

    private fun createTrackIdAndStart() {
        ioScope.launch {
            val track = TrackDto(
                startTimestamp = System.currentTimeMillis(),
                distance = 0.0F
            )
            this@ForegroundTracker.trackId = trackDao.add(track)
            withContext(Dispatchers.Main) {
                startLocationUpdates()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        Log.v(GeoTrackerApplication.LOG_TAG, "Start location updates")
        sessionDistance = 0.0F
        sessionStartTime = System.currentTimeMillis()
        val minDistance = appPreferences.gpsMinDistance
        val updateInterval = appPreferences.gpsUpdateInterval
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            updateInterval,
            minDistance,
            locationListener
        )
    }

    private val locationListener = LocationListener { location ->
        Log.v(GeoTrackerApplication.LOG_TAG, "Received location update: $location")
        trackId?.let { trackId ->
            ioScope.launch {
                addPoint(trackId, location)
                appendDistance(location, trackId)
            }
            if (liveTrackerEnabled) {
                notifyLiveLocation(location, trackId)
            }
        }
    }

    private fun notifyLiveLocation(location: Location, trackId: Long) {
        val liveLocation = LiveLocation(
            location.latitude,
            location.longitude,
            location.speed,
            location.time,
            trackId
        )
        liveTracker.notifyLocation(liveLocation)
    }

    private fun appendDistance(location: Location, trackId: Long) {
        lastLocation?.distanceTo(location)?.let { distance ->
            trackDao.appendDistance(trackId, distance)
            sessionDistance += distance
            updateNotification()
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

    private fun updateNotification() {
        if (sessionDistance - lastSessionDistance > appPreferences.gpsNotificationMinDistance) {
            val elapsedTimeInMs = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
            val notification = createNotification(
                getString(
                    R.string.notification_title,
                    sessionDistance / TrackViewModel.ONE_KILOMETER,
                    DateTimeUtils.getFormattedTime(elapsedTimeInMs)
                ),
                getString(R.string.notification_text)
            )
            notificationManager.notify(NOTIFICATION_ID, notification)
            lastSessionDistance = sessionDistance
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @ExperimentalCoroutinesApi
    private fun createNotification(title: String, text: String): Notification {

        val stopIntent = Intent(TracksFragment.STOP_ACTION)
        val stopPendingIntent = getBroadcast(applicationContext, 0,
            stopIntent, FLAG_UPDATE_CURRENT)

        val tracksIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(TRACK_ID_KEY, trackId)
        }
        val tracksPendingIntent = getActivity(applicationContext, 0,
            tracksIntent, FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tracksPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_stop, getString(R.string.button_stop), stopPendingIntent)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        var channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (channel == null) {
            channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                title,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        return builder.build()
    }

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val liveTrackerEnabled by lazy { AppPreferences(applicationContext).liveTracking }
}
