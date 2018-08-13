package edu.artic.audioui

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
abstract class AudioUiModule {


    @Binds
    @IntoMap
    @ViewModelKey(BottomAudioPlayerViewModel::class)
    abstract fun bottomAudioPlayerViewModel(bottomAudioPlayerViewModel: BottomAudioPlayerViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val bottomAudioPlayerFragment: BottomAudioPlayerFragment

}
