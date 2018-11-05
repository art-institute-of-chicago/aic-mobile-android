package edu.artic.map

import android.os.Bundle
import android.view.View
import com.fuzz.rx.*
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenName
import edu.artic.db.models.ArticObject
import edu.artic.image.GlideApp
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_map_object_details.*
import kotlin.reflect.KClass

/**
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsFragment : BaseViewModelFragment<MapObjectDetailsViewModel>() {

    enum class Type {
        Map,
        Search,
        Tour
    }

    override val viewModelClass: KClass<MapObjectDetailsViewModel>
        get() = MapObjectDetailsViewModel::class

    override val title = R.string.noTitle

    override val layoutResId: Int
        get() = R.layout.fragment_map_object_details

    override val overrideStatusBarColor: Boolean
        get() = false

    override val screenName: ScreenName
        get() = ScreenName.OnViewDetails

    private val mapObject: ArticObject by lazy { arguments!!.getParcelable<ArticObject>(ARG_MAP_OBJECT) }
    private var audioService: Subject<AudioPlayerService> = BehaviorSubject.create()

    override fun onRegisterViewModel(viewModel: MapObjectDetailsViewModel) {
        viewModel.articObject = mapObject
    }

    override fun setupBindings(viewModel: MapObjectDetailsViewModel) {

        viewModel.title
                .bindToMain(tourStopTitle.text())
                .disposedBy(disposeBag)

        viewModel.galleryNumber
                .observeOn(AndroidSchedulers.mainThread())
                .filter { it.isNotEmpty() }
                .map {
                    resources.getString(R.string.gallery, it)
                }
                .bindToMain(tourStopGallery.text())
                .disposedBy(disposeBag)

        viewModel.image.subscribe {
            GlideApp.with(this)
                    .load(it)
                    .placeholder(R.color.placeholderBackground)
                    .error(R.drawable.placeholder_thumb)
                    .into(image)
        }.disposedBy(disposeBag)


        viewModel.playerControl
                .observeOn(AndroidSchedulers.mainThread())
                .withLatestFrom(audioService) { playerAction, service ->
                    playerAction to service
                }.subscribe { actionWithService ->
                    val playerAction = actionWithService.first
                    val service = actionWithService.second

                    when (playerAction) {
                        is MapObjectDetailsViewModel.PlayerAction.Play -> {
                            service.playPlayer(playerAction.requestedObject)
                        }
                        is MapObjectDetailsViewModel.PlayerAction.Pause -> {
                            service.pausePlayer()
                        }
                    }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getAudioServiceObservable()
                .bindTo(audioService)
                .disposedBy(disposeBag)

        audioService.flatMap { service -> service.audioPlayBackStatus }
                .bindTo(viewModel.audioPlayBackStatus)
                .disposedBy(disposeBag)

        audioService.flatMap { service -> service.currentTrack }
                .mapOptional()
                .bindTo(viewModel.currentTrack)
                .disposedBy(disposeBag)


        playCurrent
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.playCurrentObject()
                }.disposedBy(disposeBag)

        pauseCurrent
                .clicks()
                .defaultThrottle()
                .subscribe {
                    viewModel.pauseCurrentObject()
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