package edu.artic.localizer

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class LocalizerModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        fun provideLocalizer(context: Context): Localizer {
            return Localizer(LocalizerPreferences(context))
        }

    }

}
