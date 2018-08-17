package edu.artic.tours.carousel

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.ArticTourDao
import edu.artic.db.models.ArticAudioFile
import edu.artic.db.models.ArticObject
import edu.artic.db.models.ArticTour
import edu.artic.db.models.audioFile
import edu.artic.media.audio.AudioPlayerService
import edu.artic.tours.TourDetailsStopCellViewModel
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class TourCarouselViewModel @Inject constructor(private val analyticsTracker: AnalyticsTracker,
                                                private val articTourDao: ArticTourDao,
                                                private val objectDao: ArticObjectDao) : BaseViewModel() {


    val tourObservable: Subject<ArticTour> = BehaviorSubject.create()
    val tourTitle: Subject<String> = BehaviorSubject.create()
    val stops: Subject<List<TourCarousalStopCellViewModel>> = BehaviorSubject.create()
    val currentTrack: Subject<Optional<ArticAudioFile>> = BehaviorSubject.createDefault(Optional(null))
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<TourCarousalStopCellViewModel.PlayerAction> = PublishSubject.create()

    init {

        articTourDao.getAsyncFirstTour()
                .bindTo(tourObservable)
                .disposedBy(disposeBag)

        tourObservable.map { it.title }
                .bindTo(tourTitle)
                .disposedBy(disposeBag)

        tourObservable
                .map { it.tourStops }
                .observeOn(Schedulers.io())
                .map {
                    val list = mutableListOf<TourCarousalStopCellViewModel>()
                    it.forEach { tourStop ->

                        val element = TourCarousalStopCellViewModel(tourStop, objectDao)

                        element.playerControl
                                .bindTo(playerControl)
                                .disposedBy(element.viewDisposeBag)

                        audioPlayBackStatus
                                .bindTo(element.audioPlayBackStatus)
                                .disposedBy(disposeBag)

                        currentTrack
                                .bindTo(element.currentAudioTrack)
                                .disposedBy(disposeBag)

                        list.add(element)
                    }
                    return@map list
                }.bindTo(stops)
                .disposedBy(disposeBag)

    }

}


class TourCarousalStopCellViewModel(tourStop: ArticTour.TourStop, objectDao: ArticObjectDao) : TourDetailsStopCellViewModel(tourStop, objectDao) {

    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<PlayerAction> = PublishSubject.create()

    val viewPlayBackState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val currentAudioTrack: Subject<Optional<ArticAudioFile>> = BehaviorSubject.createDefault(Optional(null))

    sealed class PlayerAction {
        class Play(val requestedObject: ArticObject) : PlayerAction()
        class Pause : PlayerAction()
    }

    init {

        /**
         * Display play icon if the current stop's audio is already playing
         * current track and request object
         */
        audioPlayBackStatus
                .withLatestFrom(articObjectObservable.toObservable()) { mediaPlayBackState, articObject ->
                    val currentlyPlaying = mediaPlayBackState?.articAudioFile == articObject.audioFile
                    mediaPlayBackState to currentlyPlaying
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {

                    val currentPlayBackState = it.first
                    val isCurrentTrack = it.second

                    if (isCurrentTrack) {
                        viewPlayBackState.onNext(currentPlayBackState)
                    } else {
                        /**
                         * Reset the view state when
                         */
                        viewPlayBackState.onNext(AudioPlayerService.PlayBackState.Paused(currentPlayBackState.articAudioFile))
                    }

                }
                .disposedBy(disposeBag)

    }

    /**
     * Bind play/pause button action to playerControl which is then used by
     * observer (i.e. fragment) to communicate to audio service.
     */
    fun playCurrentObject() {
        articObjectObservable
                .toObservable()
                .take(1)
                .subscribe { articObject ->
                    playerControl.onNext(PlayerAction.Play(articObject))
                }.disposedBy(disposeBag)
    }

    fun pauseCurrentObject() {
        playerControl.onNext(PlayerAction.Pause())
    }

}
