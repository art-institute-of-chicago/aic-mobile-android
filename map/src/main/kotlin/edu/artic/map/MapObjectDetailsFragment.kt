package edu.artic.map

import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.bindToMain
import com.fuzz.rx.defaultThrottle
import com.fuzz.rx.disposedBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.ArticObject
import edu.artic.media.audio.AudioPlayerService
import edu.artic.viewmodel.BaseViewModelFragment
import kotlinx.android.synthetic.main.fragment_map_object_details.*
import kotlin.reflect.KClass

/**
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsFragment : BaseViewModelFragment<MapObjectDetailsViewModel>() {

    override val viewModelClass: KClass<MapObjectDetailsViewModel>
        get() = MapObjectDetailsViewModel::class

    override val title: String
        get() = ""

    override val layoutResId: Int
        get() = R.layout.fragment_map_object_details

    //TODO figure out correct analytics category
    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.OnViewDetails

    private val mapObject: ArticObject? by lazy { arguments?.getParcelable<ArticObject>(ARG_MAP_OBJECT) }

    override fun onRegisterViewModel(viewModel: MapObjectDetailsViewModel) {
        viewModel.articObject = mapObject
    }

    override fun setupBindings(viewModel: MapObjectDetailsViewModel) {

        viewModel.title
                .bindToMain(tourStopTitle.text())
                .disposedBy(disposeBag)

        viewModel.galleryLocation
                .bindToMain(tourStopGallery.text())
                .disposedBy(disposeBag)

        viewModel.image.subscribe {
            Glide.with(this)
                    .load(it)
                    .into(image)
        }.disposedBy(disposeBag)


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapActivity = context as MapActivity
        val boundService = mapActivity.boundService
        viewModel.setService(boundService)

        playCurrent
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.playAudioTrack()
                }.disposedBy(disposeBag)

        pauseCurrent
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.pauseAudioTrack()
                }.disposedBy(disposeBag)

        viewModel.playState.subscribe { playBackState ->
            when (playBackState) {
                is AudioPlayerService.PlayBackState.Playing -> {
                    displayPause()
                }
                is AudioPlayerService.PlayBackState.Paused -> {
                    displayPlayButton()
                }
                is AudioPlayerService.PlayBackState.Stopped -> {
                    displayPlayButton()
                }
            }
        }.disposedBy(disposeBag)

    }

    private fun displayPause() {
        playCurrent.visibility = View.INVISIBLE
        pauseCurrent.visibility = View.VISIBLE

    }

    private fun displayPlayButton() {
        playCurrent.visibility = View.VISIBLE
        pauseCurrent.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposeBag.clear()
    }

    companion object {
        private val ARG_MAP_OBJECT = MapObjectDetailsFragment::class.java.simpleName

        fun create(articObject: ArticObject): MapObjectDetailsFragment {
            return MapObjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MAP_OBJECT, articObject)
                }
            }
        }
    }
}