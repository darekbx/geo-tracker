package com.darekbx.geotracker.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.TrackDto

@Dao
interface TrackDao {

    @Query("SELECT * FROM track")
    fun fetchAll(): LiveData<List<TrackDto>>

    @Insert
    fun add(trackDto: TrackDto): Long

    @Delete
    fun delete(trackDto: TrackDto)
}