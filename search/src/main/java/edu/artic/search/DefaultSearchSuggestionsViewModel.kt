package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticSearchObjectDao
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class DefaultSearchSuggestionsViewModel @Inject constructor(searchSuggestionsDao: ArticSearchObjectDao,
                                                            objectDao: ArticObjectDao,
                                                            analyticsTracker: AnalyticsTracker,
                                                            searchManager: SearchResultsManager,
                                                            dataObjectDao: ArticDataObjectDao,
                                                            galleryDao: ArticGalleryDao
) : SearchBaseViewModel(analyticsTracker, searchManager, dataObjectDao, galleryDao) {

    private val suggestedKeywords: Subject<List<SearchTextCellViewModel>> = BehaviorSubject.create()
    private val suggestedArtworks: Subject<List<SearchCircularCellViewModel>> = BehaviorSubject.create()

    private fun getAmenitiesViewModels(): List<SearchBaseCellViewModel> {
        return listOf(
                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restaurant, SuggestedMapAmenities.Dining, 0),
                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_lounge, SuggestedMapAmenities.MembersLounge, 1),
                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_shop, SuggestedMapAmenities.GiftShop, 2),
                SearchAmenitiesCellViewModel(R.drawable.ic_icon_amenity_map_restroom, SuggestedMapAmenities.Restrooms, 3))
    }

    init {
        searchSuggestionsDao.getDataObject()
                .toObservable()
                .map { suggestedSearchOptions ->
                    suggestedSearchOptions
                            .searchStrings
                            .map { keyword ->
                                SearchTextCellViewModel(keyword)
                            }
                }
                .bindTo(suggestedKeywords)
                .disposedBy(disposeBag)

        getSuggestedArtworks(searchSuggestionsDao, objectDao)
                .map { objects ->
                    objects.mapIndexed { index, articObject ->
                        SearchCircularCellViewModel(articObject, index)
                    }
                }
                .bindTo(suggestedArtworks)
                .disposedBy(disposeBag)

        Observables.combineLatest(suggestedArtworks, suggestedKeywords)
        { artworks, keywords ->
            mutableListOf<SearchBaseCellViewModel>()
                    .apply {
                        add(0, SearchTextHeaderViewModel(R.string.suggested))
                        addAll(keywords)
                        add(SearchTextHeaderViewModel(R.string.on_the_map))
                        addAll(getAmenitiesViewModels())
                        add(RowPaddingViewModel())
                        addAll(artworks)
                    }
        }
                .bindTo(cells)
                .disposedBy(disposeBag)

    }
}
