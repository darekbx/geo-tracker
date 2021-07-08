package com.darekbx.geotracker.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

object DateTimeUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm"

    @SuppressLint("SimpleDateFormat")
    fun format(timestamp: Long?) = timestamp?.let {
        SimpleDateFormat(DATE_FORMAT).format(it)
    }

    fun getFormattedTime(timeInSeconds: Int): String {
        var time = timeInSeconds
        val hours = time / 3600
        time %= 3600
        val minutes = time / 60
        time %= 60
        val seconds = time
        return "${hours}h ${minutes}m ${seconds}s"
    }
}
