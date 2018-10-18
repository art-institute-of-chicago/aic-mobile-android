package edu.artic.location

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * @author Piotr Leja (Fuzz)
 */
@Module
abstract class LocationModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        fun provideLocationService(
                context: Context,
                locationPreferenceManager: LocationPreferenceManager
        ): LocationService = LocationServiceImpl(context.applicationContext, locationPreferenceManager)

        @JvmStatic
        @Provides
        @Singleton
        fun provideLocationPreferenceManager(
                context: Context
        ): LocationPreferenceManager = LocationPreferenceManager(context)
    }
}