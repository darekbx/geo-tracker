package com.darekbx.geotracker.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.TrackDto

@Database(entities = arrayOf(PointDto::class, TrackDto::class), version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val DB_NAME = "geo_tracker"
    }

    abstract fun trackDao(): TrackDao
    abstract fun pointDao(): PointDao
}