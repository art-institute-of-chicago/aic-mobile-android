package artic.edu.search

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
}