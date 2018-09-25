package edu.artic.media.audio

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import javax.inject.Singleton

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class MediaModule {

    @get:ContributesAndroidInjector
    abstract val audioPlayerService: AudioPlayerService

    @Module
    companion object {

        @JvmStatic
        @Provides
        fun provideAudioPrefManager(context: Context):
                AudioPrefManager = AudioPrefManager(context)

        @JvmStatic
        @Provides
        @Singleton
        fun provideAudioServiceHook(): AudioServiceHook = AudioServiceHook()

    }
}
