package edu.artic.map.carousel

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.AnalyticsTracker
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
                                                tourProgressManager: TourProgressManager) : BaseViewModel() {


    val tourObservable: Subject<ArticTour> = BehaviorSubject.create()
    val tourTitle: Subject<String> = BehaviorSubject.create()
    val tourStops: Subject<List<ArticTour.TourStop>> = BehaviorSubject.create()
    val tourStopViewModels: Subject<List<TourCarousalBaseViewModel>> = BehaviorSubject.create()
    val currentTrack: Subject<Optional<ArticAudioFile>> = BehaviorSubject.createDefault(Optional(null))
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<TourCarousalStopCellViewModel.PlayerAction> = PublishSubject.create()
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
                .map { tourStops ->
                    val list = mutableListOf<TourCarousalBaseViewModel>()
                    tourStops.forEach { tourStop ->
                        if (tourStop.objectId === "INTRO") {
                            list.add(TourCarousalIntroViewModel(tourStop))
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

open class TourCarousalBaseViewModel : BaseViewModel()


/**
 * ViewModel for the tour intro layout.
 * Play pause the tour introduction.
 */
class TourCarousalIntroViewModel(tourStop: ArticTour.TourStop) : TourCarousalBaseViewModel() {

    //current track status
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val isPlaying: Subject<Boolean> = BehaviorSubject.createDefault(false)

    init {

        /**
         * check if the current audio translation's id same as this one.
         * if the current tracks audio id and the intro audio io are same display pause otherwise pause
         */
        audioPlayBackStatus.filterFlatMap({ it is AudioPlayerService.PlayBackState.Playing }, { it as AudioPlayerService.PlayBackState.Playing })
                .map { it.articAudioFile.nid == tourStop.audioId }
                .bindTo(isPlaying)
                .disposedBy(disposeBag)

    }


}


class TourCarousalStopCellViewModel(tourStop: ArticTour.TourStop, objectDao: ArticObjectDao) : TourCarousalBaseViewModel() {

    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<PlayerAction> = PublishSubject.create()

    val viewPlayBackState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val currentAudioTrack: Subject<Optional<ArticAudioFile>> = BehaviorSubject.createDefault(Optional(null))
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val galleryText: Subject<String> = BehaviorSubject.create()
    val stopNumber: Subject<String> = BehaviorSubject.createDefault("${tourStop.order + 1}.")

    val articObjectObservable = objectDao.getObjectById(tourStop.objectId.toString())

    sealed class PlayerAction {
        class Play(val requestedObject: ArticObject) : PlayerAction()
        class Pause : PlayerAction()
    }

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
