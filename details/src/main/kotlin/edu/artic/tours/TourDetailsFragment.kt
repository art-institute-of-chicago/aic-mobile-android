package edu.artic.tours

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.*
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.itemSelections
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.adapter.*
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.fromHtml
import edu.artic.db.models.ArticTour
import edu.artic.details.R
import edu.artic.language.LanguageAdapter
import edu.artic.language.LanguageSelectorViewBackground
import edu.artic.localization.SpecifiesLanguage
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.cell_tour_details_stop.view.*
import kotlinx.android.synthetic.main.fragment_tour_details.*
import kotlin.reflect.KClass

class TourDetailsFragment : BaseViewModelFragment<TourDetailsViewModel>() {

    override val viewModelClass: KClass<TourDetailsViewModel>
        get() = TourDetailsViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_tour_details

    override val title = R.string.noTitle

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.TourDetails

    override fun hasTransparentStatusBar() = true


    private val tour by lazy {
        //Support arguments from the search activity
        if (arguments?.containsKey(ARG_TOUR) == true) {
            arguments!!.getParcelable<ArticTour>(ARG_TOUR)
        } else {
            requireActivity().intent.extras.getParcelable<ArticTour>(ARG_TOUR)
        }
    }

    private val translationsAdapter: BaseRecyclerViewAdapter<SpecifiesLanguage, BaseViewHolder>
        get() = languageSelector.adapter.baseRecyclerViewAdapter()

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

        languageSelector.adapter = LanguageAdapter().toBaseAdapter()

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

        tourDetailIntroCell.clicks()
                .subscribe {
                    viewModel.onClickStartTour()
                }
                .disposedBy(disposeBag)

        viewModel.stopsText
                .map { resources.getString(R.string.stops, it) }
                .bindToMain(tourStops.text())
                .disposedBy(disposeBag)

        viewModel.timeText
                .bindToMain(tourTime.text())
                .disposedBy(disposeBag)

        viewModel.startTourButtonText
                .bindToMain(startTourButtonText.textRes())
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

        adapter.itemClicks()
                .subscribeBy { item ->
                    viewModel.tourStopClicked(item)
                }.disposedBy(disposeBag)

        viewModel.availableTranslations
                .bindToMain(translationsAdapter.itemChanges())
                .disposedBy(disposeBag)

        LanguageSelectorViewBackground(languageSelector)
                .listenToLayoutChanges()
                .disposedBy(disposeBag)

        languageSelector
                .itemSelections()
                .distinctUntilChanged()
                .filter { position -> position >= 0 }
                .map { position ->
                    val translation = translationsAdapter.getItem(position)
                    translation as ArticTour.Translation
                }
                .bindTo(viewModel.chosenTranslation)
                .disposedBy(disposeBag)

        viewModel.chosenTranslation
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { chosen ->
                    languageSelector.setSelection(translationsAdapter.itemIndexOf(chosen))
                }
                .disposedBy(disposeBag)


    }

    override fun setupNavigationBindings(viewModel: TourDetailsViewModel) {
        viewModel.navigateTo
                .filterTo<Navigate<TourDetailsViewModel.NavigationEndpoint>,
                        Navigate.Forward<TourDetailsViewModel.NavigationEndpoint>>()
                .subscribe { forward ->
                    val endpoint = forward.endpoint
                    when (endpoint) {
                        is TourDetailsViewModel.NavigationEndpoint.Map -> {
                            startActivity(NavigationConstants.MAP.asDeepLinkIntent()
                                    .apply {
                                        putExtra(NavigationConstants.ARG_TOUR, endpoint.tour)
                                        putExtra(NavigationConstants.ARG_TOUR_START_STOP, endpoint.stop)
                                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                    }
                            )
                            requireActivity().finish()
                        }
                    }

                }.disposedBy(disposeBag)
    }

    companion object {
        public val ARG_TOUR = "${TourDetailsFragment::class.java.simpleName}:TOUR"

        fun argsBundle(tour: ArticTour) = Bundle().apply {
            putParcelable(ARG_TOUR, tour)
        }
    }
}