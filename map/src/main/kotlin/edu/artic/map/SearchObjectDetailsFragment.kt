package edu.artic.map

import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.fuzz.rx.*
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.text
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.ArticObject
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.ui.getAudioServiceObservable
import edu.artic.viewmodel.BaseViewModelFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.fragment_search_object_details.*
import kotlin.reflect.KClass

/**
 * Displays the object details of the searched item.
 * @author Sameer Dhakal (Fuzz)
 */
class SearchObjectDetailsFragment : BaseViewModelFragment<SearchObjectDetailsViewModel>() {

    override val viewModelClass: KClass<SearchObjectDetailsViewModel>
        get() = SearchObjectDetailsViewModel::class

    override val title: String
        get() = "Artworks"

    override val layoutResId: Int
        get() = R.layout.fragment_search_object_details


    override val screenCategory: ScreenCategoryName
        get() = ScreenCategoryName.Search

    private val mapObject: ArticObject by lazy { arguments!!.getParcelable<ArticObject>(ARG_MAP_OBJECT) }
    private var audioService: Subject<AudioPlayerService> = BehaviorSubject.create()

    override fun onRegisterViewModel(viewModel: SearchObjectDetailsViewModel) {
        viewModel.articObject = mapObject
    }

    override fun setupBindings(viewModel: SearchObjectDetailsViewModel) {

        viewModel.title
                .bindToMain(searchObjectTitle.text())
                .disposedBy(disposeBag)

        viewModel.galleryLocation
                .bindToMain(subtitle.text())
                .disposedBy(disposeBag)

        viewModel.image
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Glide.with(this)
                            .load(it)
                            .into(image)
                }.disposedBy(disposeBag)

        close.clicks()
                .defaultThrottle()
                .withLatestFrom(audioService)
                .subscribeBy { (_, service) ->
                    viewModel.leaveSearchMode()
                    service.stopPlayer()
                }.disposedBy(disposeBag)


        viewModel.playerControl
                .observeOn(AndroidSchedulers.mainThread())
                .withLatestFrom(audioService) { playerAction, service ->
                    playerAction to service
                }.subscribe { actionWithService ->
                    val playerAction = actionWithService.first
                    val service = actionWithService.second

                    when (playerAction) {
                        is SearchObjectDetailsViewModel.PlayerAction.Play -> {
                            service.playPlayer(playerAction.requestedObject)
                        }
                        is SearchObjectDetailsViewModel.PlayerAction.Pause -> {
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
        private val ARG_MAP_OBJECT = SearchObjectDetailsFragment::class.java.simpleName

        fun create(articObject: ArticObject): SearchObjectDetailsFragment {
            return SearchObjectDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MAP_OBJECT, articObject)
                }
            }
        }
    }
}