package edu.artic.map.carousel

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.Playable
import edu.artic.db.daos.ArticAudioFileDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.*
import edu.artic.media.audio.AudioPlayerService
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
                                                private val objectDao: ArticObjectDao,
                                                private val audioObjectDao: ArticAudioFileDao,
                                                tourProgressManager: TourProgressManager) : BaseViewModel() {


    val tourObservable: Subject<ArticTour> = BehaviorSubject.create()
    val tourTitle: Subject<String> = BehaviorSubject.create()
    val tourStops: Subject<List<ArticTour.TourStop>> = BehaviorSubject.create()
    val tourStopViewModels: Subject<List<TourCarousalBaseViewModel>> = BehaviorSubject.create()
    val currentTrack: Subject<Optional<AudioFileModel>> = BehaviorSubject.createDefault(Optional(null))
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<TourCarousalBaseViewModel.PlayerAction> = PublishSubject.create()
    val currentPage: Subject<Int> = BehaviorSubject.createDefault(0)

    private val selectedStop: Subject<ArticObject> = BehaviorSubject.create()

    var tourObject: ArticTour? = null
        set(value) {
            field = value
            value?.let {
                tourObservable.onNext(it)
            }
        }

    init {
        tourObservable.map { it.title }
                .bindTo(tourTitle)
                .disposedBy(disposeBag)


        /**
         * Build the list of tour stops including the tour introduction stop.
         */
        tourObservable
                .observeOn(Schedulers.io())
                .map { tour ->
                    val list = mutableListOf<ArticTour.TourStop>()
                    val tourIntro = tour.getIntroStop()
                    list.add(tourIntro)
                    list.addAll(tour.tourStops)
                    list
                }.bindTo(tourStops)
                .disposedBy(disposeBag)


        tourStops
                .withLatestFrom(tourObservable) { tourStops, tour ->
                    tour to tourStops
                }
                .map { tourWithTourStops ->
                    val tour = tourWithTourStops.first
                    val tourStops = tourWithTourStops.second

                    val list = mutableListOf<TourCarousalBaseViewModel>()
                    tourStops.forEach { tourStop ->
                        if (tourStop.objectId === "INTRO") {
                            val element = TourCarousalIntroViewModel(tour, audioObjectDao)

                            element.playerControl
                                    .bindTo(playerControl)
                                    .disposedBy(element.viewDisposeBag)

                            list.add(element)
                        } else {
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

                    }
                    return@map list
                }.bindTo(tourStopViewModels)
                .disposedBy(disposeBag)

        /**
         * Get index of the object using id and bind it to currentPage.
         */
        tourProgressManager
                .selectedStop
                .withLatestFrom(tourStops) { objectId, tourStops ->
                    tourStops.map { it.objectId }.indexOf(objectId)
                }
                .filter { it > -1 }
                .distinctUntilChanged()
                .bindTo(currentPage)
                .disposedBy(disposeBag)

        /**
         * Get the object id from the tour stop and send it to TourProgressManager.
         */
        currentPage
                .withLatestFrom(tourStops) { page, tourStops ->
                    tourStops[page].objectId.orEmpty()
                }
                .distinctUntilChanged()
                .bindTo(tourProgressManager.selectedStop)
                .disposedBy(disposeBag)

    }

}

open class TourCarousalBaseViewModel : BaseViewModel() {
    sealed class PlayerAction {
        class Play(val requestedObject: Playable, val audioFileModel: AudioFileModel? = null) : PlayerAction()
        class Pause : PlayerAction()
    }

    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<PlayerAction> = PublishSubject.create()
    val viewPlayBackState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
}


/**
 * ViewModel for the tour intro layout.
 * Play pause the tour introduction.
 */
class TourCarousalIntroViewModel(val tour: ArticTour,
                                 val audioObjectDao: ArticAudioFileDao) : TourCarousalBaseViewModel() {

    val isPlaying: Subject<Boolean> = BehaviorSubject.createDefault(false)

    fun playTourIntro() {
        tour.tourAudio?.let { audioId ->
            audioObjectDao.getAudioByIdAsync(audioId)
                    .toObservable()
                    .take(1)
                    .map { it.asAudioFileModel() }
                    .subscribe { audioFileModel ->
                        playerControl.onNext(PlayerAction.Play(tour, audioFileModel))
                    }
        }
    }


}


class TourCarousalStopCellViewModel(tourStop: ArticTour.TourStop, objectDao: ArticObjectDao) : TourCarousalBaseViewModel() {
    val currentAudioTrack: Subject<Optional<AudioFileModel>> = BehaviorSubject.createDefault(Optional(null))
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val galleryText: Subject<String> = BehaviorSubject.create()
    val stopNumber: Subject<String> = BehaviorSubject.createDefault("${tourStop.order + 1}.")
    val articObjectObservable = objectDao.getObjectById(tourStop.objectId.toString())


    init {

        articObjectObservable
                .filter { it.thumbnailFullPath != null }
                .map { it.thumbnailFullPath!! }
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        articObjectObservable
                .map { it.title }
                .bindTo(titleText)
                .disposedBy(disposeBag)

        articObjectObservable
                .filter { it.galleryLocation != null }
                .map { it.galleryLocation!! }
                .bindTo(galleryText)

        /**
         * Display play icon if the current stop's audio is already playing
         * current track and request object
         */
        audioPlayBackStatus
                .withLatestFrom(articObjectObservable.toObservable()) { mediaPlayBackState, articObject ->
                    val currentlyPlaying = mediaPlayBackState?.audio?.audioGroupId == articObject.audioFile?.nid
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
                        viewPlayBackState.onNext(AudioPlayerService.PlayBackState.Paused(currentPlayBackState.audio))
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
