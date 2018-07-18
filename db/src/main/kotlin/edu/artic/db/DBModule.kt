package edu.artic.db

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DBModule {

    @Provides
    @Singleton
    fun provideDB(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, "articdb")
                    .apply { if (BuildConfig.DEBUG) fallbackToDestructiveMigration() } // allow unsafe schema changes in debug
                    .build()

    @Provides
    fun provideDashboardDao(appDatabase: AppDatabase) = appDatabase.dashboardDao

    @Provides
    fun provideGeneralInfoDao(appDatabase: AppDatabase) = appDatabase.generalInfoDao

    @Provides
    fun provideGalleryDao(appDatabase: AppDatabase) = appDatabase.galleryDao

    @Provides
    fun provideObjectDao(appDatabase: AppDatabase) = appDatabase.objectDao

    @Provides
    fun provideAudioFileDao(appDatabase: AppDatabase) = appDatabase.audioFileDao

    @Provides
    fun provideTourDao(appDatabase: AppDatabase) = appDatabase.articTourDao

    @Provides
    fun provideMapAnnotationDao(appDatabase: AppDatabase) = appDatabase.articMapAnnotationDao

    @Provides
    fun provideDataObjectDao(appDatabase: AppDatabase) = appDatabase.dataObjectDao

    @Provides
    fun provideExhibitionDao(appDatabase: AppDatabase) = appDatabase.exhibitionDao

    @Provides
    fun provideEventDao(appDatabase: AppDatabase) = appDatabase.eventDao
}