package com.darekbx.geotracker.repository.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
data class SimplePointDto(
    @ColumnInfo(name = "track_id") val trackId: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double
)