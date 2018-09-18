package edu.artic.location

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.viewmodel.ViewModelKey

/**
 * @author Piotr Leja (Fuzz)
 */
@Module
abstract class LocationModule {

    @Binds
    @IntoMap
    @ViewModelKey(InfoLocationSettingsViewModel::class)
    abstract fun infoLocationSettingsViewModel(viewModel: InfoLocationSettingsViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val infoLocationSettingsFragment: InfoLocationSettingsFragment

//    @Module
//    companion object {
//
//        @JvmStatic
//        @Provides
//        @Singleton
//        fun provideSearchService(
//                @Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit,
//                dataObjectDao: ArticDataObjectDao
//        ): SearchServiceProvider = RetrofitSearchServiceProvider(retrofit, dataObjectDao)
//    }
}