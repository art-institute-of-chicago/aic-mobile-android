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
import edu.artic.db.models.getAudio
import edu.artic.media.audio.AudioPlayerService
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
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

    sealed class PlayerAction {
        class Play(val requestedObject: ArticObject) : PlayerAction()
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
                    playBackState.articAudioFile == articObject?.getAudio()
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
                    val requestedObject = playerAction.requestedObject.getAudio()
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