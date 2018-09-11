package edu.artic.details

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.exhibitions.ExhibitionDetailViewModel
import edu.artic.tours.TourDetailsFragment
import edu.artic.tours.TourDetailsViewModel
import edu.artic.viewmodel.ViewModelKey

/**
 *@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class DetailsModule {

    @Binds
    @IntoMap
    @ViewModelKey(TourDetailsViewModel::class)
    abstract fun tourDetailsViewModel(allToursViewModel: TourDetailsViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val tourDetailsFragment: TourDetailsFragment



    @Binds
    @IntoMap
    @ViewModelKey(ExhibitionDetailViewModel::class)
    abstract fun exhibitionDetailsViewModel(exhibitionDetailViewModel: ExhibitionDetailViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val exhibitionDetailFragment: ExhibitionDetailFragment

}