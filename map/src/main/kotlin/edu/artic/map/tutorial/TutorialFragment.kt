package edu.artic.map.tutorial

import android.app.Activity
import android.content.Context
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
import edu.artic.analytics.ScreenName
import edu.artic.db.INVALID_FLOOR
import edu.artic.map.R
import edu.artic.map.overrideMapAccess
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
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
    override val screenName: ScreenName? = null

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
                viewModel.currentIndex.onNext(position + positionOffset)
            }

            override fun onPageSelected(position: Int) {
                viewModel.onTutorialPageChanged(position)
            }
        })

        val floor = arguments!!.getInt(ARG_FLOOR, INVALID_FLOOR)
        viewModel.floor.onNext(floor)
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

        viewModel.currentIndex
                // Defensive coding: we really only expect values from 0f to 1f (both inclusive).
                .filter { it >= 0 }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    tutorialBack.alpha = Math.min(it, 1f)
                }
                .disposedBy(disposeBag)

        viewModel.currentIndex
                .withLatestFrom(viewModel.cells)
                .filter { (showDismiss, cells) ->
                    showDismiss.toInt() == cells.size.minus(1)
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    tutorialNext.text = getString(R.string.dismiss)
                }.disposedBy(disposeBag)

        viewModel.currentIndex
                .withLatestFrom(viewModel.cells)
                .filter { (showDismiss, cells) ->
                    showDismiss.toInt() < cells.size.minus(1)
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

        viewModel.floor
                .map { it == 0 }
                .bindToMain(tutorial_lower_level_text.visibility())
                .disposedBy(disposeBag)

        viewModel.floor
                .map { it == 1 }
                .bindToMain(tutorial_first_floor_text.visibility())
                .disposedBy(disposeBag)

        viewModel.floor
                .map { it == 2 }
                .bindToMain(tutorial_second_floor_text.visibility())
                .disposedBy(disposeBag)

        viewModel.floor
                .map { it == 3 }
                .bindToMain(tutorial_third_floor_text.visibility())
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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is Activity) {
            overrideMapAccess(context, View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        }
    }

    override fun onDetach() {
        super.onDetach()
        overrideMapAccess(activity, View.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
    }

    companion object {
        val ARG_FLOOR = "${TutorialFragment::class.java.simpleName}: floor"

        fun withExtras(floor: Int): TutorialFragment {
            val fragment = TutorialFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_FLOOR, floor)
            }
            return fragment
        }
    }

}