package edu.artic.search

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import androidx.navigation.Navigation
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationConstants.Companion.ARG_AMENITY_TYPE
import edu.artic.tours.TourDetailsFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_search_results_sub.*
import java.util.concurrent.TimeUnit

abstract class SearchBaseFragment<TViewModel : SearchBaseViewModel> : BaseViewModelFragment<TViewModel>() {
    override val title = R.string.noTitle
    override val layoutResId: Int = R.layout.fragment_search_results_sub
    override val screenCategory: ScreenCategoryName? = null

    override val overrideStatusBarColor: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lm = GridLayoutManager(view.context, 5)
        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return (resultsRV.adapter as SearchResultsAdapter).getSpanCount(position)
            }
        }

        resultsRV.apply {
            adapter = SearchResultsAdapter()
            layoutManager = lm
            addItemDecoration(SearchDividerItemDecoration(this.context))
        }

    }

    override fun setupBindings(viewModel: TViewModel) {
        super.setupBindings(viewModel)

        val adapter = resultsRV.adapter as SearchResultsAdapter

        viewModel.cells
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.setItemsList(it)
                }
                .disposedBy(disposeBag)

        /**
         * Weird timing issue with setItemList prevents calling scrollToPosition right after setItemList
         * if tried looks like scrollToPosition just gets forgotten about during layout pass or something
         * thus a delay of 50 - 100 milliseconds to make the scroll during next layout pass
         */
        viewModel.cells
                .delay(50, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { resultsRV.layoutManager.scrollToPosition(0) }
                .disposedBy(disposeBag)


        adapter.itemClicksWithPosition()
                .subscribe { (pos, vm) ->
                    viewModel.onClickItem(pos, vm)
                }.disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: TViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filter { it is Navigate.Forward }
                .map { it as Navigate.Forward }
                .subscribe {
                    val endpoint: SearchBaseViewModel.NavigationEndpoint = it.endpoint

                    val searchNavController = Navigation.findNavController(requireActivity(), R.id.container)
                    when (endpoint) {
                        is SearchBaseViewModel.NavigationEndpoint.ArtworkOnMap -> {
                            val o: ArticSearchArtworkObject = endpoint.articObject
                            val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                putExtra(NavigationConstants.ARG_SEARCH_OBJECT, o)
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            startActivity(mapIntent)
                        }
                        is SearchBaseViewModel.NavigationEndpoint.TourDetails -> {
                            val o = endpoint.tour
                            searchNavController.navigate(
                                    R.id.goToTourDetails,
                                    TourDetailsFragment.argsBundle(o)
                            )
                        }
                        is SearchBaseViewModel.NavigationEndpoint.ExhibitionDetails -> {
                            val o = endpoint.exhibition
                            searchNavController.navigate(
                                    R.id.goToExhibitionDetails,
                                    ExhibitionDetailFragment.argsBundle(o)
                            )
                        }
                        is SearchBaseViewModel.NavigationEndpoint.ArtworkDetails -> {
                            val o = endpoint.articObject
                            searchNavController.navigate(
                                    R.id.goToSearchAudioDetails,
                                    SearchAudioDetailFragment.argsBundle(o)
                            )
                        }
                        is SearchBaseViewModel.NavigationEndpoint.AmenityOnMap -> {
                            val amenity = endpoint.type
                            val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                putExtra(ARG_AMENITY_TYPE, amenity.type)
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            startActivity(mapIntent)
                        }
                        SearchBaseViewModel.NavigationEndpoint.Web -> {

                        }
                    }
                }
                .disposedBy(navigationDisposeBag)
    }

}