package edu.artic.tours

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.content.listing.R
import edu.artic.navigation.NavigationConstants
import edu.artic.decoration.AllToursItemDecoration
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_view_all.*
import kotlin.reflect.KClass

/**
 * This represents the `tour` sub-screen of the ':welcome' module.
 *
 * It shows titles, descriptions, number of tour stops, and other
 * essential information about each [tour][edu.artic.db.models.ArticTour]
 * in a dual-column vertical grid. At the top of the list is a
 * small blurb explaining the connection between tours and audio
 * commentaries.
 *
 * Unlike events and exhibitions, tours may come with translations
 * into other languages.
 *
 * @see AllToursAdapter
 */
class AllToursFragment : BaseViewModelFragment<AllToursViewModel>() {

    override val screenName: ScreenName
        get() = ScreenName.Tours

    override val viewModelClass: KClass<AllToursViewModel>
        get() = AllToursViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_view_all

    override val title = R.string.tours

    /**
     * The host activity can be trusted to use our preferred color,
     * `@color/colorPrimary`, but it might have been told to hide
     * the status bar. By returning `true` here we reaffirm that
     * color choice.
     */
    override val overrideStatusBarColor: Boolean = true

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
        recyclerView.addItemDecoration(AllToursItemDecoration(2))

        /* Ensure search events go through ok. */
        searchIcon
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onClickSearch()
                }
                .disposedBy(disposeBag)
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