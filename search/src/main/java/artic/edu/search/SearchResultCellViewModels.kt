package artic.edu.search

import android.support.annotation.DrawableRes
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.ui.util.asCDNUri
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

open class SearchBaseCellViewModel(var hasDivider: Boolean = false) : BaseViewModel()

class SearchTextCellViewModel(val textString: String, highlightedText: String = "") : SearchBaseCellViewModel() {
    val text: Subject<Pair<String,String>> = BehaviorSubject.createDefault(Pair(textString, highlightedText))
    override fun equals(other: Any?): Boolean {
        return if(other is SearchTextCellViewModel) {
            other.textString == textString
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return textString.hashCode()
    }
}

class SearchEmptyCellViewModel : SearchBaseCellViewModel()

class SearchHeaderCellViewModel(header: Header) : SearchBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(header.title)
}

class SearchTextHeaderViewModel(text: String) : SearchBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

open class SearchBaseListItemViewModel(isHeadphonesVisisble: Boolean = false)
    : SearchBaseCellViewModel(hasDivider = true) {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val itemTitle: Subject<String> = BehaviorSubject.create()
    val itemSubTitle: Subject<String> = BehaviorSubject.create()
    val isHeadphonesVisible: Subject<Boolean> = BehaviorSubject.createDefault(isHeadphonesVisisble)
}

class SearchArtworkCellViewModel(val articObject: ArticObject)
    : SearchBaseListItemViewModel(isHeadphonesVisisble = true) {

    init {
        imageUrl.onNext(articObject.thumbUrl.orEmpty())
        itemTitle.onNext(articObject.title)
        itemSubTitle.onNext(articObject.galleryLocation.orEmpty())
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