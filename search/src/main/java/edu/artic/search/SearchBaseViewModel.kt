package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.models.ArticAppData
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

open class SearchBaseViewModel @Inject constructor(
        protected val analyticsTracker: AnalyticsTracker,
        protected val searchResultsManager: SearchResultsManager,
        private val dataObjectDao: ArticDataObjectDao
)
    : NavViewViewModel<SearchBaseViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class TourDetails(val tour: ArticTour) : NavigationEndpoint()
        data class ExhibitionDetails(val exhibition: ArticExhibition) : NavigationEndpoint()
        data class ArtworkDetails(val articObject: ArticSearchArtworkObject) : NavigationEndpoint()
        data class ArtworkOnMap(val articObject: ArticSearchArtworkObject) : NavigationEndpoint()
        data class AmenityOnMap(val type: SuggestedMapAmenities) : NavigationEndpoint()
        object HideKeyboard: NavigationEndpoint()
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
                                NavigationEndpoint.ArtworkDetails(viewModel.artwork)
                        )
                )
            }
            is SearchAmenitiesCellViewModel -> {
                when (viewModel.type) {
                    SuggestedMapAmenities.Dining -> {
                        analyticsTracker.reportEvent(ScreenCategoryName.Map, AnalyticsAction.mapShowDining)
                    }
                    SuggestedMapAmenities.Restrooms -> {
                        analyticsTracker.reportEvent(ScreenCategoryName.Map, AnalyticsAction.mapShowRestrooms)
                    }
                    SuggestedMapAmenities.GiftShop -> {
                        analyticsTracker.reportEvent(ScreenCategoryName.Map, AnalyticsAction.mapShowGiftShops)
                    }
                    SuggestedMapAmenities.MembersLounge -> {
                        analyticsTracker.reportEvent(ScreenCategoryName.Map, AnalyticsAction.mapShowMemberLounge)
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
                    /** Convert the [ArticObject] to [ArticSearchArtworkObject] **/
                    val articSearchArtworkObject = ArticSearchArtworkObject(
                            artworkId = articObject.id.toString(),
                            backingObject = articObject,
                            title = articObject.title,
                            thumbnailUrl = articObject.thumbUrl,
                            imageUrl = articObject.largeImageUrl,
                            artistTitle = articObject.tombstone,
                            artistDisplay = articObject.artistCulturePlaceDelim,
                            location = articObject.location,
                            floor = articObject.floor,
                            gallery = null /* Currently, gallery is not required for search details */
                    )

                    navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ArtworkOnMap(articSearchArtworkObject)))
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
}