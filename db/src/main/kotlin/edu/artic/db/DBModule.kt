package edu.artic.db

import android.arch.persistence.room.Room
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * This dagger2 module allows access to each of the
 * [DAOs][android.arch.persistence.room.Dao] in the application.
 *
 * Here, one DAO usually represents exactly one type of persisted
 * model - so there's an [ArticObjectDao][edu.artic.db.daos.ArticObjectDao]
 * for [ArticObjects][edu.artic.db.models.ArticObject], a
 * [ArticGalleryDao][edu.artic.db.daos.ArticGalleryDao] for
 * [ArticGalleries][edu.artic.db.models.ArticGallery], and so forth.
 *
 * Of special note is [provideDB] - this returns a singleton
 * [AppDatabase], within which is stored every other DAO offered in
 * this file. We broadly recommend against injecting the entire
 * database into other parts of the application.
 *
 * @see AppDataManager
 */
@Module
class DBModule {

    @Provides
    @Singleton
    fun provideDB(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, "articdb")
                    // TODO: Once we've switched to Androidx, use safe migrations instead
                    .fallbackToDestructiveMigration()
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
    fun provideTourDao(appDatabase: AppDatabase) = appDatabase.tourDao

    @Provides
    fun provideMapAnnotationDao(appDatabase: AppDatabase) = appDatabase.mapAnnotationDao

    @Provides
    fun provideDataObjectDao(appDatabase: AppDatabase) = appDatabase.dataObjectDao

    @Provides
    fun provideExhibitionDao(appDatabase: AppDatabase) = appDatabase.exhibitionDao

    @Provides
    fun provideEventDao(appDatabase: AppDatabase) = appDatabase.eventDao

    @Provides
    fun provideSearchSuggestionsDao(appDatabase: AppDatabase) = appDatabase.searchSuggestionDao

    @Provides
    fun provideArticExhibitionCMSDao(appDatabase: AppDatabase) = appDatabase.exhibitionCMSDao

    @Provides
    fun provideArticMapFloorDao(appDatabase: AppDatabase) = appDatabase.articMapFloorDao
}