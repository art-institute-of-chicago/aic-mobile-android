package edu.artic.search

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.base.utils.updateDetailTitle
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.image.listenerAnimateSharedTransaction
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_search_audio_detail.*
import kotlin.reflect.KClass

class SearchAudioDetailFragment : BaseViewModelFragment<SearchAudioDetailViewModel>() {
    override val viewModelClass: KClass<SearchAudioDetailViewModel>
        get() = SearchAudioDetailViewModel::class
    override val title: String
        get() = ""
    override val layoutResId: Int
        get() = R.layout.fragment_search_audio_detail
    override val screenCategory: ScreenCategoryName?
        get() = ScreenCategoryName.ArtworkSearchDetails

    private val articObject by lazy { arguments!!.getParcelable<ArticSearchArtworkObject>(ARG_OBJECT) }

    override fun hasTransparentStatusBar(): Boolean {
        return true
    }

    override fun onRegisterViewModel(viewModel: SearchAudioDetailViewModel) {
        viewModel.articObject = articObject
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            appBarLayout.updateDetailTitle(verticalOffset, expandedTitle, toolbarTitle)
        }
    }

    override fun setupBindings(viewModel: SearchAudioDetailViewModel) {
        super.setupBindings(viewModel)

        viewModel.title.subscribe {
            expandedTitle.text = it
            toolbarTitle.text = it
        }.disposedBy(disposeBag)

        val options = RequestOptions()
                .dontAnimate()
                .dontTransform()
                .placeholder(ColorDrawable())
                .error(ColorDrawable())

        viewModel.imageUrl
                .map { it.isNotEmpty() }
                .bindToMain(audioImage.visibility())
                .disposedBy(disposeBag)



        viewModel.imageUrl
                .subscribe {
                    Glide.with(this)
                            .load(it)
                            .apply(options)
                            .listenerAnimateSharedTransaction(this, audioImage)
                            .into(audioImage)

                }.disposedBy(disposeBag)


        viewModel.showOnMapButtonText
                .bindToMain(showOnMap.text())
                .disposedBy(disposeBag)

        viewModel.playAudioButtonText
                .bindToMain(playAudio.text())
                .disposedBy(disposeBag)

        viewModel.authorCulturalPlace
                .bindToMain(artistCulturePlaceDenim.text())
                .disposedBy(disposeBag)

        viewModel.showOnMapVisible
                .bindToMain(showOnMap.visibility())
                .disposedBy(disposeBag)
        viewModel.playAudioVisible
                .bindToMain(playAudio.visibility())
                .disposedBy(disposeBag)

        viewModel.galleryNumber
                .bindTo(galleryNumber.text())
                .disposedBy(disposeBag)

        showOnMap.clicks()
                .subscribe { viewModel.onClickShowOnMap() }
                .disposedBy(disposeBag)

        playAudio.clicks()
                .subscribe { viewModel.onClickPlayAudio() }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: SearchAudioDetailViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filter { it is Navigate.Forward }
                .map { it as Navigate.Forward }
                .subscribe {
                    when (it.endpoint) {
                        is SearchAudioDetailViewModel.NavigationEndpoint.ObjectOnMap -> {
                            val o = (it.endpoint as SearchAudioDetailViewModel.NavigationEndpoint.ObjectOnMap).articObject
                            val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                putExtra(NavigationConstants.ARG_SEARCH_OBJECT, o)
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            //TODO: update map to handle ArticSearchArtworkObject
//                            startActivity(mapIntent)
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }


    companion object {
        private val ARG_OBJECT = "${SearchAudioDetailFragment::class.java.simpleName}: object"

        fun argsBundle(event: ArticSearchArtworkObject) = Bundle().apply {
            putParcelable(ARG_OBJECT, event)
        }

    }
}