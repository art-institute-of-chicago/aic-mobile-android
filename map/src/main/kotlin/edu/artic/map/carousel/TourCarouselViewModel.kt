package edu.artic.map.carousel

import com.fuzz.rx.DisposeBag
import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsTracker
import edu.artic.db.Playable
import edu.artic.db.daos.ArticAudioFileDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.hasObjectWithId
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.map.BuildConfig
import edu.artic.media.audio.AudioPlayerService
import edu.artic.tours.manager.TourProgressManager
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.CellViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class TourCarouselViewModel @Inject constructor(private val languageSelector: LanguageSelector,
                                                private val analyticsTracker: AnalyticsTracker,
                                                private val objectDao: ArticObjectDao,
                                                private val audioObjectDao: ArticAudioFileDao,
                                                val tourProgressManager: TourProgressManager) : BaseViewModel() {


    val tourObservable: Subject<ArticTour> = BehaviorSubject.create()
    val tourTitle: Subject<String> = BehaviorSubject.create()
    val tourStops: Subject<List<ArticTour.TourStop>> = BehaviorSubject.create()
    val tourStopViewModels: Subject<List<TourCarousalBaseViewModel>> = BehaviorSubject.create()
    val currentTrack: Subject<Optional<AudioFileModel>> = BehaviorSubject.createDefault(Optional(null))
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<TourCarousalBaseViewModel.PlayerAction> = PublishSubject.create()
    val currentPage: Subject<Int> = BehaviorSubject.create()
    private val chosenTranslation: Subject<ArticTour.Translation> = BehaviorSubject.create()
    private val selectedStop: Subject<ArticObject> = BehaviorSubject.create()

    var tourObject: ArticTour? = null
        set(value) {
            field = value
            value?.let {
                tourObservable.onNext(it)
            }
        }

    init {
        tourObservable
                .map { tour -> tour.allTranslations }
                .map {
                    languageSelector.selectFrom(it, true)
                }.bindTo(chosenTranslation)
                .disposedBy(disposeBag)

        chosenTranslation.map { it.title.orEmpty() }
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
                            val element = TourCarousalIntroViewModel(
                                    viewDisposeBag,
                                    languageSelector,
                                    tour,
                                    audioObjectDao
                            )

                            element.playerControl
                                    .bindTo(playerControl)
                                    .disposedBy(element.viewDisposeBag)

                            list.add(element)
                        } else if (objectDao.hasObjectWithId(tourStop.objectId)) {

                            val element = TourCarousalStopCellViewModel(
                                    viewDisposeBag,
                                    tourStop,
                                    objectDao,
                                    languageSelector
                            )

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
                        } else {
                            // Objects satisfying this condition should be filtered out at data-retrieval time (c.f. AppDataManager)
                            if (BuildConfig.DEBUG) {
                                Timber.i("Tour stop ${tourStop.objectId} not available at this time.")
                            }
                        }

                    }
                    return@map list
                }.bindTo(tourStopViewModels)
                .disposedBy(disposeBag)

        /**
         * Get index of the object using id and bind it to currentPage.
         */
        Observables.combineLatest(tourProgressManager.selectedStop, tourStops)
        { objectId, tourStops ->
            tourStops.indexOfFirst { it.objectId == objectId }
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

    fun leaveTour() {
        tourProgressManager.leaveTourRequest.onNext(true)
    }
}

open class TourCarousalBaseViewModel(
        adapterDisposeBag: DisposeBag
) : CellViewModel(adapterDisposeBag) {
    enum class Type {
        Stop,
        Overview
    }

    sealed class PlayerAction {
        class Play(val requestedObject: Playable, val audioFileModel: AudioFileModel? = null, val type: Type) : PlayerAction()
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
class TourCarousalIntroViewModel(
        adapterDisposeBag: DisposeBag,
        val languageSelector: LanguageSelector,
        val tour: ArticTour,
        val audioObjectDao: ArticAudioFileDao
) : TourCarousalBaseViewModel(adapterDisposeBag) {

    val isPlaying: Subject<Boolean> = BehaviorSubject.createDefault(false)

    fun playTourIntro() {
        tour.tourAudio?.let { audioId ->
            audioObjectDao.getAudioByIdAsync(audioId)
                    .toObservable()
                    .take(1)
                    .subscribe { audioFileModel ->
                        val fileMode = languageSelector.selectFrom(audioFileModel.allTranslations(), true)
                        playerControl.onNext(PlayerAction.Play(tour, fileMode, Type.Overview))
                    }
        }
    }


}


class TourCarousalStopCellViewModel(
        adapterDisposeBag: DisposeBag,
        tourStop: ArticTour.TourStop,
        objectDao: ArticObjectDao,
        val languageSelector: LanguageSelector
) : TourCarousalBaseViewModel(adapterDisposeBag) {
    val currentAudioTrack: Subject<Optional<AudioFileModel>> = BehaviorSubject.createDefault(Optional(null))
    val imageUrl: Subject<String> = BehaviorSubject.create()
    val titleText: Subject<String> = BehaviorSubject.create()
    val galleryText: Subject<String> = BehaviorSubject.create()
    val articObjectObservable = objectDao.getObjectById(tourStop.objectId.toString())


    init {

        articObjectObservable
                .filter { it.thumbUrl != null }
                .map { it.thumbUrl!! }
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
                .disposedBy(disposeBag)

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
                    articObject.audioFile?.let {
                        val fileMode = languageSelector.selectFrom(it.allTranslations(), true)
                        playerControl.onNext(PlayerAction.Play(articObject, fileMode, Type.Stop))
                    }

                }.disposedBy(disposeBag)
    }

    fun pauseCurrentObject() {
        playerControl.onNext(PlayerAction.Pause())
    }

}
