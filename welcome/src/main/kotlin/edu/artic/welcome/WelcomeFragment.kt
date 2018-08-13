package edu.artic.welcome

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemSelectionsWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.events.EventDetailFragment
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.tours.TourDetailsFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import kotlinx.android.synthetic.main.app_bar_layout.view.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Home

    override val title: String
        get() = "Welcome"

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    override fun hasTransparentStatusBar(): Boolean = true

    override fun hasHomeAsUpEnabled(): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * TODO:: move this logic away into the app bar view class
         * TODO:: Make a Custom AppBar view that dynamically switches the toolbar type (collapsible and non collapsible)
         */
        val appBar = appBarLayout as AppBarLayout
        appBar.apply {
            addOnOffsetChangedListener { aBarLayout, verticalOffset ->
                val progress: Double = 1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
                aBarLayout.searchIcon.background.alpha = (progress * 255).toInt()
                aBarLayout.flagIcon.drawable.alpha = (progress * 255).toInt()
                aBarLayout.expandedImage.background.alpha = (progress * 255).toInt()
            }
        }

        /* Build tour summary list*/
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        tourSummaryRecyclerView.layoutManager = layoutManager

        val decoration = DividerItemDecoration(view.context, DividerItemDecoration.HORIZONTAL)
        decoration.setDrawable(ContextCompat.getDrawable(view.context, R.drawable.space_decorator)!!)
        tourSummaryRecyclerView.addItemDecoration(decoration)

        val tourSummaryAdapter = WelcomeToursAdapter()
        tourSummaryRecyclerView.adapter = tourSummaryAdapter

        viewModel.tours.bindToMain(tourSummaryAdapter.itemChanges()).disposedBy(disposeBag)

        /* Build on view list*/
        val adapter = OnViewAdapter()
        val exhibitionLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        onViewRecyclerView.layoutManager = exhibitionLayoutManager
        onViewRecyclerView.adapter = adapter
        viewModel.exhibitions.bindToMain(adapter.itemChanges()).disposedBy(disposeBag)

        /* Build event summary list*/
        val eventsLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        eventsRecyclerView.layoutManager = eventsLayoutManager
        val eventsAdapter = WelcomeEventsAdapter()
        eventsRecyclerView.adapter = eventsAdapter
        viewModel.events.bindToMain(eventsAdapter.itemChanges()).disposedBy(disposeBag)


        viewModel.shouldPeekTourSummary
                .filter { it }
                .subscribe {
                    animateRecyclerView()
                }
                .disposedBy(disposeBag)

    }

    override fun setupBindings(viewModel: WelcomeViewModel) {
        toursSeeAllLink.clicks()
                .defaultThrottle()
                .subscribe { viewModel.onClickSeeAllTours() }
                .disposedBy(disposeBag)

        onViewLink.clicks()
                .defaultThrottle()
                .subscribe { viewModel.onClickSeeAllOnView() }
                .disposedBy(disposeBag)

        eventsLink.clicks()
                .defaultThrottle()
                .subscribe { viewModel.onClickSeeAllEvents() }
                .disposedBy(disposeBag)

        val eventsAdapter = eventsRecyclerView.adapter as WelcomeEventsAdapter
        eventsAdapter.itemSelectionsWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickEvent(pos, model.event)
                }
                .disposedBy(disposeBag)

        val onViewAdapter = onViewRecyclerView.adapter as OnViewAdapter
        onViewAdapter.itemSelectionsWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickExhibition(pos, model.exhibition)
                }
                .disposedBy(disposeBag)


        val toursAdapter = tourSummaryRecyclerView.adapter as WelcomeToursAdapter
        toursAdapter.itemSelectionsWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickTour(pos, model.tour)
                }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: WelcomeViewModel) {
        viewModel.navigateTo
                .subscribe { navigation ->
                    when (navigation) {
                        is Navigate.Forward -> {
                            when (navigation.endpoint) {
                                is WelcomeViewModel.NavigationEndpoint.SeeAllTours -> {
                                    navController.navigate(R.id.goToAllToursAction)
                                }
                                is WelcomeViewModel.NavigationEndpoint.SeeAllOnView -> {
                                    navController.navigate(R.id.goToAllExhibitionsAction)
                                }
                                is WelcomeViewModel.NavigationEndpoint.SeeAllEvents -> {
                                    navController.navigate(R.id.goToAllEventsAction)
                                }
                                is WelcomeViewModel.NavigationEndpoint.TourDetail -> {
                                    val endpoint = navigation.endpoint as WelcomeViewModel.NavigationEndpoint.TourDetail
                                    navController
                                            .navigate(
                                                    R.id.goToTourDetailsAction,
                                                    TourDetailsFragment.argsBundle(
                                                            endpoint.tour
                                                    )
                                            )
                                }
                                is WelcomeViewModel.NavigationEndpoint.ExhibitionDetail -> {
                                    val endpoint = navigation.endpoint as WelcomeViewModel.NavigationEndpoint.ExhibitionDetail
                                    navController
                                            .navigate(
                                                    R.id.goToExhibitionDetailsAction,
                                                    ExhibitionDetailFragment.argsBundle(
                                                            endpoint.exhibition
                                                    )
                                            )
                                }
                                is WelcomeViewModel.NavigationEndpoint.EventDetail -> {
                                    val endpoint = navigation.endpoint as WelcomeViewModel.NavigationEndpoint.EventDetail
                                    navController
                                            .navigate(
                                                    R.id.goToEventDetailsAction,
                                                    EventDetailFragment.argsBundle(
                                                            endpoint.event
                                                    )
                                            )
                                }
                            }
                        }
                        is Navigate.Back -> {

                        }
                    }
                }
                .disposedBy(navigationDisposeBag)
    }


    /**
     * Peek Animation.
     * Scroll RecyclerView to the last item and back again to first item.
     */
    private fun animateRecyclerView() {

        Observable.interval(2000, 500, TimeUnit.MILLISECONDS)
                .take(2)
                .subscribe { it ->
                    if (it == 0L) {
                        tourSummaryRecyclerView.smoothScrollToPosition(1)
                    } else {
                        tourSummaryRecyclerView.smoothScrollToPosition(0)
                        viewModel.onPeekedTour()
                    }
                }
                .disposedBy(disposeBag)
    }


}


