package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.*
import edu.artic.db.daos.ArticGalleryDao
import edu.artic.db.models.ArticObject
import edu.artic.db.models.AudioFileModel
import edu.artic.db.models.audioFile
import edu.artic.localization.LanguageSelector
import edu.artic.map.MapObjectDetailsViewModel.PlayerAction.Pause
import edu.artic.map.MapObjectDetailsViewModel.PlayerAction.Play
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.audio.preferredLanguage
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * Details about a single [ArticObject], usually with a linked audio track.
 *
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsViewModel @Inject constructor(val analyticsTracker: AnalyticsTracker,
                                                    val languageSelector: LanguageSelector,
                                                    val galleryDao: ArticGalleryDao
) : BaseViewModel() {

    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val galleryNumber: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<ArticObject> = BehaviorSubject.create()
    val playState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()

    val currentTrack: Subject<Optional<AudioFileModel>> = BehaviorSubject.createDefault(Optional(null))

    //Use publish subject to ensure no cache of playback events on activity recreation
    val playerControl: Subject<PlayerAction> = PublishSubject.create()


    /**
     * Use one of these to tell the [AudioPlayerService] to [start playing][Play]
     * a new track or to [pause playback][Pause] of the current audio.
     *
     * This works at a different level from
     * [PlayBackAction][edu.artic.media.audio.AudioPlayerService.PlayBackAction] -
     * where that class allows for selection of arbitrary translations and
     * has low-level functionality like seek or stop, this class can only give two
     * very specific commands. See docs on [Play] and [Pause] for details.
     */
    sealed class PlayerAction {
        /**
         * Stop playing the current audio (if any) and start playing
         * [requestedObject]. If [requestedObject] has an associated
         * audioModel (i.e. because it was already registered
         * with the [AudioPlayerService]), please provide that
         * audioModel.
         *
         * If it was not associated with an audioModel,
         * [edu.artic.localization.LanguageSelector] can choose an
         * appropriate value. Use-sites may wish to call
         * [edu.artic.db.models.ArticAudioFile.preferredLanguage]
         * to retrieve that value.
         */
        class Play(val requestedObject: ArticObject, val audioModel: AudioFileModel) : PlayerAction()

        /**
         * Pause playback of the current audio track, if any.
         */
        class Pause : PlayerAction()
    }


    var articObject: ArticObject? = null
        set(value) {
            val isDifferent = (field != value)

            field = value
            value?.let {
                objectObservable.onNext(it)
            }

            if (isDifferent) {
                audioFileModel = value?.audioFile?.preferredLanguage(languageSelector)
            }
        }

    var audioFileModel: AudioFileModel? = null

    init {

        audioPlayBackStatus
                .filter { playBackState ->
                    playBackState.audio == audioFileModel
                }.bindTo(playState)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.title
                }.bindTo(title)
                .disposedBy(disposeBag)


        Observables.combineLatest(
                languageSelector.currentLanguage,
                objectObservable
        ) { _, artwork -> artwork }
                .filterFlatMap({ !it.galleryLocation.isNullOrBlank() }, { it.galleryLocation as String })
                .observeOn(Schedulers.io())
                .flatMap {
                    galleryDao.getGalleryByTitle(it).toObservable()
                }.map { it.number.orEmpty() }
                .bindTo(galleryNumber)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.largeImageUrl.orEmpty()
                }.bindTo(image)
                .disposedBy(disposeBag)

        /**
         * Check and log audio interrupted event if current audio playback is being interrupted.
         * No-op at this time, removed during analytics migration
         */
        playerControl
                .filterFlatMap({ it is PlayerAction.Play }, { it as PlayerAction.Play })
                .withLatestFrom(currentTrack, objectObservable) { playerAction, currentTrack, articObject ->
                    val requested = playerAction.audioModel
                    val isNewTrack = currentTrack.value != requested
                    return@withLatestFrom isNewTrack to articObject
                }
                .filter { (isNewTrack: Boolean, _) -> isNewTrack }
                .subscribe { (_, articObject) ->
                }.disposedBy(disposeBag)

    }


    fun playCurrentObject() {
        articObject?.let { source: ArticObject ->
            audioFileModel?.let { translation ->
                playerControl.onNext(PlayerAction.Play(source, translation))

                var analyticsParamMap: Map<String, String> = mapOf(
                    AnalyticsLabel.playbackSource to ScreenName.Map.screenName,
                    AnalyticsLabel.title to source.title,
                    AnalyticsLabel.tourTitle to source.tourTitles.orEmpty(),
                    AnalyticsLabel.audioTitle to translation.title.orEmpty(),
                    AnalyticsLabel.playbackLanguage to translation.fileLanguageForAnalytics().toString()
                )
                analyticsTracker.reportCustomEvent(EventCategoryName.AudioPlayed, analyticsParamMap)
            }
        }
    }

    fun pauseCurrentObject() {
        playerControl.onNext(PlayerAction.Pause())
    }

}