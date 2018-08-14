package edu.artic.media.audio

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class MediaModule {

    @get:ContributesAndroidInjector
    abstract val audioPlayerService: AudioPlayerService

}
