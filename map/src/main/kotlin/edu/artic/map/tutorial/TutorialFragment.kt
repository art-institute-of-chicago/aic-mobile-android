package edu.artic.map.tutorial

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.MotionEvent
import android.view.View
import com.fuzz.indicator.OffSetHint
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.adapter.toPagerAdapter
import edu.artic.analytics.ScreenCategoryName
import edu.artic.map.R
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_tutorial.*
import kotlin.reflect.KClass

class TutorialFragment : BaseViewModelFragment<TutorialViewModel>() {
    override val viewModelClass: KClass<TutorialViewModel> = TutorialViewModel::class
    override val title: Int = 0
    override val layoutResId: Int = R.layout.fragment_tutorial
    override val screenCategory: ScreenCategoryName? = null

    private val adapter = TutorialPopupAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnTouchListener { _, event ->
            if(event.action == MotionEvent.ACTION_DOWN) {
                viewModel.touched()
            }
            true
        }

        viewPager.adapter = adapter.toPagerAdapter()
        viewPagerIndicator.setViewPager(viewPager)
        viewPagerIndicator.setOffsetHints(OffSetHint.IMAGE_ALPHA)

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                viewModel.onTutorialPageChanged(position)
            }
        })
    }

    override fun setupBindings(viewModel: TutorialViewModel) {
        super.setupBindings(viewModel)

        viewModel.cells
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

        viewModel.tutorialPopupCurrentPage
                .subscribe {
                    if (viewPager.currentItem != it) {
                        viewPager.currentItem = it
                    }
                }.disposedBy(disposeBag)

        viewModel.tutorialTitle
                .map { getString(it) }
                .bindToMain(tutorialPopupTitle.text())
                .disposedBy(disposeBag)

        viewModel.showBack
                .bindToMain(tutorialBack.visibility(View.INVISIBLE))
                .disposedBy(disposeBag)

        viewModel.currentTutorialStage
                .map { it == TutorialViewModel.Stage.One }
                .subscribe {
                    tutorialLevelOne.visibility = if (it) View.VISIBLE else View.GONE
                    tutorialLevelTwo.visibility = if (it) View.GONE else View.VISIBLE
                }
                .disposedBy(disposeBag)

        tutorialNext.clicks()
                .subscribe {
                    viewModel.onPopupNextClick()
                }
                .disposedBy(disposeBag)

        tutorialBack.clicks()
                .subscribe {
                    viewModel.onPopupBackClick()
                }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: TutorialViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filterFlatMap({ it is Navigate.Back }, { it as Navigate.Back })
                .subscribe {
                    activity?.onBackPressed()
                }.disposedBy(navigationDisposeBag)
    }

}