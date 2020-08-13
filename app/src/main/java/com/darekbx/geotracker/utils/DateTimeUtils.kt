package com.darekbx.geotracker.utils

import java.text.SimpleDateFormat

object DateTimeUtils {

    val DATE_FORMAT = "yyyy-MM-dd HH:mm"

    fun format(timestamp: Long?) = timestamp?.let {
        SimpleDateFormat(DATE_FORMAT).format(it)
    } ?: null
}