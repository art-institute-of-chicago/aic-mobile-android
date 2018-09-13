package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import edu.artic.db.debug
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
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
                                                   analyticsTracker: AnalyticsTracker)
    : SearchBaseViewModel(analyticsTracker, manager) {

    private val dynamicCells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<SearchCircularCellViewModel>> = BehaviorSubject.create()

    init {
        setupOnMapSuggestionsBind()

        setupResultsBind()

        Observables
                .combineLatest(
                        dynamicCells,
                        Observable.just(listOf(
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant, SuggestedMapAmenities.Dining),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge, SuggestedMapAmenities.MembersLounge),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop, SuggestedMapAmenities.GiftShop),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom, SuggestedMapAmenities.Restrooms),
                                /**
                                 * TODO:: Refactor it, used something other than SearchAmenitiesCellViewModel (maybe PaddingAmenitiesCellViewModel)
                                 * **/
                                SearchAmenitiesCellViewModel(0,SuggestedMapAmenities.Restrooms))
                        ),
                        suggestedArtworks)
                { dynamicCells, amenities, suggestedArtworks ->
                    return@combineLatest mutableListOf<SearchBaseCellViewModel>().apply {
                        addAll(dynamicCells)
                        add(SearchTextHeaderViewModel("On the Map"))
                        addAll(amenities)
                        addAll(suggestedArtworks)
                    }
                }
                .bindTo(cells)
                .disposedBy(disposeBag)

    }

    private fun setupOnMapSuggestionsBind() {
        searchSuggestionsDao.getDataObject()
                .toObservable()
                .map { suggestedSearchOptions -> suggestedSearchOptions.searchObjects }
                .flatMap { idsList ->
                    objectDao.getObjectsByIdList(idsList).toObservable()
                }
                .map { objects ->
                    objects.map {
                        SearchCircularCellViewModel(it)
                    }
                }
                .bindTo(suggestedArtworks)
                .disposedBy(disposeBag)
    }

    private fun setupResultsBind() {
        manager.currentSearchResults
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
                                            ScreenCategoryName.Search,
                                            AnalyticsAction.searchNoResults,
                                            result.searchTerm
                                    )
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
            cellList.add(SearchHeaderCellViewModel(Header.Artworks())) //TODO: use localizer
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
            cellList.add(SearchHeaderCellViewModel(Header.Exhibitions())) // TODO: use localizer
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
            cellList.add(SearchHeaderCellViewModel(Header.Tours())) // TODO: use localizer
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
}