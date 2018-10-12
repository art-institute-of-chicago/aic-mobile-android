package edu.artic.search

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.db.ApiModule
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.viewmodel.ViewModelKey
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * @author Sameer Dhakal (Fuzz)
 */
@Module
abstract class SearchModule {

    @Binds
    @IntoMap
    @ViewModelKey(SearchViewModel::class)
    abstract fun searchViewModel(splashViewModel: SearchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DefaultSearchSuggestionsViewModel::class)
    abstract fun defaultSearchSuggestionsViewModel(defaultSearchSuggestionsViewModel: DefaultSearchSuggestionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchResultsContainerViewModel::class)
    abstract fun searchResultsViewModel(searchResultsContainerViewModel: SearchResultsContainerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchExhibitionsViewModel::class)
    abstract fun searchResultsExhibitionsViewModel(searchResultsExhibitionsViewModel: SearchExhibitionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchArtworkViewModel::class)
    abstract fun searchResultsArtworkViewModel(searchResultsArtworkViewModel: SearchArtworkViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchToursViewModel::class)
    abstract fun searchResultsToursViewModel(searchResultsToursViewModel: SearchToursViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchSuggestedViewModel::class)
    abstract fun searchResultsSuggestedViewModel(searchResultsSuggestedViewModel: SearchSuggestedViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val splashActivity: SearchActivity

    @get:ContributesAndroidInjector
    abstract val defaultSearchSuggestionsFragment: DefaultSearchSuggestionsFragment

    @get:ContributesAndroidInjector
    abstract val searchFragment: SearchFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsContainerFragment: SearchResultsContainerFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsSuggestedFragment: SearchSuggestedFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsExhibitionsFragment: SearchExhibitionsFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsArtworkFragment: SearchArtworkFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsToursFragment: SearchToursFragment

    @Module
    companion object {
        @JvmStatic
        @Provides
        @Singleton
        fun provideSearchManager(
                searchService: SearchServiceProvider,
                tourDao: ArticTourDao,
                articObjectDao: ArticObjectDao,
                articDataObjectDao: ArticDataObjectDao,
                articGalleryDao: ArticGalleryDao
        ): SearchResultsManager = SearchResultsManager(searchService, tourDao, articObjectDao, articDataObjectDao, articGalleryDao)

        /**
         * NB: We reuse the [ApiModule]'s Retrofit here.
         *
         * Caveats regarding its base url may apply; consult docs for
         * [retrofit2.http.Url] before adding new API calls to [SearchApi].
         */
        @JvmStatic
        @Provides
        @Singleton
        fun provideSearchService(
                @Named(ApiModule.RETROFIT_BLOB_API) retrofit: Retrofit,
                dataObjectDao: ArticDataObjectDao
        ): SearchServiceProvider = RetrofitSearchServiceProvider(retrofit, dataObjectDao)
    }
}