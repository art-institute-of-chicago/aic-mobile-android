package edu.artic.tours

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.itemChanges
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.fromHtml
import edu.artic.db.models.ArticTour
import edu.artic.map.MapFragment
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.cell_tour_details_stop.view.*
import kotlinx.android.synthetic.main.fragment_tour_details.*
import kotlin.reflect.KClass

class TourDetailsFragment : BaseViewModelFragment<TourDetailsViewModel>() {

    override val viewModelClass: KClass<TourDetailsViewModel>
        get() = TourDetailsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_tour_details
    override val title: String
        get() = ""

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.TourDetails

    override fun hasTransparentStatusBar() = true


    private val tour: ArticTour get() = arguments!!.getParcelable(ARG_TOUR)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
            adapter = TourDetailsStopAdapter()
            val decoration = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(view.context, R.drawable.tour_detail_tour_stop_divider)?.let {
                decoration.setDrawable(it)
            }
            addItemDecoration(decoration)
            isNestedScrollingEnabled = true
        }

    }

    override fun onRegisterViewModel(viewModel: TourDetailsViewModel) {
        viewModel.tour = tour
    }

    override fun setupBindings(viewModel: TourDetailsViewModel) {
        viewModel.imageUrl
                .subscribe {
                    Glide.with(this)
                            .load(it)
                            .into(appBarLayout.detailImage)
                    Glide.with(this)
                            .load(it)
                            .into(tourDetailIntroCell.image)
                }.disposedBy(disposeBag)

        viewModel.titleText
                .subscribe { appBarLayout.setTitleText(it) }.disposedBy(disposeBag)

        viewModel.introductionTitleText
                .bindToMain(tourDetailIntroCell.tourStopTitle.text())
                .disposedBy(disposeBag)

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

        viewModel.location
                .bindToMain(tourDetailIntroCell.tourStopGallery.text())
                .disposedBy(disposeBag)

        startTourButton.clicks()
                .defaultThrottle()
                .subscribe { viewModel.onClickStartTour() }
                .disposedBy(disposeBag)

        val adapter = recyclerView.adapter as TourDetailsStopAdapter
        viewModel.stops
                .bindToMain(adapter.itemChanges())
                .disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: TourDetailsViewModel) {
        viewModel.navigateTo
                .subscribe { navigationEndpoint ->
                    when (navigationEndpoint) {
                        is Navigate.Forward -> {
                            when (navigationEndpoint.endpoint) {
                                is TourDetailsViewModel.NavigationEndpoint.Map -> {
                                    /**
                                     * navigate to map using args
                                     */
                                    val endpoint = navigationEndpoint.endpoint as TourDetailsViewModel.NavigationEndpoint.Map
                                    navController.navigate(
                                            R.id.loadMap,
                                            MapFragment.argsBundle(endpoint.tour)
                                    )
                                }

                            }
                        }
                        is Navigate.Back -> {
                        }
                    }

                }.disposedBy(disposeBag)
    }

    companion object {
        private val ARG_TOUR = "${TourDetailsFragment::class.java.simpleName}:TOUR"

        fun argsBundle(tour: ArticTour) = Bundle().apply {
            putParcelable(ARG_TOUR, tour)
        }
    }
}