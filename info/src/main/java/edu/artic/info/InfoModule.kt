package edu.artic.info

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class InfoModule {


    @Binds
    @IntoMap
    @ViewModelKey(InformationViewModel::class)
    abstract fun informationViewModel(informationViewModel: InformationViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val informationFragment: InformationFragment

    @Binds
    @IntoMap
    @ViewModelKey(MuseumInformationViewModel::class)
    abstract fun museumInformationViewModel(museumInformationViewModel: MuseumInformationViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val museumInformationFragment: MuseumInformationFragment

    @get:ContributesAndroidInjector
    abstract val infoActivity: InfoActivity

}
