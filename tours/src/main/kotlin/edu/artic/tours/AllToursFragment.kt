package edu.artic.tours

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
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
        viewModel.tours
                .bindToMain(toursAdapter.itemChanges())
                .disposedBy(disposeBag)
    }

    override fun setupBindings(viewModel: AllToursViewModel) {

    }
}