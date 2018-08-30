package artic.edu.search

import android.support.annotation.DrawableRes
import edu.artic.db.models.ArticObject
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class DefaultSearchSuggestionsViewModel @Inject constructor() : BaseViewModel() {


    val cells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()
    private var viewElements: List<SearchBaseCellViewModel> = mutableListOf<SearchBaseCellViewModel>().apply {
        /**
         * TODO:: replace the mock data
         */

        add(HeaderCellViewModel(SearchViewComponent.Header("Suggested")))
        add(TextCellViewModel(SearchViewComponent.SuggestedKeyword("Bedroom")))
        add(TextCellViewModel(SearchViewComponent.SuggestedKeyword("Mandala")))
        add(TextCellViewModel(SearchViewComponent.SuggestedKeyword("York")))
        add(HeaderCellViewModel(SearchViewComponent.Header("On The Map")))

        add(AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant))
        add(AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge))
        add(AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop))
        add(AmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom))

        add(DividerViewModel())

        add(CircularCellViewModel(null))
        add(CircularCellViewModel(null))
        add(CircularCellViewModel(null))
        add(CircularCellViewModel(null))
        add(CircularCellViewModel(null))
        add(CircularCellViewModel(null))
    }

    init {
        cells.onNext(viewElements)
    }

    /**
     * Span size for the artworks should be 5 (
     */
    fun getSpanCount(position: Int): Int {
        val cell = viewElements[position]
        return if (cell is CircularCellViewModel || cell is AmenitiesCellViewModel) {
            1
        } else {
            5
        }
    }
}


/**
 * This class represents different component types used to build the [DefaultSearchSuggestionsFragment] view.
 */
sealed class SearchViewComponent<T>(val value: T) {
    class Header(val value: String)
    class SuggestedKeyword(val value: String)
    class Artwork(val value: ArticObject)
}

/** Base class*/
open class SearchBaseCellViewModel : BaseViewModel()

/**
 * Represents the header for each section (e.g. "On the Map")
 */
class HeaderCellViewModel(item: SearchViewComponent.Header) : SearchBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(item.value)
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
    val resource: Subject<Int> = BehaviorSubject.createDefault(value)
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
class CircularCellViewModel(val artWork: SearchViewComponent<*>?) : SearchBaseCellViewModel() {
    /**
     * TODO:: remove this mock image url
     */
    val imageUrl: Subject<String> = BehaviorSubject.createDefault("https://upload.wikimedia.org/wikipedia/commons/thumb/e/ec/Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg/300px-Mona_Lisa%2C_by_Leonardo_da_Vinci%2C_from_C2RMF_retouched.jpg")

}
