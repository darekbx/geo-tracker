package com.darekbx.geotracker.model

import com.darekbx.geotracker.repository.entities.PointDto

class Track(
    val id: Long?,
    val label: String,
    val startTimestamp: String?,
    val endTimestamp: String?,
    val distance: Float,
    var points: List<PointDto> = emptyList()
)