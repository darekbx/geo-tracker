package com.darekbx.geotracker.model

import com.darekbx.geotracker.repository.entities.PointDto

class Track(
    val id: Long?,
    val label: String,
    val startTimestamp: String?,
    val endTimestamp: String?,
    val timeDifference: String?,
    val distance: Float,
    var points: List<PointDto> = emptyList()
) {
    val isTimeBroken = startTimestamp?.take(10) != endTimestamp?.take(10)
}
