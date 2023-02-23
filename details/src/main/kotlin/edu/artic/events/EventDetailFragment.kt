package edu.artic.events

import android.net.Uri
import android.os.Bundle
import androidx.core.math.MathUtils
import androidx.core.widget.NestedScrollView
import android.view.View
import android.widget.ImageView
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
import edu.artic.image.GlideApp
import edu.artic.image.ImageViewScaleInfo
import edu.artic.image.listenerAnimateSharedTransaction
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import io.reactivex.rxkotlin.Observables
//import kotlinx.android.synthetic.main.fragment_event_details.*
import java.util.*
import javax.inject.Inject
import kotlin.reflect.KClass

class EventDetailFragment : BaseViewModelFragment<EventDetailViewModel>() {
    @Inject
    lateinit var customTabManager: CustomTabManager

    override val screenName: ScreenName
        get() = ScreenName.EventDetails
    override val viewModelClass: KClass<EventDetailViewModel>
        get() = EventDetailViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_event_details

    override val title = R.string.global_empty_string

    override val customToolbarColorResource: Int
        get() = R.color.audioBackground

    private val event by lazy { requireActivity().intent.getParcelableExtra<ArticEvent>(ARG_EVENT) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        image.transitionName = event?.title

        if (BuildConfig.IS_RENTAL) {
            registerToday.visibility = View.GONE
        }
    }

    override fun onRegisterViewModel(viewModel: EventDetailViewModel) {
        viewModel.event = event
    }

    override fun setupBindings(viewModel: EventDetailViewModel) {
        viewModel.title
                .subscribe {
                    expandedTitle.text = it
                    toolbarTitle.text = it
                    /**
                     * When the scrollView is
                     * - scrolled down and expandedTitle is about to go behind the toolbar,
                     *   update the toolbar's title.
                     * - scrolled up and expandedTitle is visible (seen in scrollView) clear
                     *   the toolbar's title.
                     */
                    val toolbarHeight = toolbar?.layoutParams?.height ?: 0
                    scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener
                    { _, _, scrollY, _, _ ->
                        val threshold = image.measuredHeight + toolbarHeight / 2
                        val alpha: Float = (scrollY - threshold + 40f) / 40f
                        toolbarTitle.alpha = MathUtils.clamp(alpha, 0f, 1f)
                        expandedTitle.alpha = 1 - alpha
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
                            imageScaleType = ImageView.ScaleType.MATRIX)

                    GlideApp.with(this)
                            .load(it)
                            .apply(options)
                            .error(R.drawable.placeholder_large)
                            .placeholder(R.color.placeholderBackground)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .listenerAnimateSharedTransaction(this, image, scaleInfo)
                            .into(image)
                }
                .disposedBy(disposeBag)

        viewModel.metaData
                .bindToMain(metaData.text())
                .disposedBy(disposeBag)

        viewModel.description
                .map {
                    it.trimDownBlankLines().fromHtml()
                }
                .bindToMain(description.text())
                .disposedBy(disposeBag)

        viewModel.throughDate
                .map { getString(R.string.content_through_date, it) }
                .bindToMain(throughDate.text())
                .disposedBy(disposeBag)

        viewModel.location
                .bindToMain(location.text())
                .disposedBy(disposeBag)

        if (!BuildConfig.IS_RENTAL) {
            Observables
                    .combineLatest(
                            viewModel.eventButtonText.map { it.isNotEmpty() },
                            viewModel.hasEventUrl
                    )
                    .map { it.first && it.second }
                    .bindToMain(registerToday.visibility())
                    .disposedBy(disposeBag)
        } else {
            Observables
                    .combineLatest(
                            viewModel.eventButtonText.map { it.isNotEmpty() },
                            viewModel.hasEventUrl
                    )
                    .map { it.first && it.second }
                    .bindToMain(visitWebsiteToRegister.visibility())
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
                .bindToMain(registerToday.text())
                .disposedBy(disposeBag)

        registerToday.clicks()
                .subscribe { viewModel.onClickRegisterToday() }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: EventDetailViewModel) {

        viewModel.navigateTo.subscribe {
            when (it) {
                is Navigate.Forward -> {
                    when (it.endpoint) {
                        is EventDetailViewModel.NavigationEndpoint.LoadUrl -> {
                            val endpoint = it.endpoint as EventDetailViewModel.NavigationEndpoint.LoadUrl
                            customTabManager.openUrlOnChromeCustomTab(requireContext(), Uri.parse(endpoint.url))
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
        scrollView?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }

    companion object {
        val ARG_EVENT = "${EventDetailFragment::class.java.simpleName}: event"

        fun argsBundle(event: ArticEvent) = Bundle().apply {
            putParcelable(ARG_EVENT, event)
        }

    }
}