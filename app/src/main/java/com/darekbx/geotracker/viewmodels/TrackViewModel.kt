package com.darekbx.geotracker.viewmodels

import androidx.lifecycle.*
import com.darekbx.geotracker.model.DaySummary
import com.darekbx.geotracker.model.RecordStatus
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.SimplePointDto
import com.darekbx.geotracker.repository.entities.TrackDto
import com.darekbx.geotracker.repository.entities.TrackPoints
import com.darekbx.geotracker.utils.AppPreferences
import com.darekbx.geotracker.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val pointDao: PointDao,
    private val appPreferences: AppPreferences
) : BaseViewModel() {

    data class PointScope(val idFrom: Long, val idTo: Long)

    companion object {
        const val ONE_KILOMETER = 1000.0F // [meters]
    }

    var recordStatus: LiveData<RecordStatus>? = null
    var updateResult = MutableLiveData<Boolean>()
    var tracks = MutableLiveData<Map<String?, List<Track>>>()
    var daySummaries = MutableLiveData<List<DaySummary>>()
    var pointsDeleteResult = MutableLiveData<Boolean>()
    var fixResult = MutableLiveData<Boolean>()
    var trackLoadingStatus = MutableLiveData<Boolean>()

    @ExperimentalCoroutinesApi
    fun fetchTracks() {
        trackLoadingStatus.postValue(true)
        viewModelScope.launch {
            val tracksList = tracksFlow().toList()
            val yearTracks =
                tracksList.groupBy { it.startTimestamp?.take(4) /* group by year, take 'yyyy' from 'yyyy-MM-dd HH:mm' date format */ }
            tracks.postValue(yearTracks)
            trackLoadingStatus.postValue(false)
        }
    }

    @ExperimentalCoroutinesApi
    fun fetchDaySummaries() {
        viewModelScope.launch {
            val tracks = tracksFlow().toList()
            val sumDistances = tracks
                .groupBy { it.startTimestamp!!.take(10) }
                .mapValues { entry -> entry.value.sumOf { it.distance.toDouble() } }
                .map { DaySummary(it.key, it.value) }
            daySummaries.postValue(sumDistances)
        }
    }

    @ExperimentalCoroutinesApi
    private fun tracksFlow(): Flow<Track> {
        return flow {
            trackDao.fetchAll2().forEach { trackDto ->
                val track = mapTrackDtoToTrack(trackDto)
                emit(track)
            }
        }.flowOn(Dispatchers.IO)
    }

    val newTrackid = MutableLiveData<Long>()

    fun deleteTrackPoints(trackId: Long) {
        ioScope.launch {
            pointDao.deleteByTrack(trackId)
            pointsDeleteResult.postValue(true)
        }
    }

    fun deleteTrackPoints(trackId: Long, startPoints: List<PointDto>, endPoints: List<PointDto>) {
        ioScope.launch {
            val pointScopes = arrayOf(
                createPointScope(startPoints),
                createPointScope(endPoints)
            )
            for (scope in pointScopes) {
                pointDao.deletePoints(trackId, scope.idFrom, scope.idTo)
            }
            pointsDeleteResult.postValue(true)
        }
    }

    private fun createPointScope(points: List<PointDto>): PointScope {
        val startId = points.mapNotNull { it.id }.minByOrNull { it } ?: 0L
        val endId = points.mapNotNull { it.id }.maxByOrNull { it } ?: 0L
        return PointScope(startId, endId)
    }

    fun deleteTrack(trackId: Long, onCompleted: () -> Unit = { }) {
        ioScope.launch {
            trackDao.delete(trackId)
            pointDao.deleteByTrack(trackId)
            onCompleted()
        }
    }

    fun fetchAllPoints(nthPointsToSkip: Int = appPreferences.nthPointsToSkip): LiveData<Map<Long, List<SimplePointDto>>> {
        return MutableLiveData<Map<Long ,List<SimplePointDto>>>().apply {
            ioScope.launch {
                val points = pointDao
                    .fetchAllPoints(nthPointsToSkip)
                    .groupBy { it.trackId }
                postValue(points)
            }
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

    fun fetchSimpleTrack(trackId: Long) =
        MutableLiveData<Track>().apply {
            ioScope.launch {
                trackDao.fetch(trackId)?.let { trackDto ->
                    val track = mapTrackDtoToTrack(trackDto)
                    track.simplePoints = pointDao.fetchSimpleByTrackAsync(trackId, 1 /* dont skip nth rows */)
                    postValue(track)
                }
            }
        }

    fun subscribeToPoints(trackId: Long) {
        this@TrackViewModel.recordStatus = Transformations.switchMap(
            pointDao.fetchByTrack(trackId)
        )
        { points ->
            MutableLiveData<RecordStatus>().apply {
                ioScope.launch {
                    val track = trackDao.fetch(trackId)
                    val distance = (track?.distance ?: 0.0F) / ONE_KILOMETER
                    val averageSpeed = if (points.size > 1) averageSpeed(points) else 0F
                    val speed = if (points.isEmpty()) 0.0F else points.last().speed
                    val time = (System.currentTimeMillis() - (track?.startTimestamp ?: 0L)) / 1000

                    val maxPoint = points.maxByOrNull { it.timestamp }
                    val location = maxPoint?.let { GeoPoint(maxPoint.latitude, maxPoint.longitude) }
                    postValue(RecordStatus(points.size, distance, averageSpeed, speed, time, location))
                }
            }
        }
    }

    private fun averageSpeed(points: List<PointDto>) =
        points.map { it.speed }.average().toFloat()

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

    fun fixDate(trackId: Long) {
        ioScope.launch {
            val track = trackDao.fetch(trackId)
            if (track != null) {
                /**
                 * Some tracks have broken endtimestamp, they will be fixed by chaning endtimestamp to
                 * startTimestamp plus one hour
                 */
                val endTimestamp = track.startTimestamp + TimeUnit.HOURS.toMillis(1)
                trackDao.updateDate(trackId, endTimestamp)
                fixResult.postValue(true)
            } else {
                fixResult.postValue(false)
            }
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

    private fun mapTrackDtoToTrack(trackPoints: TrackPoints): Track {
        return mapTrackDtoToTrack(trackPoints.trackDto).apply {
            pointsCount = trackPoints.pointsCount
        }
    }

    private fun getFormattedTimeDiff(start: Long, end: Long): String {
        val time = (end - start) / 1000
        return DateTimeUtils.getFormattedTime(time.toInt())
    }
}
