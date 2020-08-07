package com.darekbx.geotracker.viewmodels

import android.location.Location
import androidx.hilt.lifecycle.ViewModelInject
import com.darekbx.geotracker.repository.AppDatabase
import com.darekbx.geotracker.repository.entities.PointDto
import kotlinx.coroutines.launch

class TrackViewModel @ViewModelInject constructor(
    private val appDatabase: AppDatabase
) : BaseViewModel() {

}