package edu.artic.exhibitions

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.tours.recyclerview.AllExhibitionsItemDecoration
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.cell_all_exhibitions_layout.view.*
import kotlinx.android.synthetic.main.fragment_all_exhibitions.*
import kotlin.reflect.KClass

class AllExhibitionsFragment : BaseViewModelFragment<AllExhibitionsViewModel>() {

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
        val exhibitionsAdapter = AllExhibitionsAdapter(viewModel)
        recyclerView.adapter = exhibitionsAdapter
        recyclerView.addItemDecoration(AllExhibitionsItemDecoration(view.context, 1))

    }

    override fun setupBindings(viewModel: AllExhibitionsViewModel) {
        viewModel.exhibitions
                .bindToMain((recyclerView.adapter as AllExhibitionsAdapter).itemChanges())
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: AllExhibitionsViewModel) {
        viewModel.navigateTo.subscribe {
            when (it) {

                is Navigate.Forward -> {
                    when(it.endpoint) {

                        is AllExhibitionsViewModel.NavigationEndpoint.ExhibitionDetails -> {
                            val endpoint = it.endpoint as AllExhibitionsViewModel.NavigationEndpoint.ExhibitionDetails
                            val view = recyclerView.findViewHolderForAdapterPosition(endpoint.pos).itemView.image
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