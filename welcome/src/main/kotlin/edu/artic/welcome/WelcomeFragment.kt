package edu.artic.welcome

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.events.EventDetailFragment
import edu.artic.exhibitions.ExhibitionDetailFragment
import edu.artic.message.PagedMessageFragment
import edu.artic.navigation.NavigationConstants
import edu.artic.tours.TourDetailsFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import edu.artic.welcome.databinding.FragmentWelcomeBinding
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class WelcomeFragment : BaseViewModelFragment<FragmentWelcomeBinding, WelcomeViewModel>() {

    override val screenName: ScreenName
        get() = ScreenName.Home

    override val title = R.string.welcome_title

    override val viewModelClass: KClass<WelcomeViewModel>
        get() = WelcomeViewModel::class

    override fun hasTransparentStatusBar(): Boolean = true

    override fun hasHomeAsUpEnabled(): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = LinearLayoutManager(
            activity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.tourSection.list.layoutManager = layoutManager

        val decoration = DividerItemDecoration(
            view.context,
            DividerItemDecoration.HORIZONTAL
        )
        decoration.setDrawable(
            ContextCompat.getDrawable(
                view.context,
                R.drawable.space_decorator
            )!!
        )
        binding.tourSection.list.addItemDecoration(decoration)

        val tourSummaryAdapter = WelcomeToursAdapter()
        binding.tourSection.list.adapter = tourSummaryAdapter

        viewModel.tours
            .bindToMain(tourSummaryAdapter.itemChanges())
            .disposedBy(disposeBag)

        /* Build on view list*/
        val adapter = OnViewAdapter()
        val exhibitionLayoutManager =
            LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        binding.exhibitionSection.list.layoutManager = exhibitionLayoutManager
        binding.exhibitionSection.list.adapter = adapter
        viewModel.exhibitions
            .bindToMain(adapter.itemChanges())
            .disposedBy(disposeBag)

        viewModel.exhibitions
            .map { it.isNotEmpty() }
            .bindToMain(binding.exhibitionSection.root.visibility())
            .disposedBy(disposeBag)


        /* Build event summary list*/
        val eventsLayoutManager =
            LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        binding.eventSection.list.layoutManager = eventsLayoutManager
        val eventsAdapter = WelcomeEventsAdapter()
        binding.eventSection.list.adapter = eventsAdapter

        viewModel.events
            .bindToMain(eventsAdapter.itemChanges())
            .disposedBy(disposeBag)

        viewModel.events
            .map { it.isNotEmpty() }
            .bindToMain(binding.eventSection.root.visibility())
            .disposedBy(disposeBag)

        viewModel.shouldPeekTourSummary
            .filter { it }
            .subscribe {
                animateRecyclerView()
            }
            .disposedBy(disposeBag)

        binding.appBarLayout.setOnSearchClickedConsumer { viewModel.onClickSearch() }

        if (BuildConfig.IS_RENTAL) {
            binding.memberCardLink.visibility = View.GONE
        }
        binding.memberCardLink.clicks()
            .defaultThrottle()
            .subscribe {
                viewModel.onAccessMemberCardClickEvent()
            }
            .disposedBy(disposeBag)

        viewModel.welcomePrompt
            .map { it.isBlank() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { promptIsBlank ->
                if (promptIsBlank) {
                    //Hide the welcome prompt's view and adjust the card link view's padding
                    binding.welcomeMessage.visibility = View.GONE

                    binding.memberCardLink.setPaddingRelative(
                        resources.getDimensionPixelSize(R.dimen.marginDouble),
                        resources.getDimensionPixelSize(R.dimen.marginQuad),
                        resources.getDimensionPixelSize(R.dimen.marginDouble),
                        resources.getDimensionPixelSize(R.dimen.marginQuad)
                    )
                } else {
                    binding.welcomeMessage.visibility = View.VISIBLE

                    binding.memberCardLink.setPaddingRelative(
                        resources.getDimensionPixelSize(R.dimen.marginDouble),
                        0,
                        resources.getDimensionPixelSize(R.dimen.marginDouble),
                        resources.getDimensionPixelSize(R.dimen.marginDouble)
                    )
                }
            }
            .disposedBy(disposeBag)

        viewModel.welcomePrompt
            .map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).toString() }
            .bindToMain(binding.welcomeMessage.text())
            .disposedBy(disposeBag)

        viewModel.currentCardHolder
            .subscribeBy { cardHolder ->
                val firstName = cardHolder.split(" ").first()
                val title = resources.getString(R.string.welcome_title_logged_in, firstName)
                requestTitleUpdate(title)
            }
            .disposedBy(disposeBag)

    }

    override fun setupBindings(viewModel: WelcomeViewModel) {

        binding.tourSection.label.setText(R.string.welcome_tours_header)
        binding.exhibitionSection.label.setText(R.string.welcome_on_view_header)
        binding.eventSection.label.setText(R.string.welcome_events_header)

        binding.tourSection.seeAllLink.clicks()
            .defaultThrottle()
            .subscribe { viewModel.onClickSeeAllTours() }
            .disposedBy(disposeBag)

        binding.exhibitionSection.seeAllLink.clicks()
            .defaultThrottle()
            .subscribe { viewModel.onClickSeeAllOnView() }
            .disposedBy(disposeBag)

        binding.eventSection.seeAllLink.clicks()
            .defaultThrottle()
            .subscribe { viewModel.onClickSeeAllEvents() }
            .disposedBy(disposeBag)

        val eventsAdapter = binding.eventSection.list.adapter as WelcomeEventsAdapter
        eventsAdapter.itemClicksWithPosition()
            .subscribe { (pos, model) ->
                viewModel.onClickEvent(pos, model.event)
            }
            .disposedBy(disposeBag)

        val onViewAdapter = binding.exhibitionSection.list.adapter as OnViewAdapter
        onViewAdapter.itemClicksWithPosition()
            .subscribe { (pos, model) ->
                viewModel.onClickExhibition(pos, model.exhibition)
            }
            .disposedBy(disposeBag)


        val toursAdapter = binding.tourSection.list.adapter as WelcomeToursAdapter
        toursAdapter.itemClicksWithPosition()
            .subscribe { (pos, model) ->
                viewModel.onClickTour(pos, model.tour)
            }
            .disposedBy(disposeBag)
    }

    override fun onResume() {
        super.onResume()

        viewModel.updateData()
        viewModel.onScreenAppeared()
    }

    override fun setupNavigationBindings(viewModel: WelcomeViewModel) {
        viewModel.navigateTo
            .subscribe { navigation ->
                when (navigation) {
                    is Navigate.Forward -> {
                        when (val endpoint = navigation.endpoint) {
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
                                val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                    putExtras(TourDetailsFragment.argsBundle(endpoint.tour))
                                }
                                startActivity(intent)
                            }
                            is WelcomeViewModel.NavigationEndpoint.ExhibitionDetail -> {
                                val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                    putExtras(ExhibitionDetailFragment.argsBundle(endpoint.exhibition))
                                }
                                startActivity(intent)
                            }
                            is WelcomeViewModel.NavigationEndpoint.EventDetail -> {
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
                                val deepLinkIntent =
                                    NavigationConstants.INFO_MEMBER_CARD.asDeepLinkIntent().apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                    }
                                startActivity(deepLinkIntent)
                            }
                            is WelcomeViewModel.NavigationEndpoint.Messages -> {
                                val manager = activity?.supportFragmentManager
                                    ?: return@subscribe
                                val tag = "PagedMessageFragment"
                                (manager.findFragmentByTag(tag) as? androidx.fragment.app.DialogFragment)?.dismiss()
                                PagedMessageFragment.create(endpoint.messages).show(manager, tag)
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
                    binding.tourSection.list.smoothScrollToPosition(1)
                } else {
                    binding.tourSection.list.smoothScrollToPosition(0)
                    viewModel.onPeekedTour()
                }
            }
            .disposedBy(disposeBag)
    }


}


