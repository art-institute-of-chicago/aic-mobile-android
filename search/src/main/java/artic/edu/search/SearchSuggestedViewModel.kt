package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchSuggestedViewModel @Inject constructor(private val manager: SearchResultsManager,
                                                   private val searchSuggestionsDao: ArticSearchObjectDao,
                                                   private val objectDao: ArticObjectDao)
    : SearchBaseViewModel<SearchSuggestedViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    private val dynamicCells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<SearchCircularCellViewModel>> = BehaviorSubject.create()

    init {
        setupOnMapSuggestionsBind()

        setupResultsBind()

        Observables
                .combineLatest(
                        dynamicCells,
                        Observable.just(listOf(
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop),
                                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom),
                                SearchAmenitiesCellViewModel(0))
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
                    val cellList = mutableListOf<SearchBaseCellViewModel>()

                    cellList.addAll(result.suggestions.map { SearchTextCellViewModel(it) })
                    if (result.artworks.isNotEmpty()) {
                        cellList.add(SearchHeaderCellViewModel("Artwork"))
                        cellList.addAll(
                                result.artworks
                                        .take(3)
                                        .map { SearchArtworkCellViewModel(it) }
                        )
                    }
                    if (result.exhibitions.isNotEmpty()) {
                        cellList.add(SearchHeaderCellViewModel("Exhibitions"))
                        cellList.addAll(
                                result.exhibitions
                                        .take(3)
                                        .map { SearchExhibitionCellViewModel(it) }
                        )
                    }

                    if (result.tours.isNotEmpty()) {
                        cellList.add(SearchHeaderCellViewModel("Tours"))
                        cellList.addAll(
                                result.tours
                                        .take(3)
                                        .map { SearchTourCellViewModel(it) }
                        )
                    }

                    return@map cellList

                }.bindTo(dynamicCells)
                .disposedBy(disposeBag)
    }

}