package com.darekbx.geotracker.repository.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track")
class TrackDto(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: Long,
    @ColumnInfo(name = "end_timestamp") val endTimestamp: Long,
    @ColumnInfo(name = "distance") val distance: Float
)