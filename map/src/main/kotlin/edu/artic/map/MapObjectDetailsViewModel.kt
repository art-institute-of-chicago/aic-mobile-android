package edu.artic.map

import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.models.ArticAudioFile
import edu.artic.db.models.ArticObject
import edu.artic.db.models.audioFile
import edu.artic.media.audio.AudioPlayerService
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * Details about a single [ArticObject], usually with a linked audio track.
 *
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsViewModel @Inject constructor(val analyticsTracker: AnalyticsTracker) : BaseViewModel() {

    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val galleryLocation: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<ArticObject> = BehaviorSubject.create()
    val playState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()

    val currentTrack: Subject<Optional<ArticAudioFile>> = BehaviorSubject.createDefault(Optional(null))
    val playerControl: Subject<PlayerAction> = BehaviorSubject.create()

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
         * translation (i.e. because it was already registered
         * with the [AudioPlayerService]), then use that translation.
         *
         * If it was not associated with a translation,
         * [edu.artic.localization.LanguageSelector] will choose an
         * appropriate value.
         */
        class Play(val requestedObject: ArticObject) : PlayerAction()

        /**
         * Pause playback of the current audio track, if any.
         */
        class Pause : PlayerAction()
    }


    var articObject: ArticObject? = null
        set(value) {
            field = value
            value?.let {
                objectObservable.onNext(it)
            }
        }

    init {

        audioPlayBackStatus
                .filter { playBackState ->
                    playBackState.articAudioFile == articObject?.audioFile
                }.bindTo(playState)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.title
                }.bindTo(title)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.galleryLocation.orEmpty()
                }.bindTo(galleryLocation)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.largeImageFullPath.orEmpty()
                }.bindTo(image)
                .disposedBy(disposeBag)


        playerControl
                .filterFlatMap({ it is PlayerAction.Play }, { it as PlayerAction.Play })
                .withLatestFrom(currentTrack, objectObservable) { playerAction, currentTrack, articObject ->
                    val requestedObject = playerAction.requestedObject.audioFile
                    (currentTrack.value != requestedObject) to articObject
                }
                .filter { (newTrack, _) -> newTrack }
                .subscribe { (_, articObject) ->
                    analyticsTracker.reportEvent(EventCategoryName.PlayAudio, AnalyticsAction.playAudioMap, articObject.title)
                }.disposedBy(disposeBag)

    }


    fun playCurrentObject() {
        articObject?.let {
            playerControl.onNext(PlayerAction.Play(it))
        }
    }

    fun pauseCurrentObject() {
        playerControl.onNext(PlayerAction.Pause())
    }

}