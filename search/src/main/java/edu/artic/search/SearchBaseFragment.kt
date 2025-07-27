package edu.artic.search

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenName
import edu.artic.artwork.ArtworkDetailFragment
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.base.utils.dpToPixels
import edu.artic.base.utils.hideSoftKeyboard
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.navigation.NavigationConstants
import edu.artic.navigation.NavigationConstants.Companion.ARG_AMENITY_TYPE
import edu.artic.search.databinding.FragmentSearchResultsSubBinding
import edu.artic.tours.TourDetailsFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject


abstract class SearchBaseFragment<VB : ViewBinding, TViewModel : SearchBaseViewModel> :
    BaseViewModelFragment<VB, TViewModel>() {
    override val title = R.string.global_empty_string
    override val screenName: ScreenName? = null

    override val overrideStatusBarColor: Boolean = false

    @Inject
    lateinit var customTabManager: CustomTabManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val width = resources.displayMetrics.widthPixels
        val ten = resources.dpToPixels(10f)
        val size = resources.dpToPixels(70f)
        val numToDisplay = ((width - ten) / size).toInt()
        SearchResultsAdapter.MAX_ARTWORKS_PER_ROW = numToDisplay

        val lm = GridLayoutManager(
            view.context,
            SearchResultsAdapter.MAX_ARTWORKS_PER_ROW
        )

        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return ((binding as FragmentSearchResultsSubBinding).resultsRV.adapter as SearchResultsAdapter).getSpanCount(
                    position
                )
            }
        }

        (binding as FragmentSearchResultsSubBinding).resultsRV.apply {
            adapter = SearchResultsAdapter()
            layoutManager = lm
            addItemDecoration(SearchDividerItemDecoration(this.context))
        }

    }

    override fun setupBindings(viewModel: TViewModel) {
        super.setupBindings(viewModel)

        val adapter =
            (binding as FragmentSearchResultsSubBinding).resultsRV.adapter as SearchResultsAdapter

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
            .subscribe {
                (binding as FragmentSearchResultsSubBinding).resultsRV.layoutManager?.scrollToPosition(
                    0
                )
            }
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

                val searchNavController = baseActivity.navController
                when (endpoint) {
                    is SearchBaseViewModel.NavigationEndpoint.ArtworkOnMap -> {
                        val o: ArticSearchArtworkObject = endpoint.articObject
                        val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                            putExtra(NavigationConstants.ARG_SEARCH_OBJECT, o)
                            flags =
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        }
                        startActivity(mapIntent)
                        requireActivity().finish()
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
                        val term = endpoint.searchTerm
                        searchNavController.navigate(
                            R.id.goToSearchAudioDetails,
                            ArtworkDetailFragment.argsBundle(o, term)
                        )
                    }
                    is SearchBaseViewModel.NavigationEndpoint.AmenityOnMap -> {
                        val amenity = endpoint.type
                        val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                            putExtra(ARG_AMENITY_TYPE, amenity.type)
                            flags =
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        }
                        startActivity(mapIntent)
                        requireActivity().finish()
                    }
                    SearchBaseViewModel.NavigationEndpoint.HideKeyboard -> {
                        requireActivity().hideSoftKeyboard()
                    }
                    is SearchBaseViewModel.NavigationEndpoint.Web -> {
                        val url = endpoint.url
                        customTabManager.openUrlOnChromeCustomTab(requireContext(), Uri.parse(url))
                    }
                }
            }
            .disposedBy(navigationDisposeBag)
    }

}