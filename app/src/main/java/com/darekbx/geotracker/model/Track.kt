package com.darekbx.geotracker.model

class Track(
    val id: Long?,
    val label: String?,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val distance: Float?
)