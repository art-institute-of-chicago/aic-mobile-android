package edu.artic.audio

import android.support.annotation.AnyThread
import android.support.annotation.UiThread
import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.db.Playable
import edu.artic.db.models.ArticObject
import edu.artic.db.models.AudioFileModel
import edu.artic.db.models.audioFile
import edu.artic.localization.LanguageSelector
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.refreshPlayBackState
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * This is where the [AudioDetailsFragment] gets its information about a specific
 * [ArticObject]. One such object may contain an [ArticAudioFile][edu.artic.db.models.ArticAudioFile],
 * and if it does so we populate fields in this class with that as a primary
 * source.
 *
 * @author Sameer Dhakal (Fuzz)
 * @see AudioFileModel
 */
class AudioDetailsViewModel @Inject constructor(val languageSelector: LanguageSelector) : BaseViewModel() {
    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val availableTranslations: Subject<List<AudioFileModel>> = BehaviorSubject.create()
    val audioTrackToUse: Subject<AudioFileModel> = BehaviorSubject.create()
    val transcript: Subject<String> = BehaviorSubject.create()
    val credits: Subject<String> = BehaviorSubject.create()
    val authorCulturalPlace: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<Playable> = BehaviorSubject.create()

    /**
     * [Disposable] representing the stream of events from [AudioPlayerService.currentTrack]
     * to [audioTrackToUse].
     *
     * Only to be touched by [disposeBag], [onServiceConnected] and [onServiceDisconnected].
     */
    private var trackDisposable: Disposable? = null


    var playable: Playable? = null
        set(value) {
            field = value
            value?.let {
                objectObservable.onNext(it)
            }
        }

    init {

        // Just bind all the properties that aren't specific to the audioModel so
        // that we don't need to worry about them later

        objectObservable
                .filterFlatMap({ it is ArticObject }, { it as ArticObject })
                .map {
                    it.artistCulturePlaceDelim?.replace("\r", "\n").orEmpty()
                }.bindTo(authorCulturalPlace)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.getPlayableThumbnailUrl().orEmpty()
                }.bindTo(image)
                .disposedBy(disposeBag)

        // Note there is a 'credits' property on each AudioFileModel. We do not want that here.
        objectObservable
                .filterFlatMap({ it is ArticObject }, { it as ArticObject })
                .map {
                    // This is a credit line for the object, _not_ translated on a per-language basis
                    it.creditLine.orEmpty()
                }.bindTo(credits)
                .disposedBy(disposeBag)


        // Retrieve a list of all translations we have available for this object
        objectObservable
                .filterFlatMap({ it is ArticObject }, { it as ArticObject })
                .map {
                    it.audioFile?.allTranslations().orEmpty()
                }
                .bindTo(availableTranslations)
                .disposedBy(disposeBag)


        // Lastly, we need to attach the translatable audio properties. These come from 'audioTrackToUse'.

        audioTrackToUse
                .map {
                    // TODO: default to playable.title, perhaps?
                    it.title.orEmpty()
                }.bindTo(title)
                .disposedBy(disposeBag)
        audioTrackToUse
                .map {
                    it.transcript.orEmpty()
                }.bindTo(transcript)
                .disposedBy(disposeBag)


    }

    /**
     * Call this whenever the [AudioDetailsFragment] connects to an [AudioPlayerService].
     *
     * We define [playable] and [audioTrackToUse] here.
     */
    @UiThread
    fun onServiceConnected(service: AudioPlayerService) {
        playable = service.playable
        service.player.refreshPlayBackState()

        // Register for updates
        trackDisposable = service.currentTrack
                .subscribeBy { translation ->
                    audioTrackToUse.onNext(translation)
                }.disposedBy(disposeBag)

        if (service.hasNoTrack()) {
            // Set up the default language selection.
            chooseDefaultLanguage()
        }
    }

    /**
     * Call this whenever the [AudioDetailsFragment] disconnects from its [AudioPlayerService].
     *
     * We won't see any further events to [trackDisposable] (unless the service is re-connected),
     * so it's safe to dispose it now.
     */
    @AnyThread
    fun onServiceDisconnected() {
        playable = null
        trackDisposable?.dispose()
    }


    /**
     * This override lasts solely for the current object audio; it
     * does not transfer to other objects in the tour or persist
     * into app- or system-settings.
     *
     * The caller is responsible for sending `audioModel` to an
     * [AudioPlayerService] (if one is available).
     */
    fun setTranslationOverride(audioModel: AudioFileModel) {
        audioTrackToUse.onNext(audioModel)
    }

    /**
     * Only call this if the [edu.artic.media.audio.AudioPlayerService] itself is not
     * defining the language for us.
     */
    private fun chooseDefaultLanguage() {
        availableTranslations
                .map {
                    // TODO: inject 'edu.artic.map.carousel.TourProgressManager', use that state as boolean
                    languageSelector.selectFrom(it, true)
                }.bindTo(audioTrackToUse)
                .disposedBy(disposeBag)
    }

}