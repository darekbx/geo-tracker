package com.darekbx.geotracker.model

class Progress(val value: Int, val max: Int) {

    val isCompleted = max == value
}
