package com.darekbx.geotracker.repository

import androidx.room.*
import com.darekbx.geotracker.repository.entities.SimplePointDto
import com.darekbx.geotracker.repository.entities.TrackDto
import com.darekbx.geotracker.repository.entities.TrackPoints

@Dao
interface TrackDao {

    @Query("""
SELECT 
	track.*,
	COUNT(point.id) AS `pointsCount`
FROM track 
LEFT OUTER JOIN point ON point.track_id = track.id
GROUP BY track.id
ORDER BY track.id DESC
    """)
    fun fetchAll2(): List<TrackPoints>

    @Query("SELECT * FROM track ORDER BY id DESC")
    fun fetchAll(): List<TrackDto>

    @Query("SELECT COUNT(id) FROM track LIMIT 1")
    fun countAllTracks(): Int

    @Deprecated("Use fetchAllPoints")
    @Query("SELECT * FROM track ORDER BY id ASC")
    fun fetchAllAscending(): List<TrackDto>

    @Query("SELECT * FROM track WHERE id = :trackId")
    fun fetch(trackId: Long): TrackDto?

    @Query("UPDATE track SET label = :label, end_timestamp = :endTimestamp WHERE id = :trackId")
    fun update(trackId: Long, label: String?, endTimestamp: Long)

    @Query("UPDATE track SET end_timestamp = :endTimestamp WHERE id = :trackId")
    fun update(trackId: Long, endTimestamp: Long)

    @Query("UPDATE track SET distance = distance + :distance WHERE id = :trackId")
    fun appendDistance(trackId: Long, distance: Float)

    @Insert
    fun add(trackDto: TrackDto): Long

    @Query("DELETE FROM track WHERE id = :trackId")
    fun delete(trackId: Long)

    @Query("UPDATE track SET end_timestamp = :endTimestamp WHERE id = :trackId")
    fun updateDate(trackId: Long, endTimestamp: Long)
}
