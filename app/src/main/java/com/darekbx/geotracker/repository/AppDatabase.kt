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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Database(entities = [PointDto::class, TrackDto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DB_NAME = "geo_tracker"

        fun restoreDataFromBackup(context: Context, sourcePath: String, callback: (success: Boolean) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                val databaseFile = context.getDatabasePath(DB_NAME)
                try {
                    if (databaseFile.exists()) {
                        context.deleteDatabase(DB_NAME)
                    }
                    FileInputStream(sourcePath).use { inStream ->
                        FileOutputStream(databaseFile.toString()).use { outStream ->
                            FileUtils.copy(inStream, outStream)
                            callback(true)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(false)
                }
            }
        }

        fun makeBackup(context: Context, callback: (path: String?) -> Unit) {
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
                                    callback(uri.path)
                                }
                            } ?: callback(null)
                        } ?: callback(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }

    abstract fun trackDao(): TrackDao
    abstract fun pointDao(): PointDao
}
