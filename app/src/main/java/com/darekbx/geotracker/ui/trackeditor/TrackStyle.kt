package com.darekbx.geotracker.ui.trackeditor

sealed class TrackStyle(
    val color: Int,
    val width: Float
) {

    class Shadowed(color: Int, width: Float) : TrackStyle(color, width)
    class Current(color: Int, width: Float) : TrackStyle(color, width)
}
