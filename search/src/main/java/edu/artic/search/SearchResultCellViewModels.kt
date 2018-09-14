package edu.artic.search

import android.support.annotation.DrawableRes
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour
import edu.artic.ui.util.asCDNUri
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

/**
 * Base class for all of the [BaseViewModel]s in this file.
 *
 * Each of these corresponds more-or-less directly to a single
 * [android.support.v7.widget.RecyclerView.ViewHolder] in the list
 * of search results.
 */
open class SearchBaseCellViewModel(var hasDivider: Boolean = false) : BaseViewModel()

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

class SearchHeaderCellViewModel(header: Header) : SearchBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(header.title)
}

class SearchTextHeaderViewModel(text: String) : SearchBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

open class SearchBaseListItemViewModel(isHeadphonesVisible: Boolean = false)
    : SearchBaseCellViewModel(hasDivider = true) {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val itemTitle: Subject<String> = BehaviorSubject.create()
    val itemSubTitle: Subject<String> = BehaviorSubject.create()
    val isHeadphonesVisible: Subject<Boolean> = BehaviorSubject.createDefault(isHeadphonesVisible)
}

class SearchArtworkCellViewModel(val articObject: ArticSearchArtworkObject)
    : SearchBaseListItemViewModel(isHeadphonesVisible = articObject.audioObject?.audioCommentary?.isNotEmpty() ?: false) {

    init {
        imageUrl.onNext(articObject.thumbUrl.orEmpty())
        itemTitle.onNext(articObject.title)
        itemSubTitle.onNext(articObject.artistDisplay.orEmpty())
    }
}

class SearchExhibitionCellViewModel (val articExhibition: ArticExhibition) : SearchBaseListItemViewModel() {
    init {
        imageUrl.onNext(articExhibition.legacy_image_mobile_url.orEmpty())
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
class SearchCircularCellViewModel(val artWork: ArticObject?) : SearchBaseCellViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.createDefault(
            artWork?.thumbnailFullPath?.asCDNUri().orEmpty()
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


sealed class Header(val title : String) {
    class Artworks(title: String = "Artwork") : Header(title)
    class Tours(title: String = "Tours") : Header(title)
    class Exhibitions(title: String = "Exhibitions") : Header(title)
}