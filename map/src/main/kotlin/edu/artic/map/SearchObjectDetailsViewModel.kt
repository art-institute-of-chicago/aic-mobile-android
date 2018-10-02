package edu.artic.map;

import com.fuzz.rx.*
import edu.artic.db.Playable
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.audio.preferredLanguage
import edu.artic.viewmodel.BaseViewModel
import edu.artic.viewmodel.CellViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject


/**
 * @author Sameer Dhakal (Fuzz)
 */
class SearchObjectDetailsViewModel @Inject constructor(
        val languageSelector: LanguageSelector,
        private val articMapAnnotationDao: ArticMapAnnotationDao,
        private val searchManager: SearchManager,
        generalInfoDao: GeneralInfoDao
) : BaseViewModel() {

    private val generalInfo: Subject<ArticGeneralInfo.Translation> = BehaviorSubject.create()
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


        // This detects subsequent changes in the app language
        Observables.combineLatest(
                languageSelector.appLanguageWithUpdates(),
                generalInfoDao.getGeneralInfo().toObservable()
        )
                .map { (_, generalInfo) ->
                    languageSelector.selectFrom(generalInfo.allTranslations())
                }
                .bindTo(generalInfo)
                .disposedBy(disposeBag)
    }

    /**
     * when the search type is amenities fetch all the amenities and create viewmodels
     */
    fun viewResumed(requestedSearchObject: ArticSearchArtworkObject?,
                    requestedSearchAmenityType: String?,
                    exhibition: ArticExhibition?) {

        /**
         * latestTourObject and amenityType are mutually exclusive
         */
        if (requestedSearchObject != null) {
            /**
             * Update carousel to display objects.
             */
            Observable.just(requestedSearchObject)
                    .map { artwork ->
                        val element = ArtworkViewModel(viewDisposeBag, artwork, languageSelector)
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
                                DiningAnnotationViewModel(viewDisposeBag, mapAnnotation)
                            }
                        }.bindTo(searchedObjectViewModels)
                        .disposedBy(disposeBag)
            } else {
                generalInfo
                        .map {
                            val amenityTitle: String = ArticMapAmenityType.titleFor(it, requestedSearchAmenityType)
                            val amenityDescription: String = ArticMapAmenityType.textFor(it, requestedSearchAmenityType)
                            listOf(AnnotationViewModel(viewDisposeBag, amenityTitle, amenityDescription))
                        }
                        .bindTo(searchedObjectViewModels)
                        .disposedBy(disposeBag)
            }
        } else if (exhibition != null) {
            Observable.just(exhibition)
                    .map { exhibition -> ExhibitionViewModel(exhibition) }
                    .map { listOf(it) }
                    .bindTo(searchedObjectViewModels)
                    .disposedBy(disposeBag)
        }
    }

}


open class SearchObjectBaseViewModel(
        adapterDisposeBag: DisposeBag?
) : CellViewModel(adapterDisposeBag) {

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
class DiningAnnotationViewModel(
        adapterDisposeBag: DisposeBag,
        val item: ArticMapAnnotation
) : SearchObjectBaseViewModel(adapterDisposeBag) {
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
class AnnotationViewModel(
        adapterDisposeBag: DisposeBag,
        item: String,
        modelDescription: String
) : SearchObjectBaseViewModel(adapterDisposeBag) {
    val description: Subject<String> = BehaviorSubject.create()

    init {
        title.onNext(item)
        description.onNext(modelDescription)
    }

}

/**
 * ViewModel for the artwork cell.
 */
class ArtworkViewModel(
        adapterDisposeBag: DisposeBag,
        val item: ArticSearchArtworkObject,
        val languageSelector: LanguageSelector
) : SearchObjectBaseViewModel(adapterDisposeBag) {

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

/**
 * ViewModel for the Exhibition cell.
 */
class ExhibitionViewModel(val item: ArticExhibition) : SearchObjectBaseViewModel(null) {
    val objectType: Subject<Int> = BehaviorSubject.createDefault(R.string.artworks)
    val description: Subject<String> = BehaviorSubject.create()

    init {
        title.onNext(item.title?.replace("\r", "\n"))
        item.legacyImageUrl?.let {
            imageUrl.onNext(it)
        }
        description.onNext(item.short_description.orEmpty())
    }
}


