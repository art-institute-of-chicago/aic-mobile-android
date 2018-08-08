package edu.artic.map

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
abstract class MapModule {


    @Binds
    @IntoMap
    @ViewModelKey(MapViewModel::class)
    abstract fun mapViewModel(mapViewModel: MapViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val mapActivity: MapActivity

    @get:ContributesAndroidInjector
    abstract val mapFragment: MapFragment

    @get:ContributesAndroidInjector
    abstract val mapObjectDetailsFragment: MapObjectDetailsFragment

}
