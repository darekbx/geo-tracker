package com.darekbx.geotracker.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.PlaceDto

@Dao
interface PlaceDao {

    @Query("SELECT COUNT(id) FROM place")
    fun countAll(): LiveData<Int>

    @Query("SELECT * FROM place")
    fun fetchAllPlaces(): LiveData<List<PlaceDto>>

    @Query("DELETE FROM place WHERE id = :placeId")
    fun delete(placeId: Long)

    @Insert
    fun add(placeDto: PlaceDto)
}
