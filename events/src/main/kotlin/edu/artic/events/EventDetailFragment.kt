package edu.artic.events

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.fromHtml
import edu.artic.base.utils.listenerAnimateSharedTransaction
import edu.artic.base.utils.updateDetailTitle
import edu.artic.db.models.ArticEvent
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_event_details.*
import kotlin.reflect.KClass


class EventDetailFragment : BaseViewModelFragment<EventDetailViewModel>() {
    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.EventDetails
    override val viewModelClass: KClass<EventDetailViewModel>
        get() = EventDetailViewModel::class
    override val layoutResId: Int
        get() = R.layout.fragment_event_details
    override val title: String
        get() = ""

    private val event by lazy { arguments!!.getParcelable<ArticEvent>(ARG_EVENT) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }
        eventImage.transitionName = event.title
    }

    override fun onRegisterViewModel(viewModel: EventDetailViewModel) {
        viewModel.event = event
    }

    override fun setupBindings(viewModel: EventDetailViewModel) {
        viewModel.title
                .subscribe {
                    expandedTitle.text = it
                    toolbarTitle.text = it
                }
                .disposedBy(disposeBag)

        viewModel.imageUrl
                .subscribe {
                    val options = RequestOptions()
                            .dontAnimate()
                            .dontTransform()
                    Glide.with(this)
                            .load(it)
                            .apply(options)
                            .listenerAnimateSharedTransaction(this, eventImage)
                            .into(eventImage)
                }
                .disposedBy(disposeBag)

        viewModel.metaData
                .bindToMain(metaData.text())
                .disposedBy(disposeBag)

        viewModel.description
                .map {
                    it.fromHtml()
                }
                .bindToMain(description.text())
                .disposedBy(disposeBag)

        viewModel.throughDate
                .bindToMain(throughDate.text())
                .disposedBy(disposeBag)

        viewModel.location
                .bindToMain(location.text())
                .disposedBy(disposeBag)

        viewModel.registerTodayText
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
                        is EventDetailViewModel.NavigationEndpoint.RegisterToday -> {
                            val endpoint = it.endpoint as EventDetailViewModel.NavigationEndpoint.RegisterToday
                            var url = endpoint.url
                            if (!url.startsWith("http://") && !url.startsWith("https://"))
                                url = "https://$url"
                            val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(myIntent)
                        }
                    }
                }
                is Navigate.Back -> {

                }
            }
        }.disposedBy(disposeBag)
    }

    companion object {
        private val ARG_EVENT = "${EventDetailFragment::class.java.simpleName}: event"

        fun argBundle(event: ArticEvent) = Bundle().apply {
            putParcelable(ARG_EVENT, event)
        }

    }
}