package edu.artic.exhibitions

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.exhibitions.AllExhibitionsFragment
import edu.artic.exhibitions.AllExhibitionsViewModel
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.exhibitions.ExhibitionDetailViewModel
import edu.artic.viewmodel.ViewModelKey

/**
 *@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class ExhibitionsModule {

    @Binds
    @IntoMap
    @ViewModelKey(AllExhibitionsViewModel::class)
    abstract fun allExhibitionsViewModel(allToursViewModel: AllExhibitionsViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val allExhibitionsFragment: AllExhibitionsFragment


    @Binds
    @IntoMap
    @ViewModelKey(ExhibitionDetailViewModel::class)
    abstract fun exhibitionDetailsViewModel(exhibitionDetailViewModel: ExhibitionDetailViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val exhibitionDetailFragment: ExhibitionDetailFragment

}