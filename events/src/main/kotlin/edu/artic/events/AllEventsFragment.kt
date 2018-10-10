package edu.artic.events

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterTo
import edu.artic.adapter.itemChanges
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.events.recyclerview.AllEventsItemDecoration
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.subscribeBy
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
                 * this is specifically for handling the dateline above each day's events
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
        viewModel.navigateTo
                .filterTo<Navigate<AllEventsViewModel.NavigationEndpoint>, Navigate.Forward<AllEventsViewModel.NavigationEndpoint>>()
                .subscribeBy { navigation ->
                    val endpoint = navigation.endpoint
                    when (endpoint) {
                        is AllEventsViewModel.NavigationEndpoint.EventDetail -> {
                            val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                putExtras(EventDetailFragment.argsBundle(endpoint.event))
                            }
                            startActivity(intent)
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_events, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            when(it.itemId) {
                R.id.search -> {
                    val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                    startActivity(intent)
                    return true
                }
                else -> {
                    return super.onOptionsItemSelected(item)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}