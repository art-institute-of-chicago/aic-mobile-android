package artic.edu.search

import android.support.annotation.DrawableRes
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.ui.util.asCDNUri
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

open class SearchResultBaseCellViewModel(var hasDivider: Boolean = false) : BaseViewModel()

class SearchResultTextCellViewModel(text: String) : SearchResultBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

class SearchResultEmptyCellViewModel : SearchResultBaseCellViewModel()

class SearchResultHeaderCellViewModel(text: String) : SearchResultBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

class SearchResultTextHeaderViewModel(text: String) : SearchResultBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

open class SearchResultBaseListItemViewModel(isHeadphonesVisisble: Boolean = false)
    : SearchResultBaseCellViewModel(hasDivider = true) {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val itemTitle: Subject<String> = BehaviorSubject.create()
    val itemSubTitle: Subject<String> = BehaviorSubject.create()
    val isHeadphonesVisible: Subject<Boolean> = BehaviorSubject.createDefault(isHeadphonesVisisble)
}

class SearchResultArtworkCellViewModel(val articObject: ArticObject)
    : SearchResultBaseListItemViewModel(isHeadphonesVisisble = true) {

    init {
        imageUrl.onNext(articObject.thumbUrl.orEmpty())
        itemTitle.onNext(articObject.title)
        itemSubTitle.onNext(articObject.galleryLocation.orEmpty())
    }
}

class SearchResultExhibitionCellViewModel (val articExhibition: ArticExhibition) : SearchResultBaseListItemViewModel() {
    init {
        imageUrl.onNext(articExhibition.legacy_image_mobile_url.orEmpty())
        itemTitle.onNext(articExhibition.title)
    }
}

class SearchResultTourCellViewModel(val articTour: ArticTour) : SearchResultBaseListItemViewModel() {
    init {
        imageUrl.onNext(articTour.thumbUrl.orEmpty())
        itemTitle.onNext(articTour.title)
    }
}

/**
 * ViewModel for displaying the circular artwork image under "On the map" section.
 */
class SearchResultCircularCellViewModel(val artWork: ArticObject?) : SearchResultBaseCellViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.createDefault(
            artWork?.thumbnailFullPath?.asCDNUri().orEmpty()
    )
}

/**
 * ViewModel for displaying the amenities icons
 */
class SearchResultAmenitiesCellViewModel(@DrawableRes val value: Int) : SearchResultBaseCellViewModel()



/**
 * Adds an empty row in the RecyclerView.
 *
 * Both Map Amenities and Artworks requires span size of 1, and they appear next to each other.
 * In order to display these in different rows, [RowPaddingViewModel] is added in between them to break the row.
 *
 * ViewModel breaks the.
 */
class RowPaddingViewModel2 : SearchResultBaseCellViewModel()