package edu.artic.events

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.core.math.MathUtils
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenName
import edu.artic.base.utils.customTab.CustomTabManager
import edu.artic.base.utils.fromHtml
import edu.artic.base.utils.trimDownBlankLines
import edu.artic.db.models.ArticEvent
import edu.artic.details.BuildConfig
import edu.artic.details.R
import edu.artic.details.databinding.FragmentEventDetailsBinding
import edu.artic.image.GlideApp
import edu.artic.image.ImageViewScaleInfo
import edu.artic.image.listenerAnimateSharedTransaction
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
import java.util.Locale
import javax.inject.Inject
import kotlin.reflect.KClass

class EventDetailFragment :
    BaseViewModelFragment<FragmentEventDetailsBinding, EventDetailViewModel>() {
    @Inject
    lateinit var customTabManager: CustomTabManager

    override val screenName: ScreenName
        get() = ScreenName.EventDetails
    override val viewModelClass: KClass<EventDetailViewModel>
        get() = EventDetailViewModel::class

    override val title = R.string.global_empty_string

    override val customToolbarColorResource: Int
        get() = R.color.audioBackground

    private val event by lazy { requireActivity().intent.getParcelableExtra<ArticEvent>(ARG_EVENT) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.image.transitionName = event?.title

        if (BuildConfig.IS_RENTAL) {
            binding.registerToday.visibility = View.GONE
        }
    }

    override fun onRegisterViewModel(viewModel: EventDetailViewModel) {
        viewModel.event = event
    }

    override fun setupBindings(viewModel: EventDetailViewModel) {
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
            .subscribe {
                val options = RequestOptions()
                    .dontAnimate()
                    .dontTransform()

                val scaleInfo = ImageViewScaleInfo(
                    placeHolderScaleType = ImageView.ScaleType.CENTER_CROP,
                    imageScaleType = ImageView.ScaleType.MATRIX
                )

                GlideApp.with(this)
                    .load(it)
                    .apply(options)
                    .error(R.drawable.placeholder_large)
                    .placeholder(R.color.placeholderBackground)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listenerAnimateSharedTransaction(this, binding.image, scaleInfo)
                    .into(binding.image)
            }
            .disposedBy(disposeBag)

        viewModel.metaData
            .bindToMain(binding.metaData.text())
            .disposedBy(disposeBag)

        viewModel.description
            .map {
                it.trimDownBlankLines().fromHtml()
            }
            .bindToMain(binding.description.text())
            .disposedBy(disposeBag)

        viewModel.throughDate
            .map { getString(R.string.content_through_date, it) }
            .bindToMain(binding.throughDate.text())
            .disposedBy(disposeBag)

        viewModel.location
            .bindToMain(binding.location.text())
            .disposedBy(disposeBag)

        if (!BuildConfig.IS_RENTAL) {
            Observables
                .combineLatest(
                    viewModel.eventButtonText.map { it.isNotEmpty() },
                    viewModel.hasEventUrl,
                    viewModel.isOnSale
                )
                .map { it.first && it.second && it.third }
                .bindToMain(binding.registerToday.visibility())
                .disposedBy(disposeBag)
        } else {
            Observables
                .combineLatest(
                    viewModel.eventButtonText.map { it.isNotEmpty() },
                    viewModel.hasEventUrl
                )
                .map { it.first && it.second }
                .bindToMain(binding.visitWebsiteToRegister.visibility())
                .disposedBy(disposeBag)
        }

        viewModel.eventButtonText
            .map {
                when (it.toLowerCase(Locale.ROOT)) {
                    "buy tickets" -> getString(R.string.event_buy_tickets_action)
                    "register" -> getString(R.string.event_register_action)
                    else -> it
                }
            }
            .bindToMain(binding.registerToday.text())
            .disposedBy(disposeBag)

        viewModel.buttonCaptionText
            .map { it.trimDownBlankLines().fromHtml() }
            .bindToMain(binding.buttonCaption.text())
            .disposedBy(disposeBag)

        viewModel.buttonCaptionText
            .map { it.isNotBlank() }
            .bindToMain(binding.buttonCaption.visibility())
            .disposedBy(disposeBag)

        binding.registerToday.clicks()
            .subscribe { viewModel.onClickRegisterToday() }
            .disposedBy(disposeBag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            requireActivity().finish()
        }
    }

    override fun setupNavigationBindings(viewModel: EventDetailViewModel) {

        viewModel.navigateTo.subscribe {
            when (it) {
                is Navigate.Forward -> {
                    when (it.endpoint) {
                        is EventDetailViewModel.NavigationEndpoint.LoadUrl -> {
                            val endpoint =
                                it.endpoint as EventDetailViewModel.NavigationEndpoint.LoadUrl
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
        }.disposedBy(disposeBag)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        val ARG_EVENT = "${EventDetailFragment::class.java.simpleName}: event"

        fun argsBundle(event: ArticEvent) = Bundle().apply {
            putParcelable(ARG_EVENT, event)
        }

    }
}