package artic.edu.search

import android.arch.lifecycle.ViewModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap
import edu.artic.db.ApiModule
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.viewmodel.ViewModelKey
import okhttp3.OkHttpClient
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

    @get:ContributesAndroidInjector
    abstract val splashActivity: SearchActivity

    @get:ContributesAndroidInjector
    abstract val defaultSearchSuggestionsFragment: DefaultSearchSuggestionsFragment

    @get:ContributesAndroidInjector
    abstract val searchFragment: SearchFragment

    @get:ContributesAndroidInjector
    abstract val searchAudioDetailFragment: SearchAudioDetailFragment


    @Module
    companion object {

        @JvmStatic
        @Provides
        @Singleton
        @Named(SEARCH_CLIENT_API)
        fun provideClient(): OkHttpClient {
            return OkHttpClient.Builder().build()
        }

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


        const val SEARCH_CLIENT_API = "SEARCH_CLIENT_API"
    }
}