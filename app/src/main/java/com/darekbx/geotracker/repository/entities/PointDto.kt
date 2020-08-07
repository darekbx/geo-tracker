package com.darekbx.geotracker.repository.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "point")
class PointDto(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "track_id") val trackId: Int,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "speed") val speed: Float,
    @ColumnInfo(name = "altitude") val altitude: Double
)