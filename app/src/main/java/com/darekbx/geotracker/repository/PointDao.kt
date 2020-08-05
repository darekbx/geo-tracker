package com.darekbx.geotracker.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.PointDto

@Dao
interface PointDao {

    @Query("SELECT * FROM point WHERE track_id = :trackId")
    fun fetchByTrack(trackId: Int)

    @Query("DELETE FROM point WHERE track_id = :trackId")
    fun deleteByTrack(trackId: Int)

    @Insert
    fun add(pointDto: PointDto)
}