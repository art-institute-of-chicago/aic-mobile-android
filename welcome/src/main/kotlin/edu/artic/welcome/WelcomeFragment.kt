package edu.artic.welcome

import android.graphics.Color
import android.graphics.drawable.ClipDrawable.VERTICAL
import android.graphics.drawable.ColorDrawable
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
import edu.artic.tours.AllToursFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import kotlinx.android.synthetic.main.app_bar_layout.view.*
import kotlinx.android.synthetic.main.fragment_welcome.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {
    override val title: String
        get() = "Welcome"

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

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

        context?.let {
            /* Build tour summary list*/
            val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            tourSummaryRecyclerView.layoutManager = layoutManager

            val decoration = DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)
            decoration.setDrawable(ContextCompat.getDrawable(it, R.drawable.space_decorator)!!)
            tourSummaryRecyclerView.addItemDecoration(decoration)

            val tourSummaryAdapter = WelcomeToursAdapter()
            tourSummaryRecyclerView.adapter = tourSummaryAdapter


            viewModel.tours.bindToMain(tourSummaryAdapter.itemChanges()).disposedBy(disposeBag)
        }


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
    }

    override fun setupNavigationBindings(viewModel: WelcomeViewModel) {
        Timber.d("NavigationDisposeBagSize pre setup: ${navigationDisposeBag.size()}")
        viewModel.navigateTo
                .subscribe {navigation ->
                    when(navigation) {
                        is Navigate.Forward -> {
                            when(navigation.endpoint) {
                                is WelcomeViewModel.NavigationEndpoint.SeeAllTours -> {
                                    fragmentManager?.let {fm ->
                                        val ft = fm.beginTransaction()
                                        ft.replace(R.id.container, AllToursFragment())
                                        ft.addToBackStack("AllToursFragment")
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
        Timber.d("NavigationDisposeBagSize post setup: ${navigationDisposeBag.size()}")
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


