package artic.edu.search

import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.ArticObject
import edu.artic.image.listenerAnimateSharedTransaction
import edu.artic.viewmodel.BaseViewModelFragment
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

    private val articObject by lazy { arguments!!.getParcelable<ArticObject>(ARG_OBJECT) }

    override fun hasTransparentStatusBar(): Boolean {
        return true
    }

    override fun onRegisterViewModel(viewModel: SearchAudioDetailViewModel) {
        viewModel.articObject = articObject
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

        showOnMap.clicks()
                .subscribe { viewModel.onClickShowOnMap() }
                .disposedBy(disposeBag)

        playAudio.clicks()
                .subscribe { viewModel.onClickPlayAudio() }
                .disposedBy(disposeBag)
    }

    override fun setupNavigationBindings(viewModel: SearchAudioDetailViewModel) {
        super.setupNavigationBindings(viewModel)
    }


    companion object {
        private val ARG_OBJECT = "${SearchAudioDetailFragment::class.java.simpleName}: object"

        fun argsBundle(event: ArticObject) = Bundle().apply {
            putParcelable(ARG_OBJECT, event)
        }

    }
}