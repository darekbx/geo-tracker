package com.darekbx.geotracker.livetracking

import com.darekbx.geotracker.model.LiveLocation

interface ILiveTracker {

    fun notifyLocation(liveLocation: LiveLocation)
}
