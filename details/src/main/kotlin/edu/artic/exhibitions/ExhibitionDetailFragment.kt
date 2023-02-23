package edu.artic.exhibitions

//import kotlinx.android.synthetic.main.fragment_exhibition_details.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.math.MathUtils
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.EventCategoryName
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.base.utils.fromHtml
import edu.artic.base.utils.trimDownBlankLines
import edu.artic.db.models.ArticExhibition
import edu.artic.details.BuildConfig
import edu.artic.details.R
import edu.artic.details.databinding.FragmentExhibitionDetailsBinding
import edu.artic.image.GlideApp
import edu.artic.image.ImageViewScaleInfo
import edu.artic.image.listenerSetHeight
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import kotlin.reflect.KClass


/**
 * This is where we show extra information about a specific [ArticExhibition].
 *
 * In the UI, it is more frequently called
 * # On View
 */
class ExhibitionDetailFragment :
    BaseViewModelFragment<FragmentExhibitionDetailsBinding, ExhibitionDetailViewModel>() {
    @Inject
    lateinit var customTabManager: CustomTabManager

    override val screenName: ScreenName
        get() = ScreenName.OnViewDetails

    override val viewModelClass: KClass<ExhibitionDetailViewModel>
        get() = ExhibitionDetailViewModel::class

    override val title = R.string.global_empty_string

    override val customToolbarColorResource: Int
        get() = R.color.audioBackground


    private val exhibition by lazy {
        //Support arguments from the search activity
        if (arguments?.containsKey(ARG_EXHIBITION) == true) {
            requireArguments().getParcelable<ArticExhibition>(ARG_EXHIBITION)
        } else {
            requireActivity().intent.getParcelableExtra<ArticExhibition>(ARG_EXHIBITION)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.IS_RENTAL) {
            binding.buyTickets.visibility = View.GONE
        }
    }

    override fun onRegisterViewModel(viewModel: ExhibitionDetailViewModel) {
        viewModel.exhibition = exhibition
    }

    override fun setupBindings(viewModel: ExhibitionDetailViewModel) {
        viewModel.title
            .subscribe {
                binding.expandedTitle.text = it
                binding.toolbarTitle.text = it
                /**
                 * When the scrollView is
                 * - scrolled down and expandedTitle is about to go behind the toolbar,
                 *   update the toolbar's title.
                 * - scrolled up and expandedTitle is visible (seen in scrollView) clear
                 *   the toolbar's title.
                 */
                val toolbarHeight = toolbar?.layoutParams?.height ?: 0
                binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener
                { _, _, scrollY, _, _ ->
                    val threshold = binding.image.measuredHeight + toolbarHeight / 2
                    val alpha: Float = (scrollY - threshold + 40f) / 40f
                    binding.toolbarTitle.alpha = MathUtils.clamp(alpha, 0f, 1f)
                    binding.expandedTitle.alpha = 1 - alpha
                })

            }
            .disposedBy(disposeBag)

        viewModel.imageUrl
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val options = RequestOptions()
                    .dontAnimate()
                    .dontTransform()

                val scaleInfo = ImageViewScaleInfo(
                    placeHolderScaleType = ImageView.ScaleType.CENTER_CROP,
                    imageScaleType = ImageView.ScaleType.MATRIX
                )
                binding.image.post {
                    GlideApp.with(this)
                        .load("$it?w=${binding.image.measuredWidth}&h=${binding.image.measuredHeight}")
                        .apply(options)
                        .placeholder(R.color.placeholderBackground)
                        .error(R.drawable.placeholder_large)
                        .listenerSetHeight(binding.image, scaleInfo)
                        .into(binding.image)
                }
            }
            .disposedBy(disposeBag)

        viewModel.description
            .map {
                it.trimDownBlankLines().fromHtml()
            }
            .bindToMain(binding.description.text())
            .disposedBy(disposeBag)

        viewModel.galleryTitle
            .bindToMain(binding.galleryTitle.text())
            .disposedBy(disposeBag)

        viewModel.galleryTitle
            .map { it.isNotBlank() }
            .bindToMain(binding.galleryTitle.visibility())
            .disposedBy(disposeBag)

        viewModel.throughDate
            .map { getString(R.string.content_through_date, it) }
            .bindToMain(binding.throughDate.text())
            .disposedBy(disposeBag)



        viewModel.location
            .map { (lat, long) -> lat != null && long != null }
            .bindToMain(binding.showOnMap.visibility())
            .disposedBy(disposeBag)

        binding.showOnMap.clicks()
            .subscribe { viewModel.onClickShowOnMap() }
            .disposedBy(disposeBag)

        binding.buyTickets.clicks()
            .subscribe { viewModel.onClickBuyTickets() }
            .disposedBy(disposeBag)

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun setupNavigationBindings(viewModel: ExhibitionDetailViewModel) {
        viewModel.navigateTo
            .subscribe { navEvent ->
                when (navEvent) {
                    is Navigate.Forward -> {
                        val endpoint = navEvent.endpoint

                        when (endpoint) {
                            is ExhibitionDetailViewModel.NavigationEndpoint.ShowOnMap -> {
                                analyticsTracker.reportEvent(
                                    EventCategoryName.Map,
                                    AnalyticsAction.mapShowExhibition,
                                    exhibition?.title.orEmpty()
                                )
                                val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                    putExtra(NavigationConstants.ARG_EXHIBITION_OBJECT, exhibition)
                                    flags =
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                                }
                                startActivity(mapIntent)
                                requireActivity().finish()
                            }

                            is ExhibitionDetailViewModel.NavigationEndpoint.BuyTickets -> {
                                customTabManager.openUrlOnChromeCustomTab(
                                    requireContext(),
                                    Uri.parse(endpoint.url)
                                )
                            }
                        }
                    }
                    is Navigate.Back -> {

                    }
                }
            }.disposedBy(navigationDisposeBag)
    }

    companion object {
        const val ARG_EXHIBITION = "exhibition"

        fun argsBundle(exhibition: ArticExhibition) = Bundle().apply {
            putParcelable(ARG_EXHIBITION, exhibition)
        }
    }
}