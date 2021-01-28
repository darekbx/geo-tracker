package com.darekbx.geotracker.model

import okhttp3.FormBody

class LiveLocation(
    val lat: Double,
    val lng: Double,
    val speed: Float,
    val timestamp: Long,
    val track_id: Long
) {

    fun asFormBody() = FormBody.Builder()
        .add("lat", "${lat}")
        .add("lng", "${lng}")
        .add("speed", "${speed}")
        .add("timestamp", "${timestamp}")
        .add("track_id", "${track_id}")
        .build()
}
