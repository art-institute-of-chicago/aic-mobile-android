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
}