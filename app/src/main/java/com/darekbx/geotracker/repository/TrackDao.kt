package com.darekbx.geotracker.repository

import androidx.lifecycle.LiveData
import androidx.room.*
import com.darekbx.geotracker.repository.entities.TrackDto

@Dao
interface TrackDao {

    @Query("SELECT * FROM track")
    fun fetchAll(): LiveData<List<TrackDto>>

    @Query("SELECT * FROM track WHERE id = :trackId")
    fun fetch(trackId: Long): TrackDto?

    @Query("UPDATE track SET label = :label, end_timestamp = :endTimestamp WHERE id = :trackId")
    fun update(trackId: Long, label: String?, endTimestamp: Long)

    @Query("UPDATE track SET distance = distance + :distance WHERE id = :trackId")
    fun appendDistance(trackId: Long, distance: Float)

    @Insert
    fun add(trackDto: TrackDto): Long

    @Query("DELETE FROM track WHERE id = :trackId")
    fun delete(trackId: Long)
}