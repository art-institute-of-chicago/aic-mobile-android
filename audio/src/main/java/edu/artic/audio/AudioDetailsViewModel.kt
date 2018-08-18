package edu.artic.audio

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import edu.artic.db.models.ArticObject
import edu.artic.db.models.AudioTranslation
import edu.artic.db.models.audioFile
import edu.artic.localization.BaseTranslation
import edu.artic.localization.LanguageSelector
import edu.artic.viewmodel.BaseViewModel
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
 * @see AudioTranslation
 */
class AudioDetailsViewModel @Inject constructor() : BaseViewModel() {
    val title: Subject<String> = BehaviorSubject.create()
    val image: Subject<String> = BehaviorSubject.create()
    val availableTranslations: Subject<List<AudioTranslation>> = BehaviorSubject.create()
    val chosenTranslation: Subject<BaseTranslation> = BehaviorSubject.create()
    val transcript: Subject<String> = BehaviorSubject.create()
    val credits: Subject<String> = BehaviorSubject.create()
    val authorCulturalPlace: Subject<String> = BehaviorSubject.create()

    private val objectObservable: Subject<ArticObject> = BehaviorSubject.create()

    @Inject
    lateinit var languageSelector: LanguageSelector

    var audioObject: ArticObject? = null
        set(value) {
            field = value
            value?.let {
                objectObservable.onNext(it)
            }
        }

    init {
        objectObservable
                .map {
                    it.artistCulturePlaceDelim?.replace("\r", "\n").orEmpty()
                }.bindTo(authorCulturalPlace)
                .disposedBy(disposeBag)

        objectObservable
                .map {
                    it.title
                }.bindTo(title)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.largeImageFullPath.orEmpty()
                }.bindTo(image)
                .disposedBy(disposeBag)

        val known = objectObservable
                .map {
                    it.audioFile?.allTranslations().orEmpty()
                }.share()

        known.bindTo(availableTranslations).disposedBy(disposeBag)

        // Set up the default language selection.
        known.map {
            // TODO: we'll need to modify this to account for the current Tour language
                    languageSelector.selectFrom(it)
                }.bindTo(chosenTranslation)
                .disposedBy(disposeBag)


        objectObservable
                .map {
                    it.audioFile?.transcript.orEmpty()
                }.bindTo(transcript)
                .disposedBy(disposeBag)

        objectObservable
                .map {
                    it.creditLine.orEmpty()
                }.bindTo(credits)
                .disposedBy(disposeBag)


    }

    /**
     * This override lasts solely for the current object audio; it
     * does not transfer to other objects in the tour or persist
     * into app- or system-settings.
     */
    fun setLanguageOverride(translation: AudioTranslation) {
        chosenTranslation.onNext(translation)
    }

}