package edu.artic.tours

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.tours.carousel.TourCarouselFragment
import edu.artic.tours.carousel.TourCarouselViewModel
import edu.artic.viewmodel.ViewModelKey

/**
 *@author Sameer Dhakal (Fuzz)
 */
@Module
abstract class ToursModule {

    @Binds
    @IntoMap
    @ViewModelKey(AllToursViewModel::class)
    abstract fun allToursViewModel(allToursViewModel: AllToursViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val allToursFragment: AllToursFragment

    @Binds
    @IntoMap
    @ViewModelKey(TourDetailsViewModel::class)
    abstract fun tourDetailsViewModel(allToursViewModel: TourDetailsViewModel): ViewModel


    @get:ContributesAndroidInjector
    abstract val tourDetailsFragment: TourDetailsFragment

    @get:ContributesAndroidInjector
    abstract val tourCarouselFragment: TourCarouselFragment

    @Binds
    @IntoMap
    @ViewModelKey(TourCarouselViewModel::class)
    abstract fun tourCarouselViewModel(tourCarouselViewModel: TourCarouselViewModel): ViewModel

}