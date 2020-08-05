package com.darekbx.geotracker.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.TrackDto

@Dao
interface TrackDao {

    @Query("SELECT * FROM track")
    fun fetchAll(): List<TrackDto>

    @Insert
    fun add(trackDto: TrackDto)

    @Delete
    fun delete(trackDto: TrackDto)
}