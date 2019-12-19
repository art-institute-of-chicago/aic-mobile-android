package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.ArticTour
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchSuggestedViewModel @Inject constructor(private val manager: SearchResultsManager,
                                                   private val searchSuggestionsDao: ArticSearchObjectDao,
                                                   private val objectDao: ArticObjectDao,
                                                   analyticsTracker: AnalyticsTracker,
                                                   dataObjectDao: ArticDataObjectDao,
                                                   galleryDao: ArticGalleryDao)
    : SearchBaseViewModel(analyticsTracker, manager, dataObjectDao, galleryDao) {

    private val dynamicCells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<SearchCircularCellViewModel>> = BehaviorSubject.create()
    var parentViewModel : SearchResultsContainerViewModel? = null
    var lastSearchTerm: String = ""

    init {
        setupOnMapSuggestionsBind()

        setupResultsBind()

        Observables
                .combineLatest(
                        dynamicCells,
                        Observable.just(listOf(
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant, SuggestedMapAmenities.Dining, 0),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge, SuggestedMapAmenities.MembersLounge, 1),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop, SuggestedMapAmenities.GiftShop, 2),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom, SuggestedMapAmenities.Restrooms, 3),
                                /**
                                 * TODO:: Refactor it, used something other than SearchAmenitiesCellViewModel (maybe PaddingAmenitiesCellViewModel)
                                 * **/
                                SearchAmenitiesCellViewModel(0, SuggestedMapAmenities.Restrooms, 4))
                        ),
                        suggestedArtworks)
                { dynamicCells, amenities, suggestedArtworks ->
                    return@combineLatest mutableListOf<SearchBaseCellViewModel>().apply {
                        addAll(dynamicCells)
                        add(SearchTextHeaderViewModel(R.string.on_the_map))
                        addAll(amenities)
                        addAll(suggestedArtworks)
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)

    }

    private fun setupOnMapSuggestionsBind() {
        getSuggestedArtworks(searchSuggestionsDao, objectDao)
                .map { objects ->
                    objects.mapIndexed { index, item ->
                        SearchCircularCellViewModel(item, index)
                    }
                }
                .bindTo(suggestedArtworks)
                .disposedBy(disposeBag)
    }

    private fun setupResultsBind() {
        manager.currentSearchResults
                .filter { !it.searchTerm.isBlank() }
                .map { result ->
                    mutableListOf<SearchBaseCellViewModel>()
                            .apply {
                                addAll(filterSearchSuggestions(result.searchTerm, result.suggestions))

                                addAll(filterArtworkForViewModel(result.artworks))

                                addAll(filterToursForViewModel(result.tours))

                                // Should the new cell list be empty at that point, we notify the
                                // user that it's empty
                                if (isEmpty()) {
                                    add(SearchEmptyCellViewModel())
                                    analyticsTracker.reportEvent(
                                            EventCategoryName.Search,
                                            AnalyticsAction.searchNoResults,
                                            result.searchTerm
                                    )
                                } else {
                                    if (lastSearchTerm != result.searchTerm && result.searchTerm.trim().length > 3) {
                                        analyticsTracker.reportEvent(
                                                EventCategoryName.Search,
                                                AnalyticsAction.searchLoaded,
                                                result.searchTerm
                                        )
                                        lastSearchTerm = result.searchTerm.trim()
                                    }
                                }

                                addAll(filterExhibitionsForViewModel(result.exhibitions))
                            }


                }
                .bindTo(dynamicCells)
                .disposedBy(disposeBag)
    }

    private fun filterArtworkForViewModel(artworkList: List<ArticSearchArtworkObject>): List<SearchBaseCellViewModel> {
        val cellList = mutableListOf<SearchBaseCellViewModel>()
        if (artworkList.isNotEmpty()) {
            cellList.add(SearchHeaderCellViewModel(Header.Artworks(), this)) //TODO: use localizer
            cellList.addAll(
                    artworkList
                            .take(3)
                            .map { SearchArtworkCellViewModel(it) }
            )
        }
        return cellList
    }


    private fun filterExhibitionsForViewModel(list: List<ArticExhibition>): List<SearchBaseCellViewModel> {
        /**
         * Filters the tours, returning top 3 tours or an empty list if no tours are available for
         * search terms
         */
        val cellList = mutableListOf<SearchBaseCellViewModel>()
        if (list.isNotEmpty()) {
            cellList.add(SearchHeaderCellViewModel(Header.Exhibitions(), this)) // TODO: use localizer
            cellList.addAll(
                    list
                            .take(3)
                            .map { SearchExhibitionCellViewModel(it) }
            )
        }
        return cellList
    }

    /**
     * Filters the tours, returning top 3 tours or an empty list if no tours are available for
     * search terms
     */
    private fun filterToursForViewModel(list: List<ArticTour>): List<SearchBaseCellViewModel> {
        val cellList = mutableListOf<SearchBaseCellViewModel>()
        if (list.isNotEmpty()) {
            cellList.add(SearchHeaderCellViewModel(Header.Tours(),this)) // TODO: use localizer
            cellList.addAll(
                    list
                            .take(3)
                            .map { SearchTourCellViewModel(it) }
            )
        }
        return cellList
    }

    /**
     * Filters search suggestions, returning top 3 or an empty list
     */
    private fun filterSearchSuggestions(searchTerm: String, list: List<String>): List<SearchBaseCellViewModel> {
        val l = list.take(3).map { SearchTextCellViewModel(it, searchTerm) }
        if (l.isNotEmpty()) {
            l.last().hasDivider = true
        }
        return l
    }

    override fun onClickSeeAll(header: Header) {
        parentViewModel?.onClickSeeAll(header)
    }

}