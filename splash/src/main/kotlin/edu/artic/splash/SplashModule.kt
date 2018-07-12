package edu.artic.splash

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
 * Description:
 */
@Module
abstract class SplashModule {

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun splashViewModel(splashViewModel: SplashViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val splashActivity: SplashActivity
}