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

    private var audioService: AudioPlayerService? = null

    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val galleryLocation: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<ArticObject> = BehaviorSubject.create()
    val playState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()


    fun setService(service: AudioPlayerService?) {
        audioService = service
        audioService?.audioPlayBackStatus
                ?.filter { playBackState ->
                    playBackState.articAudioFile == articObject?.audioCommentary?.first()?.audioFile
                }
                ?.bindTo(playState)
                ?.disposedBy(disposeBag)
    }


    override fun cleanup() {
        super.cleanup()
        audioService = null
    }

    var articObject: ArticObject? = null
        set(value) {
            field = value
            value?.let {
                objectObservable.onNext(it)
            }
        }

    init {

        objectObservable
                .map {
                    it.title ?: ""
                }.bindTo(title)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.galleryLocation ?: ""
                }.bindTo(galleryLocation)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.largeImageFullPath ?: ""
                }.bindTo(image)
                .disposedBy(disposeBag)
    }

    fun playAudioTrack() {
        audioService?.playPlayer(articObject)
    }

    fun pauseAudioTrack() {
        audioService?.pausePlayer()
    }


}