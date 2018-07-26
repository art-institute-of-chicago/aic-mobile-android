package edu.artic.events

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.adapter.itemSelectionsWithPosition
import edu.artic.events.recyclerview.AllEventsItemDecoration
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_all_events.*
import kotlin.reflect.KClass

class AllEventsFragment : BaseViewModelFragment<AllEventsViewModel>() {

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Events

    override val viewModelClass: KClass<AllEventsViewModel>
        get() = AllEventsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_all_events
    override val title: String
        get() = "Events" // TODO: add to strings or figure out language stuff

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        val toursAdapter = AllEventsAdapter()
        recyclerView.adapter = toursAdapter
        recyclerView.addItemDecoration(AllEventsItemDecoration(view.context, 2))
    }

    override fun setupBindings(viewModel: AllEventsViewModel) {
        val adapter = recyclerView.adapter as AllEventsAdapter
        viewModel.events
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)
        adapter.itemSelectionsWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickEvent(pos, model.event)
                }
                .disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: AllEventsViewModel) {
        viewModel.navigateTo.subscribe { navigation ->
            when (navigation) {
                is Navigate.Forward -> {
                    val endpoint = navigation.endpoint
                    when (endpoint) {
                        is AllEventsViewModel.NavigationEndpoint.EventDetail -> {
                            navController.navigate(
                                    R.id.goToEventDetailsAction,
                                    EventDetailFragment.argBundle(endpoint.event)
                            )
                        }
                    }
                }
                is Navigate.Back -> {
                    //Nothing in vm requires back
                }
            }
        }.disposedBy(disposeBag)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_events, menu)
    }
}