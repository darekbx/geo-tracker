package com.darekbx.geotracker.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.PointDto

@Dao
interface PointDao {

    @Query("SELECT * FROM point WHERE track_id = :trackId")
    fun fetchByTrack(trackId: Long): LiveData<List<PointDto>>

    @Query("DELETE FROM point WHERE track_id = :trackId")
    fun deleteByTrack(trackId: Long)

    @Insert
    fun add(pointDto: PointDto)
}