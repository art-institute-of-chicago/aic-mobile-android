package edu.artic.tours

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.navigation.NavigationConstants
import edu.artic.tours.recyclerview.AllToursItemDecoration
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_all_tours.*
import kotlin.reflect.KClass

class AllToursFragment : BaseViewModelFragment<AllToursViewModel>() {

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Tours

    override val viewModelClass: KClass<AllToursViewModel>
        get() = AllToursViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_all_tours

    override val title = R.string.tours

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                /**
                 * Since the ui requires the first item to be full screen width, and we are using a
                 * grid layout manager, we need to add this span size for position
                 */
                return if (position == 0) 2 else 1
            }

        }

        // NB: a LayoutManager _must_ be defined before initializing 'toursAdapter'
        recyclerView.layoutManager = layoutManager
        val toursAdapter = AllToursAdapter(recyclerView, viewModel.intro, viewModel.viewDisposeBag)
        recyclerView.adapter = toursAdapter
        recyclerView.addItemDecoration(AllToursItemDecoration(view.context, 2))

        /* Ensure search events go through ok. */
        appBarLayout.setExpanded(false)
        appBarLayout.setOnSearchClickedConsumer(Consumer { viewModel.onClickSearch() })
    }

    override fun setupBindings(viewModel: AllToursViewModel) {
        viewModel.tours
                .bindToMain((recyclerView.adapter as AllToursAdapter).itemChanges())
                .disposedBy(disposeBag)

        val adapter = recyclerView.adapter as AllToursAdapter

        adapter.itemClicksWithPosition()
                .subscribe { (pos, cell) ->
                    viewModel.onClickTour(pos, cell.tour)
                }.disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: AllToursViewModel) {
        viewModel.navigateTo
                .observeOn(AndroidSchedulers.mainThread())
                .filterFlatMap({ it is Navigate.Forward }, { (it as Navigate.Forward).endpoint })
                .subscribeBy {
                    when (it) {
                        AllToursViewModel.NavigationEndpoint.Search -> {
                            val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                            startActivity(intent)
                        }
                        is AllToursViewModel.NavigationEndpoint.TourDetails -> {
                            val endpoint = it
                            val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                putExtras(TourDetailsFragment.argsBundle(endpoint.tour))
                            }
                            startActivity(intent)
                        }
                    }
                }
                .disposedBy(navigationDisposeBag)
    }
}