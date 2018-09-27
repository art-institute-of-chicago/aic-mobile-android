package edu.artic.events

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.events.recyclerview.AllEventsItemDecoration
import edu.artic.navigation.NavigationConstants
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

    override val title = R.string.events

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventsAdapter = AllEventsAdapter()
        val layoutManager = GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                /**
                 * this is specifically for handling tours with a header
                 */
                return when(eventsAdapter.getItemOrNull(position)) {
                    is AllEventsCellHeaderViewModel -> {
                        2
                    }
                    else -> {
                        1
                    }
                }
            }

        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = eventsAdapter
        recyclerView.addItemDecoration(AllEventsItemDecoration(view.context, 2, eventsAdapter))
    }

    override fun setupBindings(viewModel: AllEventsViewModel) {
        val adapter = recyclerView.adapter as AllEventsAdapter
        viewModel.events
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)
        adapter.itemClicksWithPosition()
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
                            val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                putExtras(EventDetailFragment.argsBundle(endpoint.event))
                            }
                            startActivity(intent)
                        }
                    }
                }
                is Navigate.Back -> {
                    //Nothing in vm requires back
                }
            }
        }.disposedBy(navigationDisposeBag)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_events, menu)
    }
}