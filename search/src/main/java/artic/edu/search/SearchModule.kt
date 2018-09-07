package artic.edu.search

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.db.ApiModule
import edu.artic.db.daos.ArticDataObjectDao
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
    @ViewModelKey(SearchAudioDetailViewModel::class)
    abstract fun searchAudioDetailViewModel(searchAudioDetailViewModel: SearchAudioDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchResultsContainerViewModel::class)
    abstract fun searchResultsViewModel(searchResultsContainerViewModel: SearchResultsContainerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchResultsExhibitionsViewModel::class)
    abstract fun searchResultsExhibitionsViewModel(searchResultsExhibitionsViewModel: SearchResultsExhibitionsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchResultsArtworkViewModel::class)
    abstract fun searchResultsArtworkViewModel(searchResultsArtworkViewModel: SearchResultsArtworkViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchResultsToursViewModel::class)
    abstract fun searchResultsToursViewModel(searchResultsToursViewModel: SearchResultsToursViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchResultsSuggestedViewModel::class)
    abstract fun searchResultsSuggestedViewModel(searchResultsSuggestedViewModel: SearchResultsSuggestedViewModel): ViewModel

    @get:ContributesAndroidInjector
    abstract val splashActivity: SearchActivity

    @get:ContributesAndroidInjector
    abstract val defaultSearchSuggestionsFragment: DefaultSearchSuggestionsFragment

    @get:ContributesAndroidInjector
    abstract val searchFragment: SearchFragment

    @get:ContributesAndroidInjector
    abstract val searchAudioDetailFragment: SearchAudioDetailFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsContainerFragment: SearchResultsContainerFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsSuggestedFragment: SearchResultsSuggestedFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsExhibitionsFragment: SearchResultsExhibitionsFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsArtworkFragment: SearchResultsArtworkFragment

    @get:ContributesAndroidInjector
    abstract val searchResultsToursFragment: SearchResultsToursFragment

    @Module
    companion object {
        @JvmStatic
        @Provides
        @Singleton
        fun provideSearchManager(
                searchService: SearchServiceProvider,
                tourDao: ArticTourDao,
                articObjectDao: ArticObjectDao
        ): SearchResultsManager = SearchResultsManager(searchService, tourDao, articObjectDao)

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