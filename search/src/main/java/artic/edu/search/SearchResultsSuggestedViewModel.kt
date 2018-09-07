package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchResultsSuggestedViewModel @Inject constructor(private val manager: SearchResultsManager,
                                                          private val searchSuggestionsDao: ArticSearchObjectDao,
                                                          private val objectDao: ArticObjectDao)
    : BaseViewModel() {

    val cells: Subject<List<SearchResultBaseCellViewModel>> = BehaviorSubject.create()
    private val dynamicCells: Subject<List<SearchResultBaseCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<SearchResultCircularCellViewModel>> = BehaviorSubject.create()

    init {
        setupOnMapSuggestionsBind()

        setupResultsBind()

        Observables
                .combineLatest(
                        dynamicCells,
                        Observable.just(listOf(
                                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant),
                                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge),
                                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop),
                                SearchResultAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom),
                                SearchResultAmenitiesCellViewModel(0))
                        ),
                        suggestedArtworks)
                { dynamicCells, amenities, suggestedArtworks ->
                    return@combineLatest mutableListOf<SearchResultBaseCellViewModel>().apply {
                        addAll(dynamicCells)
                        add(SearchResultOnMapHeaderCellViewModel("On the Map"))
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
                        SearchResultCircularCellViewModel(SearchViewComponent.Artwork(it))
                    }
                }
                .bindTo(suggestedArtworks)
                .disposedBy(disposeBag)
    }

    private fun setupResultsBind() {
        manager.currentSearchResults
                .map { result ->
                    val cellList = mutableListOf<SearchResultBaseCellViewModel>()

                    cellList.addAll(result.suggestions.map { SearchResultTextCellViewModel(it) })
                    if (result.artworks.isNotEmpty()) {
                        //TODO: add header
                        cellList.addAll(
                                result.artworks
                                        .take(3)
                                        .map { SearchResultArtworkCellViewModel() }
                        )
                    }
                    if (result.exhibitions.isNotEmpty()) {
                        cellList.add(SearchResultHeaderCellViewModel("Exhibitions"))
                        cellList.addAll(
                                result.exhibitions
                                        .take(3)
                                        .map { SearchResultExhibitionCellViewModel(it) }
                        )
                    }

                    if (result.tours.isNotEmpty()) {
                        //TODO: add header
                        cellList.addAll(
                                result.exhibitions
                                        .take(3)
                                        .map { SearchResultExhibitionCellViewModel(it) }
                        )
                    }

                    return@map cellList

                }.bindTo(dynamicCells)
                .disposedBy(disposeBag)
    }

}