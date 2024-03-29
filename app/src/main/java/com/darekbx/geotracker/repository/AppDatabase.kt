package com.darekbx.geotracker.repository

import android.content.Context
import android.os.Environment
import android.os.FileUtils
import android.provider.MediaStore
import androidx.core.content.contentValuesOf
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.darekbx.geotracker.repository.entities.PlaceDto
import com.darekbx.geotracker.repository.entities.PointDto
import com.darekbx.geotracker.repository.entities.RouteDto
import com.darekbx.geotracker.repository.entities.TrackDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Database(
    entities = [PointDto::class, TrackDto::class, PlaceDto::class, RouteDto::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DB_NAME = "geo_tracker"

        fun restoreDataFromBackup(
            context: Context,
            sourcePath: String,
            callback: (success: Boolean) -> Unit
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                kotlin.runCatching {
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
        }

        fun getDatabaseSize(context: Context): Long {
            return context.getDatabasePath(DB_NAME).length()
        }

        fun makeBackup(context: Context, callback: (path: String?) -> Unit) {
            try {
                val currentDate =
                    SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Calendar.getInstance().timeInMillis)
                val outFile = "geotracker_db_$currentDate.sqlite"
                val databaseFile = context.getDatabasePath(DB_NAME)
                val contentValues = contentValuesOf(
                    MediaStore.Downloads.DISPLAY_NAME to outFile,
                    MediaStore.Downloads.RELATIVE_PATH to Environment.DIRECTORY_DOWNLOADS
                )
                context.contentResolver.run {
                    CoroutineScope(Dispatchers.IO).launch {
                        kotlin.runCatching {
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
CREATE TABLE IF NOT EXISTS `place` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
    `label` TEXT NOT NULL, 
    `latitude` REAL NOT NULL, 
    `longitude` REAL NOT NULL, 
    `timestamp` INTEGER NOT NULL
)"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
CREATE TABLE IF NOT EXISTS `route` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT, 
    `label` TEXT NOT NULL, 
    `url` TEXT NOT NULL, 
    `timestamp` INTEGER NOT NULL
)""")
            }
        }
    }

    abstract fun trackDao(): TrackDao
    abstract fun pointDao(): PointDao
    abstract fun placeDao(): PlaceDao
    abstract fun routeDao(): RouteDao
}
