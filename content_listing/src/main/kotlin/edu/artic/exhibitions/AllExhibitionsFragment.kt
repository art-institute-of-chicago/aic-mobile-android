package edu.artic.exhibitions

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
import edu.artic.decoration.AllExhibitionsItemDecoration
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_view_all.*
import kotlin.reflect.KClass

/**
 * This represents the `exhibition` sub-screen of the ':welcome' module.
 *
 * It shows titles, end dates, and a simple promotional picture for
 * each [exhibition][edu.artic.db.models.ArticExhibition] in a
 * single-column vertical list.
 *
 * # `Exhibitions` are often displayed under an '`On View`' header.
 *
 * @see AllExhibitionsAdapter
 */
class AllExhibitionsFragment : BaseViewModelFragment<AllExhibitionsViewModel>() {

    override val screenName: ScreenName
        get() = ScreenName.OnView

    override val viewModelClass: KClass<AllExhibitionsViewModel>
        get() = AllExhibitionsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_view_all

    override val title = R.string.onView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        val exhibitionsAdapter = AllExhibitionsAdapter()
        recyclerView.adapter = exhibitionsAdapter
        recyclerView.addItemDecoration(AllExhibitionsItemDecoration(1))

    }

    override fun setupBindings(viewModel: AllExhibitionsViewModel) {
        val adapter = (recyclerView.adapter as AllExhibitionsAdapter)

        /* Ensure search events go through ok. */
        searchIcon
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.onClickSearch()
                }
                .disposedBy(disposeBag)

        viewModel.exhibitions
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

        adapter.itemClicksWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickExhibition(pos, model.exhibition)
                }.disposedBy(disposeBag)


    }

    override fun setupNavigationBindings(viewModel: AllExhibitionsViewModel) {
        viewModel.navigateTo
                .observeOn(AndroidSchedulers.mainThread())
                .filterFlatMap({ it is Navigate.Forward }, { (it as Navigate.Forward).endpoint })
                .subscribeBy {
                    when (it) {
                        is AllExhibitionsViewModel.NavigationEndpoint.ExhibitionDetails -> {
                            val intent = NavigationConstants.DETAILS.asDeepLinkIntent().apply {
                                putExtras(ExhibitionDetailFragment.argsBundle(it.exhibition))
                            }
                            startActivity(intent)
                        }
                        AllExhibitionsViewModel.NavigationEndpoint.Search -> {
                            val intent = NavigationConstants.SEARCH.asDeepLinkIntent()
                            startActivity(intent)
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }
}