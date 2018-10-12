package edu.artic.details

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.artwork.ArtworkDetailFragment
import edu.artic.artwork.ArtworkDetailViewModel
import edu.artic.events.EventDetailFragment
import edu.artic.events.EventDetailViewModel
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

    @Binds
    @IntoMap
    @ViewModelKey(EventDetailViewModel::class)
    abstract fun eventDetailViewModel(eventDetailViewModel: EventDetailViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val eventDetailFragment: EventDetailFragment

    @Binds
    @IntoMap
    @ViewModelKey(ArtworkDetailViewModel::class)
    abstract fun artworkDetailViewModel(eventDetailViewModel: ArtworkDetailViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val artworkDetailFragment: ArtworkDetailFragment

    @get:ContributesAndroidInjector
    abstract val detailsActivity: DetailsActivity

    @get:ContributesAndroidInjector
    abstract val detailsFragment: EmptyDetailsFragment

}