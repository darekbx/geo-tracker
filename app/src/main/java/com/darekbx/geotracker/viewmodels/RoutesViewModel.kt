package com.darekbx.geotracker.viewmodels

import androidx.lifecycle.*
import com.darekbx.geotracker.model.Route
import com.darekbx.geotracker.repository.RouteDao
import com.darekbx.geotracker.repository.entities.RouteDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class RoutesUiState {
    data class Success(
        val routes: List<Route>
    ): RoutesUiState()
    data class Error(val exception: Throwable): RoutesUiState()
}

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routeDao: RouteDao
): ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState.Success(emptyList()))
    val uiState: StateFlow<RoutesUiState> = _uiState

    init {
        viewModelScope.launch {
            routeDao
                .fetchAllRoutes()
                .map { placeDtoList ->
                    placeDtoList.map { dto ->
                        placeDtoToModel(dto)
                    }
                }
                .collect { favoriteNews ->
                    _uiState.value = RoutesUiState.Success(favoriteNews)
                }
        }
    }

    fun add(label: String, url: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                routeDao.add(
                    RouteDto(
                        label = label,
                        url = url,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun delete(routeId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                routeDao.delete(routeId)
            }
        }
    }

    fun countAll() = routeDao.countAll()

    private fun placeDtoToModel(routeDto: RouteDto): Route =
        with(routeDto) {
            Route(id!!, label, url, timestamp)
        }
}
