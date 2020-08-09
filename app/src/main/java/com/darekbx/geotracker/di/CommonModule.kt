package com.darekbx.geotracker.di

import android.content.Context
import androidx.room.Room
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
            .build()

    @Singleton
    @Provides
    fun providePointDao(db: AppDatabase) = db.pointDao()

    @Singleton
    @Provides
    fun provideTrackDao(db: AppDatabase) = db.trackDao()
}