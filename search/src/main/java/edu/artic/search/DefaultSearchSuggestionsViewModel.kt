package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.daos.ArticDataObjectDao
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.daos.ArticSearchObjectDao
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class DefaultSearchSuggestionsViewModel @Inject constructor(
    searchSuggestionsDao: ArticSearchObjectDao,
    analyticsTracker: AnalyticsTracker,
    searchManager: SearchResultsManager,
    dataObjectDao: ArticDataObjectDao,
    galleryDao: ArticGalleryDao,
) : SearchBaseViewModel(analyticsTracker, searchManager, dataObjectDao, galleryDao) {

    private fun getAmenitiesViewModels(): List<SearchBaseCellViewModel> {
        return listOf(
            SearchAmenitiesCellViewModel(
                R.drawable.ic_icon_amenity_map_restaurant,
                SuggestedMapAmenities.Dining
            ),
            SearchAmenitiesCellViewModel(
                R.drawable.ic_icon_amenity_map_lounge,
                SuggestedMapAmenities.MembersLounge
            ),
            SearchAmenitiesCellViewModel(
                R.drawable.ic_icon_amenity_map_shop,
                SuggestedMapAmenities.GiftShop
            ),
            SearchAmenitiesCellViewModel(
                R.drawable.ic_icon_amenity_map_restroom,
                SuggestedMapAmenities.Restrooms
            )
        )
    }

    init {
        searchSuggestionsDao.getDataObject()
            .toObservable()
            .map { suggestedSearchOptions ->
                val keywords = suggestedSearchOptions
                    .searchStrings
                    .map { keyword ->
                        SearchTextCellViewModel(keyword)
                    }

                mutableListOf<SearchBaseCellViewModel>()
                    .apply {
                        add(0, SearchTextHeaderViewModel(R.string.search_suggested))
                        addAll(keywords)
                        add(SearchTextHeaderViewModel(R.string.search_on_the_map_header))
                        addAll(getAmenitiesViewModels())
                    }

            }
            .bindTo(cells)
            .disposedBy(disposeBag)
    }
}
