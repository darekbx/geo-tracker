package com.darekbx.geotracker.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class BaseViewModel : ViewModel() {

    private val viewModelJob = SupervisorJob()

    protected val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    protected val ioScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}