package edu.artic.audio

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
class AudioDetailsViewModel @Inject constructor() : BaseViewModel() {
    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val transcript: Subject<String> = BehaviorSubject.create()
    val credits: Subject<String> = BehaviorSubject.create()
    val authorCulturalPlace: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<ArticObject> = BehaviorSubject.create()

    var audioObject: ArticObject? = null
        set(value) {
            field = value
            value?.let {
                objectObservable.onNext(it)
            }
        }

    private var audioService: AudioPlayerService? = null

    fun setService(service: AudioPlayerService?) {
        audioService = service
        audioObject = audioService?.articObject
    }

    override fun onCleared() {
        super.onCleared()
        audioService = null
    }

    init {
        objectObservable
                .map {
                    it.artistCulturePlaceDelim?.replace("\r", "\n") ?: ""
                }.bindTo(authorCulturalPlace)
                .disposedBy(disposeBag)

        objectObservable
                .map {
                    it.title ?: ""
                }.bindTo(title)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.largeImageFullPath ?: ""
                }.bindTo(image)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.audioCommentary.first().audioFile?.transcript ?: ""
                }.bindTo(transcript)
                .disposedBy(disposeBag)

        objectObservable
                .map {
                    it.audioCommentary.first().audioFile?.credits ?: ""
                }.bindTo(credits)
                .disposedBy(disposeBag)


    }

}