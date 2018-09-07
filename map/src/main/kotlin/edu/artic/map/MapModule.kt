package edu.artic.map

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.map.carousel.TourCarouselFragment
import edu.artic.map.carousel.TourCarouselViewModel
import edu.artic.map.carousel.TourProgressManager
import edu.artic.viewmodel.ViewModelKey
import javax.inject.Singleton

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class MapModule {


    @Binds
    @IntoMap
    @ViewModelKey(MapViewModel::class)
    abstract fun mapViewModel(mapViewModel: MapViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MapObjectDetailsViewModel::class)
    abstract fun mapObjectDetailsViewModel(mapViewModel: MapObjectDetailsViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val mapActivity: MapActivity

    @get:ContributesAndroidInjector
    abstract val mapFragment: MapFragment

    @get:ContributesAndroidInjector
    abstract val mapObjectDetailsFragment: MapObjectDetailsFragment

    @get:ContributesAndroidInjector
    abstract val tourCarouselFragment: TourCarouselFragment

    @Binds
    @IntoMap
    @ViewModelKey(TourCarouselViewModel::class)
    abstract fun tourCarouselViewModel(tourCarouselViewModel: TourCarouselViewModel): ViewModel

    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        fun tourProgressManager(): TourProgressManager = TourProgressManager()

        @JvmStatic
        @Provides
        @Singleton
        fun searchManager(): SearchManager = SearchManager()

    }
}
