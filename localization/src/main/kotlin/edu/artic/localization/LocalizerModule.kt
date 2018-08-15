package edu.artic.localization

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
        fun provideLanguageSelector(context: Context): LanguageSelector {
            return LanguageSelector(LocalizationPreferences(context))
        }

    }

}
