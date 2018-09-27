package edu.artic.welcome

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.events.EventDetailFragment
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.navigation.NavigationConstants
import edu.artic.tours.TourDetailsFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_welcome.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<WelcomeViewModel>() {

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Home

    override val title = R.string.welcome

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override val layoutResId: Int
        get() = R.layout.fragment_welcome

    override fun hasTransparentStatusBar(): Boolean = true

    override fun hasHomeAsUpEnabled(): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        tourSummaryRecyclerView.layoutManager = layoutManager

        val decoration = DividerItemDecoration(view.context, DividerItemDecoration.HORIZONTAL)
        decoration.setDrawable(ContextCompat.getDrawable(view.context, R.drawable.space_decorator)!!)
        tourSummaryRecyclerView.addItemDecoration(decoration)

        val tourSummaryAdapter = WelcomeToursAdapter()
        tourSummaryRecyclerView.adapter = tourSummaryAdapter

        viewModel.tours
                .bindToMain(tourSummaryAdapter.itemChanges())
                .disposedBy(disposeBag)

        /* Build on view list*/
        val adapter = OnViewAdapter()
        val exhibitionLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        onViewRecyclerView.layoutManager = exhibitionLayoutManager
        onViewRecyclerView.adapter = adapter
        viewModel.exhibitions
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

        /* Build event summary list*/
        val eventsLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        eventsRecyclerView.layoutManager = eventsLayoutManager
        val eventsAdapter = WelcomeEventsAdapter()
        eventsRecyclerView.adapter = eventsAdapter
        viewModel.events
                .bindToMain(eventsAdapter.itemChanges())
                .disposedBy(disposeBag)

        viewModel.shouldPeekTourSummary
                .filter { it }
                .subscribe {
                    animateRecyclerView()
                }
                .disposedBy(disposeBag)

        appBarLayout.setOnSearchClickedConsumer(Consumer { viewModel.onClickSearch() })

        memberCardLink.clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onAcessMemberCardClickEvent()
                }.disposedBy(disposeBag)

        viewModel.welcomePrompt
                .bindToMain(welcomeMessage.text())
                .disposedBy(disposeBag)

        viewModel.currentCardHolder
                .subscribe { cardHolder ->
                    val firstName = cardHolder.split(" ").first()
                    val title = resources.getString(R.string.welcomeUser, firstName)
                    requestTitleUpdate(title)
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
        eventsAdapter.itemClicksWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickEvent(pos, model.event)
                }
                .disposedBy(disposeBag)

        val onViewAdapter = onViewRecyclerView.adapter as OnViewAdapter
        onViewAdapter.itemClicksWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickExhibition(pos, model.exhibition)
                }
                .disposedBy(disposeBag)


        val toursAdapter = tourSummaryRecyclerView.adapter as WelcomeToursAdapter
        toursAdapter.itemClicksWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickTour(pos, model.tour)
                }
                .disposedBy(disposeBag)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateData()
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
                                    val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                        putExtras(TourDetailsFragment.argsBundle(endpoint.tour))
                                    }
                                    startActivity(intent)
                                }
                                is WelcomeViewModel.NavigationEndpoint.ExhibitionDetail -> {
                                    val endpoint = navigation.endpoint as WelcomeViewModel.NavigationEndpoint.ExhibitionDetail
                                    val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                        putExtras(ExhibitionDetailFragment.argsBundle(endpoint.exhibition))
                                    }
                                    startActivity(intent)
                                }
                                is WelcomeViewModel.NavigationEndpoint.EventDetail -> {
                                    val endpoint = navigation.endpoint as WelcomeViewModel.NavigationEndpoint.EventDetail
                                    val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                        putExtras(EventDetailFragment.argsBundle(endpoint.event))
                                    }
                                    startActivity(intent)
                                }
                                WelcomeViewModel.NavigationEndpoint.Search -> {
                                    val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                                    startActivity(intent)
                                }
                                WelcomeViewModel.NavigationEndpoint.AccessMemberCard -> {
                                    val deepLinkIntent = NavigationConstants.INFO_MEMBER_CARD.asDeepLinkIntent().apply {
                                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                    }
                                    startActivity(deepLinkIntent)
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


