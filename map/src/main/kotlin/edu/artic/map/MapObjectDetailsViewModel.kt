package edu.artic.map

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.models.ArticObject
import edu.artic.media.audio.AudioPlayerService
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * @author Sameer Dhakal (Fuzz)
 */
class MapObjectDetailsViewModel @Inject constructor() : BaseViewModel() {

    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val galleryLocation: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<ArticObject> = BehaviorSubject.create()
    val playState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()


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
                    playBackState.articAudioFile == articObject?.audioCommentary?.first()?.audioFile
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
    }

}