package edu.artic.artwork

import android.content.Intent
import android.os.Bundle
import android.support.v4.math.MathUtils
import android.support.v4.widget.NestedScrollView
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.base.utils.asDeepLinkIntent
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.details.R
import edu.artic.image.GlideApp
import edu.artic.image.listenerAnimateSharedTransaction
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.navigation.NavigationConstants
import edu.artic.viewmodel.BaseViewModelFragment
import edu.artic.viewmodel.Navigate
import kotlinx.android.synthetic.main.fragment_search_audio_detail.*
import kotlin.reflect.KClass

class ArtworkDetailFragment : BaseViewModelFragment<ArtworkDetailViewModel>() {
    override val viewModelClass: KClass<ArtworkDetailViewModel>
        get() = ArtworkDetailViewModel::class
    override val title = R.string.noTitle
    override val layoutResId: Int
        get() = R.layout.fragment_search_audio_detail
    override val screenCategory: ScreenCategoryName?
        get() = ScreenCategoryName.ArtworkSearchDetails

    private val articObject by lazy { arguments!!.getParcelable<ArticSearchArtworkObject>(ARG_OBJECT) }
    private val searchTerm by lazy { arguments!!.getString(ARG_SEARCH_TERM) }

    override val customToolbarColorResource: Int
        get() = R.color.audioBackground

    override fun onRegisterViewModel(viewModel: ArtworkDetailViewModel) {
        viewModel.articObject = articObject
        viewModel.searchTerm = searchTerm
    }

    override fun setupBindings(viewModel: ArtworkDetailViewModel) {
        super.setupBindings(viewModel)

        this.getAudioServiceObservable()
                .subscribe { viewModel.playerService = it }
                .disposedBy(disposeBag)

        viewModel.title.subscribe {
            expandedTitle.text = it
            toolbarTitle.text = it

            val toolbarHeight = toolbar?.layoutParams?.height ?: 0
            scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener
            { _, _, scrollY, _, _ ->
                val threshold = image.measuredHeight + toolbarHeight / 2
                val alpha: Float = (scrollY - threshold + 40f) / 40f
                toolbarTitle.alpha = MathUtils.clamp(alpha, 0f, 1f)
                expandedTitle.alpha = 1 - alpha
            })
        }.disposedBy(disposeBag)

        val options = RequestOptions()
                .dontAnimate()
                .dontTransform()

        viewModel.imageUrl
                .subscribe {
                    /**
                     * Please be aware that the "options" defined above will impact the way Glide
                     * operates.
                     */
                    GlideApp.with(this)
                            .load(it)
                            .apply(options)
                            .placeholder(R.drawable.placeholder_large)
                            //.transition(DrawableTransitionOptions.withCrossFade()) removing cross fade related to https://github.com/bumptech/glide/issues/363
                            .listenerAnimateSharedTransaction(this, image)
                            .into(image)

                }.disposedBy(disposeBag)


        viewModel.showOnMapButtonText
                .bindToMain(showOnMap.text())
                .disposedBy(disposeBag)

        viewModel.playAudioButtonText
                .bindToMain(playAudio.text())
                .disposedBy(disposeBag)

        viewModel.authorCulturalPlace
                .bindToMain(description.text())
                .disposedBy(disposeBag)

        viewModel.showOnMapVisible
                .bindToMain(showOnMap.visibility())
                .disposedBy(disposeBag)

        viewModel.playAudioVisible
                .bindToMain(playAudio.visibility(View.INVISIBLE))
                .disposedBy(disposeBag)

        viewModel.galleryNumber
                .subscribe {
                    galleryNumber.text = getString(R.string.gallery, it)
                }
                .disposedBy(disposeBag)

        showOnMap.clicks()
                .subscribe { viewModel.onClickShowOnMap() }
                .disposedBy(disposeBag)

        playAudio.clicks()
                .subscribe { viewModel.onClickPlayAudio() }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: ArtworkDetailViewModel) {
        super.setupNavigationBindings(viewModel)
        viewModel.navigateTo
                .filter { it is Navigate.Forward }
                .map { it as Navigate.Forward }
                .subscribe {
                    when (it.endpoint) {
                        is ArtworkDetailViewModel.NavigationEndpoint.ObjectOnMap -> {
                            val o: ArticSearchArtworkObject = (it.endpoint as ArtworkDetailViewModel.NavigationEndpoint.ObjectOnMap).articObject
                            val mapIntent = NavigationConstants.MAP.asDeepLinkIntent().apply {
                                putExtra(NavigationConstants.ARG_SEARCH_OBJECT, o)
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            }
                            startActivity(mapIntent)
                            if (requireActivity().javaClass.simpleName.equals("SearchActivity")) {
                                requireActivity().finish()
                            }
                        }
                    }
                }.disposedBy(navigationDisposeBag)
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollView?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }


    companion object {
        private val ARG_OBJECT = "${ArtworkDetailFragment::class.java.simpleName}: object"
        private val ARG_SEARCH_TERM = "${ArtworkDetailFragment::class.java.simpleName}: search_term"

        fun argsBundle(event: ArticSearchArtworkObject, term: String) = Bundle().apply {
            putParcelable(ARG_OBJECT, event)
            putString(ARG_SEARCH_TERM, term)
        }

    }
}