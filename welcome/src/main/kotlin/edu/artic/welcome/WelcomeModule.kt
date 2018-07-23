package edu.artic.welcome

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey
import javax.inject.Named
import javax.inject.Singleton

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

    @get:ContributesAndroidInjector
    abstract val welcomeActivity: WelcomeActivity

    @Module
    companion object {

        @JvmStatic
        @Provides
        fun provideWelcomePreferenceManager(context : Context) : WelcomePreferencesManager
                = WelcomePreferencesManager(context)

//        @JvmStatic
//        @Provides @Named("welcome")
//        fun welcomeIntent(context: Context): Intent {
//            return Intent(context, WelcomeActivity::class.java)
//        }
    }
}