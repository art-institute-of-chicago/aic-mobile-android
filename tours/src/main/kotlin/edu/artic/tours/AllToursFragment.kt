package edu.artic.tours

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Build tour summary list*/
        val layoutManager = GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        val toursAdapter = AllToursAdapter()
        recyclerView.adapter = toursAdapter
        recyclerView.addItemDecoration(AllToursItemDecoration(view.context, 2, true))

    }

    override fun setupBindings(viewModel: AllToursViewModel) {
        viewModel.tours
                .bindToMain((recyclerView.adapter as AllToursAdapter).itemChanges())
                .disposedBy(disposeBag)
        viewModel.intro
                .map {
                    !it.isEmpty()
                }
                .bindToMain(intro.visibility())
                .disposedBy(disposeBag)
        viewModel.intro
                .bindToMain(intro.text())
                .disposedBy(disposeBag)
    }
}