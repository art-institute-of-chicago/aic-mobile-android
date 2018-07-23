package edu.artic.info

import android.content.Context
import android.content.Intent
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Named

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class InfoModule {
    abstract val informationFragment: InformationFragment

    @get:ContributesAndroidInjector
    abstract val infoActivity: InfoActivity

    @Module
    companion object {

//        @JvmStatic
//        @Provides
//        @Named(value = "info")
//        fun infoIntent(context: Context): Intent {
//            return Intent(context, InfoActivity::class.java)
//        }
    }
}
