package com.darekbx.geotracker.utils

import android.util.Log
import org.osmdroid.util.GeoPoint
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

data class Gpx(val name: String, val points: List<GeoPoint>)

class GpxReader {

    fun readGpx(inputStream: InputStream): Gpx {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var currentTag = ""
        var name = ""

        val points = mutableListOf<GeoPoint>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag) {
                        "trkpt" -> {
                            val lat = parser.getAttributeValue(null, "lat")
                            val lon = parser.getAttributeValue(null, "lon")
                            if (lat != null && lon != null) {
                                points.add(GeoPoint(lat.toDouble(), lon.toDouble()))
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    when(currentTag) {
                        "name" -> {
                            Log.v("SIGMA", "name: ${parser.text}")
                            if (parser.text?.trim()?.isNotBlank() == true) {
                                name = parser.text
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return Gpx(name, points)
    }
}
