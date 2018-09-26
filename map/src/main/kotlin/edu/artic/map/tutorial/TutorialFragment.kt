package edu.artic.map.tutorial

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.map.R
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_tutorial.*
import kotlin.reflect.KClass

class TutorialFragment : BaseViewModelFragment<TutorialViewModel>() {
    override val viewModelClass: KClass<TutorialViewModel> = TutorialViewModel::class
    override val title: Int = 0
    override val layoutResId: Int = R.layout.fragment_tutorial
    override val screenCategory: ScreenCategoryName? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnTouchListener { _, _ -> true }


        recyclerView.apply {
            adapter = TutorialPopupAdapter()
            layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        }

    }

    override fun setupBindings(viewModel: TutorialViewModel) {
        super.setupBindings(viewModel)
        val adapter = recyclerView.adapter as TutorialPopupAdapter
        viewModel.cells
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)
    }

}