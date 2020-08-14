package com.darekbx.geotracker.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.darekbx.geotracker.location.ForegroundTracker
import com.darekbx.geotracker.model.RecordStatus
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.TrackDto
import com.darekbx.geotracker.utils.DateTimeUtils
import kotlinx.coroutines.launch

class TrackViewModel @ViewModelInject constructor(
    private val trackDao: TrackDao,
    private val pointDao: PointDao
) : BaseViewModel() {

    companion object {
        val ONE_KILOMETER = 1000.0F // [meters]
    }

    var recordStatus: LiveData<RecordStatus>? = null
    var updateResult = MutableLiveData<Boolean>()

    val tracks = Transformations.map(trackDao.fetchAll(), { source ->
        source.map {
            mapTrackDtoToTrack(it)
        }
    })

    val newTrackid = MutableLiveData<Long>()

    fun deleteTrack(trackId: Long) {
        ioScope.launch {
            trackDao.delete(trackId)
            pointDao.deleteByTrack(trackId)
        }
    }

    fun fetchTrack(trackId: Long) =
        MutableLiveData<Track>().apply {
            ioScope.launch {
                val trackDto = trackDao.fetch(trackId)
                val track = mapTrackDtoToTrack(trackDto)
                track.points = pointDao.fetchByTrackAsync(trackId)
                postValue(track)
            }
        }

    fun subscribeToPoints(trackId: Long) {
        this@TrackViewModel.recordStatus = Transformations.switchMap(
            pointDao.fetchByTrack(trackId),
            { points ->
                MutableLiveData<RecordStatus>().apply {
                    ioScope.launch {
                        val track = trackDao.fetch(trackId)
                        postValue(RecordStatus(points.size, (track.distance ?: 0.0F) / ONE_KILOMETER))
                    }
                }
            })
    }

    fun updateTrack(trackId: Long, label: String?) {
        ioScope.launch {
            trackDao.update(trackId, label, System.currentTimeMillis())
            updateResult.postValue(true)
        }
    }

    fun createNewTrack() {
        ioScope.launch {
            val track = TrackDto(
                startTimestamp = System.currentTimeMillis(),
                distance = 0.0F
            )
            val rowId = trackDao.add(track)
            newTrackid.postValue(rowId)
        }
    }

    private fun mapTrackDtoToTrack(trackDto: TrackDto): Track {
        return Track(
            trackDto.id,
            trackDto.label ?: "",
            DateTimeUtils.format(trackDto.startTimestamp),
            DateTimeUtils.format(trackDto.endTimestamp),
            (trackDto.distance ?: 0.0F) / ONE_KILOMETER
        )
    }
}