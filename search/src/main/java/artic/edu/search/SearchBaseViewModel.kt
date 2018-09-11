package artic.edu.search

import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.models.ArticExhibition
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

open class SearchBaseViewModel @Inject constructor(
        protected val analyticsTracker: AnalyticsTracker,
        protected val searchResultsManager: SearchResultsManager)
    : NavViewViewModel<SearchBaseViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class TourDetails(val tour: ArticTour) : NavigationEndpoint()
        data class ExhibitionDetails(val exhibition: ArticExhibition) : NavigationEndpoint()
        data class ArtworkDetails(val articObject: ArticObject) : NavigationEndpoint()
        data class ArtworkOnMap(val articObject: ArticObject) : NavigationEndpoint()
        object AmenityOnMap : NavigationEndpoint() // TODO: somehting with this?
        object Web : NavigationEndpoint()

    }

    val cells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()


    fun onClickItem(pos: Int, viewModel: SearchBaseCellViewModel) {
        when (viewModel) {
            is SearchTourCellViewModel -> {
                navigateTo.onNext(
                        Navigate.Forward(NavigationEndpoint.TourDetails(viewModel.articTour))
                )
            }
            is SearchExhibitionCellViewModel -> {
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.ExhibitionDetails(viewModel.articExhibition)
                        )
                )
            }
            is SearchArtworkCellViewModel -> {
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.ArtworkDetails(viewModel.articObject)
                        )
                )
            }
            is SearchAmenitiesCellViewModel -> {
                navigateTo.onNext(
                        Navigate.Forward(
                                NavigationEndpoint.AmenityOnMap
                        )
                )
            }
            is SearchEmptyCellViewModel -> {
                navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Web))
            }
            is SearchCircularCellViewModel -> {
                viewModel.artWork?.let { articObject ->
                    navigateTo.onNext(
                            Navigate.Forward(
                                    NavigationEndpoint.ArtworkOnMap(articObject)
                            )
                    )

                }
            }
            is SearchTextCellViewModel -> {
                searchResultsManager.onChangeSearchText(viewModel.textString)
            }
            is SearchHeaderCellViewModel -> {
                //TODO: handle this case
            }
        }
    }
}