package com.darekbx.geotracker.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.SimplePointDto

@Dao
interface PointDao {

    @Query("SELECT * FROM point WHERE track_id = :trackId")
    fun fetchByTrack(trackId: Long): LiveData<List<PointDto>>

    @Query("SELECT * FROM point WHERE track_id = :trackId AND ROWID % :nhtTwoToSkip == 0")
    fun fetchByTrackAsync(trackId: Long, nhtTwoToSkip: Int): List<PointDto>

    @Query("SELECT track_id, latitude, longitude FROM point WHERE track_id = :trackId AND ROWID % :nhtTwoToSkip == 0")
    fun fetchSimpleByTrackAsync(trackId: Long, nhtTwoToSkip: Int): List<SimplePointDto>

    @Query("SELECT track_id, latitude, longitude FROM point WHERE ROWID % :nhtTwoToSkip == 0")
    fun fetchAllPoints(nhtTwoToSkip: Int): List<SimplePointDto>

    @Query("DELETE FROM point WHERE track_id = :trackId")
    fun deleteByTrack(trackId: Long)

    @Query("DELETE FROM point WHERE track_id = :trackId AND id >= :idFrom AND id <= :idTo")
    fun deletePoints(trackId: Long, idFrom: Long, idTo: Long)

    @Insert
    fun add(pointDto: PointDto)
}
