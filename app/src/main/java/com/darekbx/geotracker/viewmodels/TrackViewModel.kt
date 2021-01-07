package com.darekbx.geotracker.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.darekbx.geotracker.model.RecordStatus
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.TrackDto
import com.darekbx.geotracker.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class TrackViewModel @ViewModelInject constructor(
    private val trackDao: TrackDao,
    private val pointDao: PointDao
) : BaseViewModel() {

    companion object {
        val ONE_KILOMETER = 1000.0F // [meters]
        val NTH_POINT_TO_SKIP = 5 // Skip rows to increase performance when displaying all tracks on map
    }

    var recordStatus: LiveData<RecordStatus>? = null
    var updateResult = MutableLiveData<Boolean>()
    var tracksWithPoints = MutableLiveData<List<Track>>()
    var tracks = MutableLiveData<Map<String?, List<Track>>>()

    fun fetchTracks() {
        viewModelScope.launch {
            val tracksList = tracksFlow().toList()
            val yearTracks = tracksList.groupBy { it.startTimestamp?.take(4) /* group by year */ }
            tracks.postValue(yearTracks)
        }
    }

    private fun tracksFlow() : Flow<Track> {
        return flow {
            trackDao.fetchAll().forEach { trackDto ->
                val track = mapTrackDtoToTrack(trackDto)
                emit(track)
            }
        }.flowOn(Dispatchers.IO)
    }

    val newTrackid = MutableLiveData<Long>()

    fun deleteTrack(trackId: Long) {
        ioScope.launch {
            trackDao.delete(trackId)
            pointDao.deleteByTrack(trackId)
        }
    }

    fun fetchTracksWithPoints() {
        ioScope.launch {
            val tracksWithPoints =
                trackDao
                    .fetchAll()
                    .map { track ->
                        val trackPoints = pointDao.fetchByTrackAsync(
                            track.id ?: throw IllegalStateException("Empty id"),
                            NTH_POINT_TO_SKIP
                        )
                        mapTrackDtoToTrack(track).apply {
                            points = trackPoints
                        }
                    }
            this@TrackViewModel.tracksWithPoints.postValue(tracksWithPoints)
        }
    }

    fun fetchTrack(trackId: Long) =
        MutableLiveData<Track>().apply {
            ioScope.launch {
                trackDao.fetch(trackId)?.let { trackDto ->
                    val track = mapTrackDtoToTrack(trackDto)
                    track.points = pointDao.fetchByTrackAsync(trackId, 1 /* dont skip nth rows */)
                    postValue(track)
                }
            }
        }

    fun subscribeToPoints(trackId: Long) {
        this@TrackViewModel.recordStatus = Transformations.switchMap(
            pointDao.fetchByTrack(trackId),
            { points ->
                MutableLiveData<RecordStatus>().apply {
                    ioScope.launch {
                        val track = trackDao.fetch(trackId)
                        val distance = (track?.distance ?: 0.0F) / ONE_KILOMETER
                        val averageSpeed = if (points.size > 1) points.map { it.speed }.average().toFloat() else 0F
                        val time = (System.currentTimeMillis() - (track?.startTimestamp ?: 0L)) / 1000
                        postValue(RecordStatus(points.size, distance, averageSpeed, time))
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
        var difference = ""
        if (trackDto.endTimestamp != null) {
            difference = getFormattedTimeDiff(trackDto.startTimestamp, trackDto.endTimestamp)
        }
        return Track(
            trackDto.id,
            trackDto.label ?: "",
            DateTimeUtils.format(trackDto.startTimestamp),
            DateTimeUtils.format(trackDto.endTimestamp),
            difference,
            (trackDto.distance ?: 0.0F) / ONE_KILOMETER
        )
    }

    private fun getFormattedTimeDiff(start: Long, end: Long): String {
        var time = (end - start) / 1000
        return DateTimeUtils.getFormattedTime(time.toInt())
    }
}
