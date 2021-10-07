package com.darekbx.geotracker.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

object DateTimeUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm"

    @SuppressLint("SimpleDateFormat")
    fun format(timestamp: Long?) = timestamp?.let {
        SimpleDateFormat(DATE_FORMAT).format(it)
    }

    fun getFormattedTime(timeInSeconds: Int, showSeconds: Boolean = true): String {
        var time = timeInSeconds
        val hours = time / 3600
        time %= 3600
        val minutes = time / 60
        time %= 60
        val seconds = time
        return if (showSeconds)
            "${hours.pad()}h ${minutes.pad()}m ${seconds.pad()}s"
        else
            "${hours.pad()}h ${minutes.pad()}m"
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
