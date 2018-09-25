package edu.artic.map;

import com.fuzz.rx.*
import edu.artic.db.Playable
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.audio.preferredLanguage
import edu.artic.viewmodel.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class SearchObjectDetailsViewModel @Inject constructor(val languageSelector: LanguageSelector,
                                                       val articMapAnnotationDao: ArticMapAnnotationDao,
                                                       val searchManager: SearchManager) : BaseViewModel() {

    val searchedObjectViewModels: Subject<List<SearchObjectBaseViewModel>> = BehaviorSubject.create()
    val currentTrack: Subject<Optional<AudioFileModel>> = BehaviorSubject.createDefault(Optional(null))
    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<SearchObjectBaseViewModel.PlayerAction> = PublishSubject.create()
    val leftSearchMode: Subject<Boolean> = PublishSubject.create()
    val currentPage: Subject<Int> = BehaviorSubject.create()

    fun leaveSearchMode() {
        searchManager.clearSearch()
    }

    init {
        searchManager.leaveSearchMode
                .bindTo(leftSearchMode)
                .disposedBy(disposeBag)

        currentPage
                .withLatestFrom(searchedObjectViewModels) { page, pages ->
                    pages[page]
                }.filterFlatMap({ it is DiningAnnotationViewModel }, { it as DiningAnnotationViewModel })
                .distinctUntilChanged()
                .map { it.item }
                .mapOptional()
                .bindTo(searchManager.activeDiningPlace)
                .disposedBy(disposeBag)
    }

    /**
     * when the search type is amenities fetch all the amenities and create viewmodels
     */
    fun viewResumed(requestedSearchObject: ArticSearchArtworkObject?, requestedSearchAmenityType: String?) {

        /**
         * latestTourObject and amenityType are mutually exclusive
         */
        if (requestedSearchObject != null) {
            /**
             * Update carousel to display objects.
             */
            Observable.just(requestedSearchObject)
                    .map { artwork ->
                        val element = ArtworkViewModel(artwork, languageSelector)
                        element.playerControl
                                .bindTo(playerControl)
                                .disposedBy(element.viewDisposeBag)

                        audioPlayBackStatus
                                .bindTo(element.audioPlayBackStatus)
                                .disposedBy(disposeBag)

                        element
                    }
                    .map { listOf(it) }
                    .bindTo(searchedObjectViewModels)
                    .disposedBy(disposeBag)
        } else if (requestedSearchAmenityType != null) {
            /**
             * Update carousel to display amenities.
             */
            if (requestedSearchAmenityType == ArticMapAmenityType.DINING) {
                val amenityTypes = ArticMapAmenityType.getAmenityTypes(requestedSearchAmenityType)
                articMapAnnotationDao.getAmenitiesByAmenityType(amenityType = amenityTypes)
                        .toObservable()
                        .map { objects ->
                            objects.map { mapAnnotation ->
                                DiningAnnotationViewModel(mapAnnotation)
                            }
                        }.bindTo(searchedObjectViewModels)
                        .disposedBy(disposeBag)
            } else {
                Observable.just(listOf(AnnotationViewModel(requestedSearchAmenityType, "Close to explore the map.")))
                        .bindTo(searchedObjectViewModels)
                        .disposedBy(disposeBag)
            }
        }
    }

}


open class SearchObjectBaseViewModel : BaseViewModel() {

    sealed class PlayerAction {
        class Play(val requestedObject: Playable, val audioFileModel: AudioFileModel? = null) : PlayerAction()
        class Pause : PlayerAction()
    }

    val audioPlayBackStatus: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val playerControl: Subject<PlayerAction> = PublishSubject.create()

    val title: Subject<String> = BehaviorSubject.create()
    val imageUrl: Subject<String> = BehaviorSubject.create()
}

/**
 * ViewModel for the amenity cell.
 */
class DiningAnnotationViewModel(val item: ArticMapAnnotation) : SearchObjectBaseViewModel() {
    val description: Subject<String> = BehaviorSubject.create()

    init {
        title.onNext(item.label?.replace("\r", "\n") ?: item.amenityType.orEmpty())
        imageUrl.onNext(item.imageUrl.orEmpty())
        description.onNext(item.description.orEmpty())
    }

}

/**
 * ViewModel for the amenity cell.
 */
class AnnotationViewModel(item: String, modelDescription: String) : SearchObjectBaseViewModel() {
    val description: Subject<String> = BehaviorSubject.create()

    init {
        title.onNext(item)
        description.onNext(modelDescription)
    }

}

/**
 * ViewModel for the artwork cell.
 */
class ArtworkViewModel(val item: ArticSearchArtworkObject, val languageSelector: LanguageSelector) : SearchObjectBaseViewModel() {

    val artworkTitle: Subject<String> = BehaviorSubject.createDefault(item.title)
    val artistName: Subject<String> = BehaviorSubject.create()

    val objectType: Subject<Int> = BehaviorSubject.createDefault(R.string.artworks)
    private val audioFileModel = item.backingObject?.audioFile?.preferredLanguage(languageSelector)
    val playState: Subject<AudioPlayerService.PlayBackState> = BehaviorSubject.create()
    val hasAudio: Subject<Boolean> = BehaviorSubject.createDefault(item.backingObject != null)

    init {
        imageUrl.onNext(item.thumbUrl.orEmpty())

        /**
         * Sync the UI with the current audio track.
         */
        audioPlayBackStatus
                .filter { playBackState ->
                    playBackState.audio == audioFileModel
                }.bindTo(playState)
                .disposedBy(disposeBag)

        item.artistDisplay?.let {
            artistName.onNext(it)
        }
    }

    /**
     * Play the audio translation for the selected artwork.
     */
    fun playAudioTranslation() {
        item.backingObject?.let {
            playerControl.onNext(PlayerAction.Play(it, audioFileModel))
        }
    }

    fun pauseAudioTranslation() {
        playerControl.onNext(PlayerAction.Pause())
    }
}


