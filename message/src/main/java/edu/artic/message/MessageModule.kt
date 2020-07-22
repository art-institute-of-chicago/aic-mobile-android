package edu.artic.message

import android.arch.lifecycle.ViewModel
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey
import javax.inject.Singleton

@Module
abstract class MessageModule {

    @get:ContributesAndroidInjector
    abstract val pagedMessageFragment: PagedMessageFragment

    @Binds
    @IntoMap
    @ViewModelKey(PagedMessageViewModel::class)
    abstract fun pagedMessageViewModel(viewModel: PagedMessageViewModel): ViewModel

    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        fun provideMessagePreferencesManager(context: Context) = MessagePreferencesManager(context)

    }

}
