package edu.artic.events

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
 *@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class EventsModule {

    @Binds
    @IntoMap
    @ViewModelKey(AllEventsViewModel::class)
    abstract fun allEveentsViewModel(allToursViewModel: AllEventsViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val allEventsFragment: AllEventsFragment

}