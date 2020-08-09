package com.darekbx.geotracker.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.darekbx.geotracker.model.Track
import com.darekbx.geotracker.repository.PointDao
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.TrackDto
import kotlinx.coroutines.launch

class TrackViewModel @ViewModelInject constructor(
    private val trackDao: TrackDao,
    private val pointDao: PointDao
) : BaseViewModel() {

    var points: LiveData<List<PointDto>>? = null

    val tracks = Transformations.map(trackDao.fetchAll(), { source ->
        source.map { Track(it.id, it.label, it.startTimestamp, it.endTimestamp, it.distance) }
    })

    val newTrackid = MutableLiveData<Long>()

    fun subscribeToPoints(trackId: Long) {
        this@TrackViewModel.points = pointDao.fetchByTrack(trackId)
    }

    fun createNewTrack() {
        ioScope.launch {
            val rowId = trackDao.add(TrackDto(startTimestamp = System.currentTimeMillis()))
            newTrackid.postValue(rowId)
        }
    }
}