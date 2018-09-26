package edu.artic.exhibitions

import android.content.Intent
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textRes
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.asUrlViewIntent
import edu.artic.db.models.ArticExhibition
import edu.artic.details.R
import edu.artic.image.listenerSetHeight
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_exhibition_details.*
import kotlin.reflect.KClass


/**
 * This is where we show extra information about a specific [ArticExhibition].
 *
 * In the UI, it is more frequently called
 * # On View
 */
class ExhibitionDetailFragment : BaseViewModelFragment<ExhibitionDetailViewModel>() {

    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.OnViewDetails

    override val viewModelClass: KClass<ExhibitionDetailViewModel>
        get() = ExhibitionDetailViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_exhibition_details

    override val title = R.string.noTitle

    override fun hasTransparentStatusBar() = true

    private val exhibition by lazy { arguments!!.getParcelable<ArticExhibition>(ARG_EXHIBITION) }

    override fun onRegisterViewModel(viewModel: ExhibitionDetailViewModel) {
        viewModel.exhibition = exhibition
    }

    override fun setupBindings(viewModel: ExhibitionDetailViewModel) {
        viewModel.title
                .subscribe { appBarLayout.setTitleText(it) }
                .disposedBy(disposeBag)

        viewModel.imageUrl
                .subscribe {
                    val options = RequestOptions()
                            .dontAnimate()
                            .dontTransform()
                    Glide.with(this)
                            .load(it)
                            .apply(options)
                            .listenerSetHeight(appBarLayout.detailImage)
                            .into(appBarLayout.detailImage)
                }
                .disposedBy(disposeBag)

        viewModel.description
                .bindToMain(description.text())
                .disposedBy(disposeBag)

        viewModel.throughDate
                .map { getString(R.string.throughDate, it) }
                .bindToMain(throughDate.text())
                .disposedBy(disposeBag)

        viewModel.showOnMapButtonText
                .bindToMain(showOnMap.textRes())
                .disposedBy(disposeBag)

        viewModel.buyTicketsButtonText
                .bindToMain(buyTickets.textRes())
                .disposedBy(disposeBag)

        viewModel.location
                .map { (lat, long) -> lat != null && long != null }
                .bindToMain(showOnMap.visibility())
                .disposedBy(disposeBag)

        showOnMap.clicks()
                .subscribe { viewModel.onClickShowOnMap() }
                .disposedBy(disposeBag)

        buyTickets.clicks()
                .subscribe { viewModel.onClickBuyTickets() }
                .disposedBy(disposeBag)

    }

    override fun setupNavigationBindings(viewModel: ExhibitionDetailViewModel) {
        viewModel.navigateTo
                .subscribe { navEvent ->
                    when (navEvent) {
                        is Navigate.Forward -> {
                            val endpoint = navEvent.endpoint

                            when (endpoint) {
                                is ExhibitionDetailViewModel.NavigationEndpoint.ShowOnMap -> {
                                    analyticsTracker.reportEvent(ScreenCategoryName.Map, AnalyticsAction.mapShowExhibition, exhibition.title)
                                    val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                        putExtra(NavigationConstants.ARG_EXHIBITION_OBJECT, exhibition)
                                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                    }
                                    startActivity(mapIntent)
                                }

                                is ExhibitionDetailViewModel.NavigationEndpoint.BuyTickets -> {
                                    startActivity(endpoint.url.asUrlViewIntent())
                                }
                            }
                        }
                        is Navigate.Back -> {

                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }

    companion object {
        private const val ARG_EXHIBITION = "exhibition"

        fun argsBundle(exhibition: ArticExhibition) = Bundle().apply {
            putParcelable(ARG_EXHIBITION, exhibition)
        }
    }
}