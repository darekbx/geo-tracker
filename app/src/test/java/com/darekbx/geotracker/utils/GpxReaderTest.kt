package com.darekbx.geotracker.utils

import junit.framework.TestCase.assertEquals
import org.junit.Test

class GpxReaderTest {

    @Test
    fun readGpx() {
        // Given
        val gpxReader = GpxReader()

        // When
        val gpx = gpxReader.readGpx(xmlString.byteInputStream())

        // Then
        assertEquals("Trirent - Kabacki + Konstancin",  gpx.name)
        assertEquals(3,  gpx.points.size)
    }

    val xmlString = """<?xml version="1.0" encoding="UTF-8"?>
        |<gpx creator="StravaGPX" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |     xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"
        |     version="1.1" xmlns="http://www.topografix.com/GPX/1/1">
        |    <metadata>
        |        <name>Trirent - Kabacki + Konstancin </name>
        |        <author>
        |            <name>Maria Kostacińska</name>
        |            <link href="https://www.strava.com/athletes/10027729"/>
        |        </author>
        |        <copyright author="OpenStreetMap contributors">
        |            <year>2020</year>
        |            <license>https://www.openstreetmap.org/copyright</license>
        |        </copyright>
        |        <link href="https://www.strava.com/routes/3002147959263604506"/>
        |    </metadata>
        |    <trk>
        |        <name>Trirent - Kabacki + Konstancin </name>
        |        <link href="https://www.strava.com/routes/3002147959263604506"/>
        |        <type>Ride</type>
        |        <trkseg>
        |            <trkpt lat="52.19489" lon="20.998150000000003">
        |                <ele>109.30000000000001</ele>
        |            </trkpt>
        |            <trkpt lat="52.19505" lon="20.998220000000003">
        |                <ele>109.26</ele>
        |            </trkpt>
        |            <trkpt lat="52.19521" lon="20.998060000000002">
        |                <ele>109.26</ele>
        |            </trkpt>
        |        </trkseg>
        |    </trk>
        |</gpx>
    """.trimMargin()
}