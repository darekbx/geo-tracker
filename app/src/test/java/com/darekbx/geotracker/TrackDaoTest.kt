package com.darekbx.geotracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darekbx.geotracker.repository.AppDatabase
import com.darekbx.geotracker.repository.TrackDao
import com.darekbx.geotracker.repository.entities.TrackDto
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TrackDaoTest {
    private lateinit var trackDao: TrackDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trackDao = db.trackDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun `Test appendDistance`() {
        // Given
        val trackDto = TrackDto(null, "Label", 100, 200, 0.0F)
        val trackId = trackDao.add(trackDto)
        assert(trackDao.fetch(trackId).distance == 0.0F)

        // When
        trackDao.appendDistance(trackId, 10F)
        trackDao.appendDistance(trackId, 20F)

        // Then
        assert(trackDao.fetch(trackId).distance == 30F)
    }
}
