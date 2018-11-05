package edu.artic.tours.manager

import dagger.Module
import dagger.Provides
import edu.artic.db.daos.ArticAudioFileDao
import javax.inject.Singleton

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class TourManagerModule {
    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        fun tourProgressManager(audioFileDao: ArticAudioFileDao): TourProgressManager = TourProgressManager(audioFileDao)
    }
}