package artic.edu.search

import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.models.ArticObject
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

open class SearchBaseViewModel @Inject constructor(protected val analyticsTracker: AnalyticsTracker)
    : NavViewViewModel<SearchBaseViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class ArticObjectDetails(val articObject: ArticObject) : NavigationEndpoint()
    }

    val cells: Subject<List<SearchBaseCellViewModel>> = BehaviorSubject.create()


    fun onClickItem(pos: Int, viewModel: SearchBaseCellViewModel) {
        when (viewModel) {
            is SearchTourCellViewModel -> {

            }
            is SearchExhibitionCellViewModel -> {

            }
            is SearchArtworkCellViewModel -> {

            }
            is SearchAmenitiesCellViewModel -> {

            }
            is SearchEmptyCellViewModel -> {

            }
            is SearchCircularCellViewModel -> {
                viewModel.artWork?.let { articObject ->
                    navigateTo.onNext(
                            Navigate.Forward(
                                    NavigationEndpoint.ArticObjectDetails(articObject)
                            )
                    )

                }
            }
            is SearchTextCellViewModel -> {

            }
            is SearchHeaderCellViewModel -> {

            }
        }
    }
}