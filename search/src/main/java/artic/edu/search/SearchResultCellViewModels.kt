package artic.edu.search

import android.support.annotation.DrawableRes
import edu.artic.db.models.ArticExhibition
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

class SearchResultOnMapHeaderCellViewModel(text: String) : SearchResultBaseCellViewModel() {
    val text: Subject<String> = BehaviorSubject.createDefault(text)
}

open class SearchResultBaseListItemViewModel(isHeadphonesVisisble: Boolean = false)
    : SearchResultBaseCellViewModel(hasDivider = true) {
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val itemTitle: Subject<String> = BehaviorSubject.create()
    val itemSubTitle: Subject<String> = BehaviorSubject.create()
    val isHeadphonesVisible: Subject<Boolean> = BehaviorSubject.createDefault(isHeadphonesVisisble)
}

class SearchResultArtworkCellViewModel
    : SearchResultBaseListItemViewModel(isHeadphonesVisisble = true)

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
class SearchResultCircularCellViewModel(val artWork: SearchViewComponent.Artwork?) : SearchResultBaseCellViewModel() {

    val imageUrl: Subject<String> = BehaviorSubject.create()

    init {
        artWork?.value?.thumbnailFullPath?.asCDNUri()?.let { url ->
            imageUrl.onNext(url)
        }

    }

}

/**
 * ViewModel for displaying the amenities icons
 */
class SearchResultAmenitiesCellViewModel(@DrawableRes val value: Int) : SearchResultBaseCellViewModel()