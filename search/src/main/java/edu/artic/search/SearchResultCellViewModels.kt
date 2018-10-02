package edu.artic.search

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.CellViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Base class for all of the [CellViewModel]s in this file.
 *
 * Each of these corresponds more-or-less directly to a single
 * [android.support.v7.widget.RecyclerView.ViewHolder] in the list
 * of search results.
 */
open class SearchBaseCellViewModel(var hasDivider: Boolean = false) : CellViewModel()

/**
 * Dedicated subclass for plain-text content, like autocomplete suggestions.
 *
 * @param textString the text to show on screen
 * @param highlightedText (optional) a substring that, if present in [textString], should
 * be highlighted with a color or made bold or something along those lines
 */
class SearchTextCellViewModel(
        val textString: String,
        highlightedText: String = ""
) : SearchBaseCellViewModel() {
    val text: Subject<Pair<String,String>> = BehaviorSubject.createDefault(Pair(textString, highlightedText))

    // Warning: overriding ::equals or ::hashCode may prevent the screen from updating correctly.

}

class SearchEmptyCellViewModel : SearchBaseCellViewModel()

class SearchHeaderCellViewModel(private val header: Header, private val parentViewModel: SearchBaseViewModel) : SearchBaseCellViewModel() {
    val text: Subject<Int> = BehaviorSubject.createDefault(header.title)

    fun onClickSeeAll() {
        parentViewModel.onClickSeeAll(header)
    }
}

class SearchTextHeaderViewModel(@StringRes text: Int) : SearchBaseCellViewModel() {
    val text: Subject<Int> = BehaviorSubject.createDefault(text)
}

open class SearchBaseListItemViewModel(isHeadphonesVisible: Boolean = false)
    : SearchBaseCellViewModel(hasDivider = true) {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val itemTitle: Subject<String> = BehaviorSubject.create()
    val itemSubTitle: Subject<String> = BehaviorSubject.create()
    val isHeadphonesVisible: Subject<Boolean> = BehaviorSubject.createDefault(isHeadphonesVisible)
}

class SearchArtworkCellViewModel(val artwork: ArticSearchArtworkObject)
    : SearchBaseListItemViewModel(isHeadphonesVisible = artwork.backingObject?.audioCommentary?.isNotEmpty() ?: false) {

    init {
        imageUrl.onNext(artwork.thumbUrl.orEmpty())
        itemTitle.onNext(artwork.title)
        itemSubTitle.onNext(artwork.artistTitle.orEmpty())
    }
}

class SearchExhibitionCellViewModel (val articExhibition: ArticExhibition) : SearchBaseListItemViewModel() {
    init {
        imageUrl.onNext(articExhibition.legacyImageUrl.orEmpty())
        itemTitle.onNext(articExhibition.title)
    }
}

class SearchTourCellViewModel(val articTour: ArticTour) : SearchBaseListItemViewModel() {
    init {
        imageUrl.onNext(articTour.thumbUrl.orEmpty())
        itemTitle.onNext(articTour.title)
    }
}

/**
 * ViewModel for displaying the circular artwork image under "On the map" section.
 */
class SearchCircularCellViewModel(val artwork: ArticObject?) : SearchBaseCellViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.createDefault(
            artwork?.thumbUrl.orEmpty()
    )
}

/**
 * ViewModel for displaying the amenities icons
 */
class SearchAmenitiesCellViewModel(@DrawableRes val value: Int, val type: SuggestedMapAmenities) : SearchBaseCellViewModel()



/**
 * Adds an empty row in the RecyclerView.
 *
 * Both Map Amenities and Artworks requires span size of 1, and they appear next to each other.
 * In order to display these in different rows, [RowPaddingViewModel] is added in between them to break the row.
 *
 */
class RowPaddingViewModel : SearchBaseCellViewModel()


sealed class Header(@StringRes val title : Int) {
    class Artworks(title: Int = R.string.artworks) : Header(title)
    class Tours(title: Int = R.string.tours) : Header(title)
    class Exhibitions(title: Int = R.string.exhibitions) : Header(title)
}