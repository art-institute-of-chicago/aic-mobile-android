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
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.adapter.toPagerAdapter
import edu.artic.analytics.ScreenCategoryName
import edu.artic.map.R
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_tutorial.*
import kotlin.reflect.KClass

/**
 * This is designed to show an overlay on top of [edu.artic.map.MapFragment].
 *
 * It consists of a 'popup dialog' and some 'metadata tags'. In a practical
 * sense these are (respectively)
 *
 * * a [android.view.ViewGroup] with high [elevation][View.getElevation] and
 * a cool shadow
 * * a few little [TextViews][android.widget.TextView] on top of a translucent scrim
 */
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
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                viewModel.showBack.onNext(position + positionOffset)
                viewModel.showDismiss.onNext(position + positionOffset)
            }

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
                // Defensive coding: we really only expect values from 0f to 1f (both inclusive).
                .filter { it >= 0 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    tutorialBack.alpha = Math.min(it, 1f)
                }
                .disposedBy(disposeBag)

        viewModel.showDismiss
                .filter {
                    it.toInt() == viewPager.adapter?.count?.minus(1)
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    tutorialNext.text = getString(R.string.dismiss)
                }.disposedBy(disposeBag)

        viewModel.showDismiss
                .filter {
                    if (viewPager.adapter == null) {
                        false
                    } else {
                        it.toInt() < viewPager.adapter!!.count.minus(1)
                    }
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    tutorialNext.text = getString(R.string.next)
                }.disposedBy(disposeBag)

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