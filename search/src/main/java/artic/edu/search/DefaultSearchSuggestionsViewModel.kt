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
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class DefaultSearchSuggestionsViewModel @Inject constructor(searchSuggestionsDao: ArticSearchObjectDao,
                                                            objectDao: ArticObjectDao
) : BaseViewModel() {

    private val suggestedKeywords: Subject<List<TextCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<CircularCellViewModel>> = BehaviorSubject.create()
    val cells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()

    private fun getAmenitiesViewModels(): List<SearchBaseCellViewModel> {
        return listOf(
                AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant),
                AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge),
                AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop),
                AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom))
    }

    init {
        searchSuggestionsDao.getDataObject()
                .toObservable()
                .map { suggestedSearchOptions ->
                    val list = mutableListOf<TextCellViewModel>()
                    suggestedSearchOptions.searchStrings.forEach { keyword ->
                        list.add(TextCellViewModel(SearchViewComponent.SuggestedKeyword(keyword)))
                    }
                    return@map list
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
                    val list = mutableListOf<CircularCellViewModel>()
                    objects.forEach { artwork ->
                        list.add(CircularCellViewModel(SearchViewComponent.Artwork(artwork)))
                    }
                    return@map list
                }
                .bindTo(suggestedArtworks)
                .disposedBy(disposeBag)

        Observables.combineLatest(suggestedArtworks, suggestedKeywords)
        { artworks, keywords ->
            mutableListOf<SearchBaseCellViewModel>()
                    .apply {
                        add(0, HeaderCellViewModel(SearchViewComponent.Header(R.string.suggested)))
                        addAll(keywords)
                        add(HeaderCellViewModel(SearchViewComponent.Header(R.string.on_the_map)))
                        addAll(getAmenitiesViewModels())
                        add(DividerViewModel())
                        addAll(artworks)
                    }
        }
                .bindTo(cells)
                .disposedBy(disposeBag)

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
 * In order to display these in different rows, [DividerViewModel] is added in between them to break the row.
 *
 * ViewModel breaks the.
 */
class DividerViewModel : SearchBaseCellViewModel()

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
class CircularCellViewModel(artWork: SearchViewComponent.Artwork?) : SearchBaseCellViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.create()

    init {
        artWork?.value?.thumbnailFullPath?.asCDNUri()?.let { url ->
            imageUrl.onNext(url)
        }

    }

}
