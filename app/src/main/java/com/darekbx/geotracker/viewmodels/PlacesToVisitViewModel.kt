package com.darekbx.geotracker.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.darekbx.geotracker.model.PlaceToVisit
import com.darekbx.geotracker.repository.PlaceDao
import com.darekbx.geotracker.repository.entities.PlaceDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class PlacesToVisitViewModel @ViewModelInject constructor(
    private val placeDao: PlaceDao
): ViewModel() {

    fun add(label: String, location: GeoPoint) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                placeDao.add(
                    PlaceDto(
                        label = label,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun delete(placeId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                placeDao.delete(placeId)
            }
        }
    }

    fun listAll(): LiveData<List<PlaceToVisit>> =
        Transformations.map(
            placeDao.fetchAllPlaces(),
            { placeDtoList ->
                placeDtoList.map { dto ->
                    placeDtoToModel(dto)
                }
            }
        )

    private fun placeDtoToModel(placeDto: PlaceDto): PlaceToVisit =
        with(placeDto) {
            PlaceToVisit(id!!, label, latitude, longitude, timestamp)
        }
}
