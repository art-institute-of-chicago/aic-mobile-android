package edu.artic.welcome

import android.arch.lifecycle.ViewModel
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
 *@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class WelcomeModule {

    @Binds
    @IntoMap
    @ViewModelKey(WelcomeViewModel::class)
    abstract fun welcomeViewModel(welcomeViewModel: WelcomeViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val welcomeFragment: WelcomeFragment

}