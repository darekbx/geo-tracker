package com.darekbx.geotracker.model

import org.osmdroid.util.GeoPoint

class RecordStatus(
    val pointsCount: Int,
    val distance: Float,
    val averageSpeed: Float,
    val speed: Float,
    val time: Long,
    val location: GeoPoint?) {

    override fun toString(): String {
        return "$distance, $averageSpeed, $speed, $time"
    }
}
