package edu.artic.tours

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.base.utils.fromHtml
import edu.artic.base.utils.listenerClean
import edu.artic.base.utils.updateDetailTitle
import edu.artic.db.models.ArticTour
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_tour_details.*
import timber.log.Timber
import kotlin.reflect.KClass

class TourDetailsFragment : BaseViewModelFragment<TourDetailsViewModel>() {

    override val viewModelClass: KClass<TourDetailsViewModel>
        get() = TourDetailsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_tour_details
    override val title: String
        get() = ""

    override fun hasTransparentStatusBar() = true


    private val tour: ArticTour get() = arguments!!.getParcelable(ARG_TOUR)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }

        val layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        val tourStopAdapter = TourDetailsStopAdapter()
        recyclerView.adapter = tourStopAdapter
        val decoration = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(view.context, R.drawable.tour_detail_tour_stop_divider)?.let {
            decoration.setDrawable(it)
        }
        recyclerView.addItemDecoration(decoration)
        recyclerView.isNestedScrollingEnabled = true

    }

    override fun onRegisterViewModel(viewModel: TourDetailsViewModel) {
        viewModel.tour = tour
    }

    override fun setupBindings(viewModel: TourDetailsViewModel) {
        viewModel.imageUrl
                .subscribe {
                    Glide.with(this)
                            .load(it)
                            .listenerClean({
                                false
                            }, {
                                Timber.d("${it.bounds}")
                                false
                            })
                            .into(tourImage)
                }.disposedBy(disposeBag)

        viewModel.titleText
                .subscribe {
                    expandedTitle.text = it
                    toolbarTitle.text = it
                }.disposedBy(disposeBag)
        viewModel.stopsText
                .bindToMain(tourStops.text())
                .disposedBy(disposeBag)
        viewModel.timeText
                .bindToMain(tourTime.text())
                .disposedBy(disposeBag)
        viewModel.startTourButtonText
                .bindToMain(startTourButtonText.text())
                .disposedBy(disposeBag)
        viewModel.description
                .map { it.fromHtml() }
                .bindToMain(description.text())
                .disposedBy(disposeBag)
        viewModel.intro
                .map { it.fromHtml() }
                .bindToMain(intro.text())
                .disposedBy(disposeBag)

        startTourButton.clicks()
                .subscribe { viewModel.onClickStartTour() }
                .disposedBy(disposeBag)

        val adapter = recyclerView.adapter as TourDetailsStopAdapter
        viewModel.stops
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

    }

    companion object {
        private val ARG_TOUR = "${TourDetailsFragment::class.java.simpleName}:TOUR"

        fun argsBundle(tour: ArticTour) = Bundle().apply {
            putParcelable(ARG_TOUR, tour)
        }
    }
}