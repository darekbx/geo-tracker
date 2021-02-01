package com.darekbx.geotracker.repository

import android.content.Context
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import androidx.room.Database
import androidx.room.RoomDatabase
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.TrackDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

@Database(entities = arrayOf(PointDto::class, TrackDto::class), version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val DB_NAME = "geo_tracker"

        fun makeBackup(context: Context, callback: (success: Boolean) -> Unit) {
            try {
                val currentDate = SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().timeInMillis)
                val outFile = "geotracker_db_$currentDate.sqlite"
                val databaseFile = context.getDatabasePath(DB_NAME)
                val contentValues = contentValuesOf(
                    MediaStore.Downloads.DISPLAY_NAME to outFile,
                    MediaStore.Downloads.RELATIVE_PATH to Environment.DIRECTORY_DOWNLOADS
                )
                context.contentResolver.run {
                    CoroutineScope(Dispatchers.IO).launch {
                        insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            contentValues
                        )?.let { uri ->
                            openOutputStream(uri)?.use { outStream ->
                                FileInputStream(databaseFile).use { inStream ->
                                    FileUtils.copy(inStream, outStream)
                                    callback(true)
                                }
                            } ?: callback(false)
                        } ?: callback(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }

    abstract fun trackDao(): TrackDao
    abstract fun pointDao(): PointDao
}
