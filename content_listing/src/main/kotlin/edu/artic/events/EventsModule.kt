package edu.artic.events

import androidx.lifecycle.ViewModel
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

}