package edu.artic.media.ui

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
abstract class MediaUiModule {


    @Binds
    @IntoMap
    @ViewModelKey(NarrowAudioPlayerViewModel::class)
    abstract fun narrowAudioPlayerViewModel(narrowAudioPlayerViewModel: NarrowAudioPlayerViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val narrowAudioPlayerFragment: NarrowAudioPlayerFragment

    @Binds
    @IntoMap
    @ViewModelKey(AudioTutorialViewModel::class)
    abstract fun audioTutorialViewModel(audioTutorialViewModel: AudioTutorialViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val audioTutorialActivity: AudioTutorialActivity

    @get:ContributesAndroidInjector
    abstract val audioDetailsActivity: AudioDetailsActivity

}
