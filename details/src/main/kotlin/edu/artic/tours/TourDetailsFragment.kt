package edu.artic.tours

//import kotlinx.android.synthetic.main.cell_tour_details_stop.view.*
//import kotlinx.android.synthetic.main.fragment_tour_details.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fuzz.rx.*
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.itemSelections
import com.jakewharton.rxbinding2.widget.text
import edu.artic.adapter.*
import edu.artic.analytics.EventCategoryName
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.dpToPixels
import edu.artic.base.utils.fromHtml
import edu.artic.db.models.ArticTour
import edu.artic.details.R
import edu.artic.details.databinding.FragmentTourDetailsBinding
import edu.artic.image.GlideApp
import edu.artic.image.ImageViewScaleInfo
import edu.artic.image.updateImageScaleType
import edu.artic.language.LanguageAdapter
import edu.artic.localization.SpecifiesLanguage
import edu.artic.localization.nameOfLanguageForAnalytics
import edu.artic.navigation.NavigationConstants
import edu.artic.ui.SizedColorDrawable
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlin.reflect.KClass

class TourDetailsFragment :
    BaseViewModelFragment<FragmentTourDetailsBinding, TourDetailsViewModel>() {

    override val viewModelClass: KClass<TourDetailsViewModel>
        get() = TourDetailsViewModel::class

    override val title = R.string.global_empty_string

    override val screenName: ScreenName
        get() = ScreenName.TourDetails

    override val customToolbarColorResource: Int
        get() = R.color.audioBackground

    private val tour by lazy {
        //Support arguments from the search activity
        if (arguments?.containsKey(ARG_TOUR) == true) {
            arguments!!.getParcelable<ArticTour>(ARG_TOUR)
        } else {
            requireActivity().intent.getParcelableExtra<ArticTour>(ARG_TOUR)
        }
    }

    private val translationsAdapter: BaseRecyclerViewAdapter<SpecifiesLanguage, BaseViewHolder>
        get() = binding.languageSelector.adapter.baseRecyclerViewAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(
                view.context,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = TourDetailsStopAdapter()
            addItemDecoration(obtainDecoration(view.context))
            isNestedScrollingEnabled = true
        }

        binding.languageSelector.adapter = LanguageAdapter().toBaseAdapter()
        //Fix for the scrollview starting mid-scroll on the recyclerview
        binding.recyclerView.isFocusable = false
        //Pass fling events to the parent 'tourScrollView' layout
        binding.recyclerView.isNestedScrollingEnabled = false
        binding.languageSelector.requestFocus()
    }

    private fun obtainDecoration(context: Context): DividerItemDecoration {
        val decoration = DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )

        decoration.setDrawable(
            SizedColorDrawable(
                color = ContextCompat.getColor(context, R.color.tourDetailTourStopDivider),
                height = context.resources.dpToPixels(1f).toInt()
            )
        )

        return decoration
    }

    override fun onRegisterViewModel(viewModel: TourDetailsViewModel) {
        viewModel.tour = tour
    }

    override fun setupBindings(viewModel: TourDetailsViewModel) {
        viewModel.imageUrl
            .subscribe { it ->

                val scaleInfo = ImageViewScaleInfo(
                    placeHolderScaleType = ImageView.ScaleType.CENTER_CROP,
                    imageScaleType = ImageView.ScaleType.MATRIX
                )

                GlideApp.with(this)
                    .load(it)
                    .error(R.drawable.placeholder_large)
                    .placeholder(R.color.placeholderBackground)
                    .updateImageScaleType(binding.image, scaleInfo)
                    .into(binding.image)


                GlideApp.with(this)
                    .load(it)
                    .placeholder(R.color.placeholderBackground)
                    .error(R.drawable.placeholder_thumb)
                    .into(binding.tourDetailIntroCell.image)

            }.disposedBy(disposeBag)

        viewModel.titleText
            .subscribe {
                binding.expandedTitle.text = it
                binding.toolbarTitle.text = it
                /**
                 * When the tourScrollView is
                 * - scrolled down and expandedTitle is about to go behind the toolbar,
                 *   update the toolbar's title.
                 * - scrolled up and expandedTitle is visible (seen in tourScrollView) clear
                 *   the toolbar's title.
                 */
                val toolbarHeight = toolbar?.layoutParams?.height ?: 0
                binding.tourScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener
                { _, _, scrollY, _, _ ->
                    val threshold = binding.image.measuredHeight + toolbarHeight / 2
                    val alpha: Float = (scrollY - threshold + 40f) / 40f
                    binding.toolbarTitle.alpha = MathUtils.clamp(alpha, 0f, 1f)
                    binding.expandedTitle.alpha = 1 - alpha
                })

            }.disposedBy(disposeBag)

        viewModel.introductionTitleText
            .bindToMain(binding.tourDetailIntroCell.tourStopTitle.text())
            .disposedBy(disposeBag)

        binding.tourDetailIntroCell.root.clicks()
            .subscribe {
                viewModel.onClickStartTour()
            }
            .disposedBy(disposeBag)

        viewModel.stopsText
            .map { resources.getString(R.string.tour_stop_count, it) }
            .bindToMain(binding.tourStops.text())
            .disposedBy(disposeBag)

        viewModel.timeText
            .bindToMain(binding.tourTime.text())
            .disposedBy(disposeBag)


        viewModel.description
            .map { it.fromHtml() }
            .bindToMain(binding.description.text())
            .disposedBy(disposeBag)

        viewModel.intro
            .map { it.fromHtml() }
            .bindToMain(binding.intro.text())
            .disposedBy(disposeBag)

        viewModel.location
            .bindToMain(binding.tourDetailIntroCell.tourStopGallery.text())
            .disposedBy(disposeBag)

        binding.startTourButton.clicks()
            .defaultThrottle()
            .subscribe { viewModel.onClickStartTour() }
            .disposedBy(disposeBag)

        val adapter = binding.recyclerView.adapter as TourDetailsStopAdapter
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

        viewModel.availableTranslations
            .map { it.size > 1 }
            .bindToMain(binding.languageSelector.visibility(View.INVISIBLE))
            .disposedBy(disposeBag)

        binding.languageSelector
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
                binding.languageSelector.setSelection(translationsAdapter.itemIndexOf(chosen))
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
                        analyticsTracker.reportEvent(
                            EventCategoryName.LanguageTour,
                            endpoint.locale.nameOfLanguageForAnalytics(),
                            endpoint.tour.title
                        )
                        startActivity(NavigationConstants.MAP.asDeepLinkIntent()
                            .apply {
                                putExtra(NavigationConstants.ARG_TOUR, endpoint.tour)
                                putExtra(NavigationConstants.ARG_TOUR_START_STOP, endpoint.stop)
                                flags =
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                        )
                        requireActivity().finish()
                    }
                }

            }.disposedBy(navigationDisposeBag)
    }

    companion object {
        val ARG_TOUR = "${TourDetailsFragment::class.java.simpleName}:TOUR"

        fun argsBundle(tour: ArticTour) = Bundle().apply {
            putParcelable(ARG_TOUR, tour)
        }
    }
}