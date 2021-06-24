package com.darekbx.geotracker.repository.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TrackPoints(
    @Embedded val trackDto: TrackDto,
    val pointsCount: Int
)

@Entity(tableName = "track")
class TrackDto(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @ColumnInfo(name = "label") val label: String? = null,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: Long,
    @ColumnInfo(name = "end_timestamp") val endTimestamp: Long? = null,
    @ColumnInfo(name = "distance") val distance: Float? = null
)
