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
    abstract fun allEventsViewModel(allEventsViewModel: AllEventsViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val allEventsFragment: AllEventsFragment

    @Binds
    @IntoMap
    @ViewModelKey(EventDetailViewModel::class)
    abstract fun allEveentsViewModel(eventDetailViewModel: EventDetailViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val eventDetailFragment: EventDetailFragment

}