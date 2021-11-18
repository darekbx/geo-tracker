package com.darekbx.geotracker.di

import android.content.Context
import androidx.room.Room
import com.darekbx.geotracker.livetracking.ILiveTracker
import com.darekbx.geotracker.livetracking.HerokuLiveTracker
import com.darekbx.geotracker.location.LastKnownLocation
import com.darekbx.geotracker.repository.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object CommonModule {

    @Singleton
    @Provides
    fun appDatabase(@ApplicationContext context: Context) =
        Room
            .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Singleton
    @Provides
    fun providePointDao(db: AppDatabase) = db.pointDao()

    @Singleton
    @Provides
    fun provideTrackDao(db: AppDatabase) = db.trackDao()

    @Singleton
    @Provides
    fun providePlaceDao(db: AppDatabase) = db.placeDao()

    @Singleton
    @Provides
    fun provideTracker(): ILiveTracker = HerokuLiveTracker()

    @Singleton
    @Provides
    fun provideLastKnownLocation(@ApplicationContext context: Context): LastKnownLocation =
        LastKnownLocation(context)
}
