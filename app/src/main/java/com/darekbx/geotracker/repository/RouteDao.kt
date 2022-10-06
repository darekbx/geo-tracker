package com.darekbx.geotracker.repository

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.darekbx.geotracker.repository.entities.RouteDto
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT COUNT(id) FROM route")
    fun countAll(): LiveData<Int>

    @Query("SELECT * FROM route")
    fun fetchAllRoutes(): Flow<List<RouteDto>>

    @Query("DELETE FROM route WHERE id = :routeId")
    fun delete(routeId: Long)

    @Insert
    fun add(routeDto: RouteDto)
}
