package com.darekbx.geotracker.model

class RecordStatus(
    val pointsCount: Int,
    val distance: Float,
    val averageSpeed: Float,
    val speed: Float,
    val time: Long) {

    override fun toString(): String {
        return "$distance, $averageSpeed, $speed, $time"
    }
}
