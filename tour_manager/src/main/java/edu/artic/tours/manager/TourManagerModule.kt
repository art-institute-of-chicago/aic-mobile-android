package edu.artic.tours.manager

import dagger.Module
import dagger.Provides
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
        fun tourProgressManager(): TourProgressManager = TourProgressManager()
    }
}