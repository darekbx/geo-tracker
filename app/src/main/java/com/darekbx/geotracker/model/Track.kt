package com.darekbx.geotracker.model

import com.darekbx.geotracker.repository.entities.PointDto

class Track(
    val id: Long?,
    val label: String,
    val startTimestamp: String?,
    val endTimestamp: String?,
    val timeDifference: String?,
    val distance: Float,
    var points: List<PointDto> = emptyList(),
    var pointsCount: Int? = null
) {
    val isTimeBroken = startTimestamp?.take(10) != endTimestamp?.take(10)
    fun hasPoints() = (pointsCount ?: 0) > 0
}
