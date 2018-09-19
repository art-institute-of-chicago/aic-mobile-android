package edu.artic.location

import android.arch.lifecycle.ViewModel
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey
import javax.inject.Singleton

/**
 * @author Piotr Leja (Fuzz)
 */
@Module
abstract class LocationModule {

    @Binds
    @IntoMap
    @ViewModelKey(InfoLocationSettingsViewModel::class)
    abstract fun infoLocationSettingsViewModel(viewModel: InfoLocationSettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LocationPromptViewModel::class)
    abstract fun locationPromptViewModel(viewModel: LocationPromptViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val infoLocationSettingsFragment: InfoLocationSettingsFragment

    @get:ContributesAndroidInjector
    abstract val locationPromptFragment: LocationPromptFragment

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