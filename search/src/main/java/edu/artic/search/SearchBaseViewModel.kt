package edu.artic.search

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.mapOptional
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import edu.artic.db.models.*
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

open class SearchBaseViewModel @Inject constructor(
        protected val analyticsTracker: AnalyticsTracker,
        protected val searchResultsManager: SearchResultsManager,
        private val dataObjectDao: ArticDataObjectDao,
        private val galleryDao: ArticGalleryDao
) : NavViewViewModel<SearchBaseViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class TourDetails(val tour: ArticTour) : NavigationEndpoint()
        data class ExhibitionDetails(val exhibition: ArticExhibition) : NavigationEndpoint()
        data class ArtworkDetails(val articObject: ArticSearchArtworkObject, val searchTerm: String) : NavigationEndpoint()
        data class ArtworkOnMap(val articObject: ArticSearchArtworkObject) : NavigationEndpoint()
        data class AmenityOnMap(val type: SuggestedMapAmenities) : NavigationEndpoint()
        object HideKeyboard : NavigationEndpoint()
        data class Web(val url: String) : NavigationEndpoint()

    }

    val cells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()


    open fun onClickItem(pos: Int, viewModel: SearchBaseCellViewModel) {
        val searchText = (searchResultsManager.currentSearchText as BehaviorSubject<String>).value.orEmpty()
        when (viewModel) {
            is SearchTourCellViewModel -> {
                analyticsTracker.reportEvent(
                        EventCategoryName.SearchTour,
                        viewModel.articTour.title,
                        searchText
                )
                navigateTo.onNext(
                        Navigate.Forward(NavigationEndpoint.TourDetails(viewModel.articTour))
                )
            }
            is SearchExhibitionCellViewModel -> {
                analyticsTracker.reportEvent(
                        EventCategoryName.SearchExhibition,
                        viewModel.articExhibition.title,
                        searchText
                )
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.ExhibitionDetails(viewModel.articExhibition)
                        )
                )
            }
            is SearchArtworkCellViewModel -> {
                analyticsTracker.reportEvent(
                        EventCategoryName.SearchArtwork,
                        viewModel.artwork.title,
                        searchText
                )
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.ArtworkDetails(viewModel.artwork, searchText)
                        )
                )
            }
            is SearchAmenitiesCellViewModel -> {
                when (viewModel.type) {
                    SuggestedMapAmenities.Dining -> {
                        analyticsTracker.reportEvent(EventCategoryName.Map, AnalyticsAction.mapShowDining)
                    }
                    SuggestedMapAmenities.Restrooms -> {
                        analyticsTracker.reportEvent(EventCategoryName.Map, AnalyticsAction.mapShowRestrooms)
                    }
                    SuggestedMapAmenities.GiftShop -> {
                        analyticsTracker.reportEvent(EventCategoryName.Map, AnalyticsAction.mapShowGiftShops)
                    }
                    SuggestedMapAmenities.MembersLounge -> {
                        analyticsTracker.reportEvent(EventCategoryName.Map, AnalyticsAction.mapShowMemberLounge)
                    }
                }
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.AmenityOnMap(viewModel.type)
                        )
                )
            }
            is SearchEmptyCellViewModel -> {
                dataObjectDao.getDataObject()
                        .map { it.websiteUrlAndroid }
                        .take(1)
                        .map { url -> Navigate.Forward(NavigationEndpoint.Web(url)) }
                        .bindTo(navigateTo)
            }
            is SearchCircularCellViewModel -> {
                viewModel.artwork?.let { articObject ->

                    Observable.just(Optional(articObject.galleryLocation))
                            .flatMap {
                                val galleryLocation = it.value
                                if (galleryLocation != null) {
                                    galleryDao.getGalleryByTitle(galleryLocation)
                                            .toObservable()
                                            .mapOptional()
                                } else {
                                    /**
                                     * If the ArticObject.galleryLocation is null, return empty
                                     * optional to down stream.
                                     */
                                    Observable.just(Optional<ArticGallery>(null))
                                }
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeBy(
                                    onNext = { gallery ->
                                        val articSearchArtworkObject: ArticSearchArtworkObject = if (gallery.value != null) {
                                            articObject.asArticSearchArtworkObject(gallery.value)
                                        } else {
                                            articObject.asArticSearchArtworkObject()
                                        }

                                        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ArtworkOnMap(articSearchArtworkObject)))
                                    },
                                    onError = {
                                        val searchedObject = articObject.asArticSearchArtworkObject()
                                        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ArtworkOnMap(searchedObject)))
                                    }
                            )


                }
            }
            is SearchTextCellViewModel -> {
                if (this is DefaultSearchSuggestionsViewModel) {
                    analyticsTracker.reportEvent(
                            EventCategoryName.Search,
                            AnalyticsAction.searchPromoted,
                            viewModel.textString
                    )
                } else {
                    analyticsTracker.reportEvent(
                            EventCategoryName.Search,
                            AnalyticsAction.searchAutocomplete,
                            viewModel.textString
                    )
                }
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.HideKeyboard
                        )
                )
                searchResultsManager.search(viewModel.textString)
            }
            is SearchHeaderCellViewModel -> {
                //TODO: handle this case
            }
        }
    }

    open fun onClickSeeAll(header: Header) {

    }

    /**
     * Returns suggested artworks preserving the CMS order.
     */
    protected fun getSuggestedArtworks(searchSuggestionsDao: ArticSearchObjectDao,
                                       objectDao: ArticObjectDao
    ): Observable<List<ArticObject>> {
        return searchSuggestionsDao.getDataObject()
                .toObservable()
                .map { suggestedSearchOptions -> suggestedSearchOptions.searchObjects }
                .flatMap { idsList ->
                    /**
                     * Database does not preserve the order of ids.
                     */
                    objectDao.getObjectsByIdList(idsList)
                            .toObservable()
                            .map { unSortedObjects ->
                                /**
                                 * Sorting the objects based on idsList
                                 */
                                val sortedObjects: List<ArticObject> = idsList.mapNotNull { id ->
                                    unSortedObjects.find { it.nid == id }
                                }
                                sortedObjects
                            }
                }
    }
}