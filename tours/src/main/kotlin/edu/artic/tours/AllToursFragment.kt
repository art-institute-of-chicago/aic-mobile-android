package edu.artic.tours

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.tours.recyclerview.AllToursItemDecoration
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_all_tours.*
import kotlin.reflect.KClass

class AllToursFragment : BaseViewModelFragment<AllToursViewModel>() {

    override val viewModelClass: KClass<AllToursViewModel>
        get() = AllToursViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_all_tours
    override val title: String
        get() = "Tours" // TODO: add to strings or figure out language stuff

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if(position == 0) 2 else 1
            }

        }
        recyclerView.layoutManager = layoutManager
        val toursAdapter = AllToursAdapter(recyclerView, viewModel.intro, viewModel.viewDisposeBag)
        recyclerView.adapter = toursAdapter
        recyclerView.addItemDecoration(AllToursItemDecoration(view.context, 2))

    }

    override fun setupBindings(viewModel: AllToursViewModel) {
        viewModel.tours
                .bindToMain((recyclerView.adapter as AllToursAdapter).itemChanges())
                .disposedBy(disposeBag)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_all_tours, menu)
    }
}