package edu.artic.location

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
 * @author Piotr Leja (Fuzz)
 */
@Module
abstract class LocationUIModule {

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
}