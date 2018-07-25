package edu.artic.welcome

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import androidx.navigation.Navigation
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import edu.artic.adapter.itemChanges
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import kotlinx.android.synthetic.main.app_bar_layout.view.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import kotlinx.android.synthetic.main.welcome_on_view_cell_layout.view.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {
    override val title: String
        get() = "Welcome"

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    override fun hasTransparentStatusBar(): Boolean = true

    override fun hasHomeAsUpEnabled(): Boolean  = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * TODO:: move this logic away into the app bar view class
         * TODO:: Make a Custom AppBar view that dynamically switches the toolbar type (collapsible and non collapsible)
         */
        val appBar = appBarLayout as AppBarLayout
        (appBarLayout as AppBarLayout).apply {
            addOnOffsetChangedListener { aBarLayout, verticalOffset ->
                val progress: Double = 1 - Math.abs(verticalOffset) / aBarLayout.totalScrollRange.toDouble()
                appBar.searchIcon.background.alpha = (progress * 255).toInt()
                appBar.flagIcon.drawable.alpha = (progress * 255).toInt()
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
        val adapter = OnViewAdapter(viewModel)
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
    }

    override fun setupNavigationBindings(viewModel: WelcomeViewModel) {
        viewModel.navigateTo
                .subscribe { navigation ->
                    when (navigation) {
                        is Navigate.Forward -> {
                            when (navigation.endpoint) {
                                is WelcomeViewModel.NavigationEndpoint.SeeAllTours -> {
                                    this.view?.let {
                                        Navigation.findNavController(it).navigate(R.id.gotToAllToursAction)
                                    }
                                }
                                is WelcomeViewModel.NavigationEndpoint.SeeAllOnView -> {
                                    this.view?.let {
                                        Navigation.findNavController(it).navigate(R.id.goToAllExhibitionsAction)
                                    }
                                }
                                is WelcomeViewModel.NavigationEndpoint.SeeAllEvents -> {
                                    this.view?.let {
                                        Navigation.findNavController(it).navigate(R.id.goToAllEventsAction)
                                    }
                                }
                                is WelcomeViewModel.NavigationEndpoint.TourDetail -> {

                                }
                                is WelcomeViewModel.NavigationEndpoint.ExhibitionDetail -> {
                                    val endpoint = navigation.endpoint as WelcomeViewModel.NavigationEndpoint.ExhibitionDetail
                                    val view = onViewRecyclerView.findViewHolderForAdapterPosition(endpoint.pos).itemView.image
                                    fragmentManager?.let { fm ->
                                        val ft = fm.beginTransaction()
                                        ft.replace(R.id.container, ExhibitionDetailFragment.newInstance(endpoint.exhibition))
                                        ft.addSharedElement(view, view.transitionName)
                                        ft.addToBackStack("ExhibitionDetail")
                                        ft.commit()
                                    }
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


