package edu.artic.exhibitions

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.adapter.itemClicksWithPosition
import edu.artic.tours.recyclerview.AllExhibitionsItemDecoration
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_all_exhibitions.*
import kotlin.reflect.KClass

class AllExhibitionsFragment : BaseViewModelFragment<AllExhibitionsViewModel>() {

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.OnView

    override val viewModelClass: KClass<AllExhibitionsViewModel>
        get() = AllExhibitionsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_all_exhibitions
    override val title: String
        get() = "On View" // TODO: add to strings or figure out language stuff

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = GridLayoutManager(activity, 1, GridLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        val exhibitionsAdapter = AllExhibitionsAdapter()
        recyclerView.adapter = exhibitionsAdapter
        recyclerView.addItemDecoration(AllExhibitionsItemDecoration(view.context, 1))

    }

    override fun setupBindings(viewModel: AllExhibitionsViewModel) {
        val adapter = (recyclerView.adapter as AllExhibitionsAdapter)
        viewModel.exhibitions
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

        adapter.itemClicksWithPosition()
                .subscribe { (pos, model) ->
                    viewModel.onClickExhibition(pos, model.exhibition)
                }.disposedBy(disposeBag)


    }

    override fun setupNavigationBindings(viewModel: AllExhibitionsViewModel) {
        viewModel.navigateTo.subscribe {
            when (it) {
                is Navigate.Forward -> {
                    when (it.endpoint) {
                        is AllExhibitionsViewModel.NavigationEndpoint.ExhibitionDetails -> {
                            val endpoint = it.endpoint as AllExhibitionsViewModel.NavigationEndpoint.ExhibitionDetails
                            navController.navigate(
                                    R.id.goToExhibitionDetailsAction,
                                    ExhibitionDetailFragment.argsBundle(
                                            endpoint.exhibition
                                    )
                            )
                        }
                    }
                }
                else -> {

                }
            }
        }.disposedBy(disposeBag)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_exhibitions, menu)
    }
}