package edu.artic.map

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class MapModule {

    @get:ContributesAndroidInjector
    abstract val mapActivity: MapActivity

}
