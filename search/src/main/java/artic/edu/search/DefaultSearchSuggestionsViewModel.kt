package artic.edu.search

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import edu.artic.db.models.ArticObject
import edu.artic.ui.util.asCDNUri
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class DefaultSearchSuggestionsViewModel @Inject constructor(searchSuggestionsDao: ArticSearchObjectDao,
                                                            objectDao: ArticObjectDao
) : SearchResultsBaseViewModel<DefaultSearchSuggestionsViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class ArticObjectDetails(val articObject: ArticObject) : NavigationEndpoint()
    }

    private val suggestedKeywords: Subject<List<SearchResultTextCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<SearchResultCircularCellViewModel>> = BehaviorSubject.create()

    private fun getAmenitiesViewModels(): List<SearchResultBaseCellViewModel> {
        return listOf(
                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant),
                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge),
                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop),
                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom))
    }

    init {
        searchSuggestionsDao.getDataObject()
                .toObservable()
                .map { suggestedSearchOptions ->
                    suggestedSearchOptions
                            .searchStrings
                            .map { keyword ->
                                SearchResultTextCellViewModel(keyword)
                            }
                }
                .bindTo(suggestedKeywords)
                .disposedBy(disposeBag)

        searchSuggestionsDao.getDataObject()
                .toObservable()
                .map { suggestedSearchOptions -> suggestedSearchOptions.searchObjects }
                .flatMap { idsList ->
                    objectDao.getObjectsByIdList(idsList).toObservable()
                }
                .map { objects ->
                    objects.map { artwork ->
                        SearchResultCircularCellViewModel(artwork)
                    }
                }
                .bindTo(suggestedArtworks)
                .disposedBy(disposeBag)

        Observables.combineLatest(suggestedArtworks, suggestedKeywords)
        { artworks, keywords ->
            mutableListOf<SearchResultBaseCellViewModel>()
                    .apply {
                        add(0, SearchResultTextHeaderViewModel("Suggested")) //TODO: use localizer
                        addAll(keywords)
                        add(SearchResultTextHeaderViewModel("On the Map"))
                        addAll(getAmenitiesViewModels())
                        add(RowPaddingViewModel2())
                        addAll(artworks)
                    }
        }
                .bindTo(cells)
                .disposedBy(disposeBag)

    }

    fun onClickItem(pos: Int, vm: SearchResultBaseCellViewModel) {
        when (vm) {
            is SearchResultCircularCellViewModel -> {
                vm.artWork?.let { articObject ->
                    navigateTo.onNext(
                            Navigate.Forward(
                                    NavigationEndpoint.ArticObjectDetails(articObject)
                            )
                    )

                }
            }
            else -> {

            }
        }
    }
}


/**
 * This class represents different component types used to build the [DefaultSearchSuggestionsFragment] view.
 */
sealed class SearchViewComponent<T>(val value: T) {
    class Header(@StringRes val value: Int)
    class SuggestedKeyword(val value: String)
    class Artwork(val value: ArticObject)
}

/** Base class*/
open class SearchBaseCellViewModel : BaseViewModel()

/**
 * Represents the header for each section (e.g. "On the Map")
 */
class HeaderCellViewModel(item: SearchViewComponent.Header) : SearchBaseCellViewModel() {
    val text: Subject<Int> = BehaviorSubject.createDefault(item.value)
}

/**
 * Adds an empty row in the RecyclerView.
 *
 * Both Map Amenities and Artworks requires span size of 1, and they appear next to each other.
 * In order to display these in different rows, [RowPaddingViewModel] is added in between them to break the row.
 *
 * ViewModel breaks the.
 */
class RowPaddingViewModel : SearchBaseCellViewModel()

/**
 * ViewModel for displaying the amenities icons
 */
class AmenitiesCellViewModel(@DrawableRes val value: Int) : SearchBaseCellViewModel() {

}

/**
 * ViewModel for displaying the suggested keywords and header.
 */
class TextCellViewModel(item: SearchViewComponent.SuggestedKeyword) : SearchBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(item.value)
}

/**
 * ViewModel for displaying the circular artwork image under "On the map" section.
 */
class CircularCellViewModel(val artWork: SearchViewComponent.Artwork?) : SearchBaseCellViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.create()

    init {
        artWork?.value?.thumbnailFullPath?.asCDNUri()?.let { url ->
            imageUrl.onNext(url)
        }

    }

}
