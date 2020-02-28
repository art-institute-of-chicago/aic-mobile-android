package edu.artic.audio

import android.annotation.SuppressLint
import com.fuzz.rx.Optional
import com.fuzz.rx.bindTo
import com.fuzz.rx.bindToMain
import com.fuzz.rx.disposedBy
import edu.artic.analytics.AnalyticsLabel
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.analytics.ScreenName
import edu.artic.audio.LookupResult.FoundAudio
import edu.artic.audio.LookupResult.NotFound
import edu.artic.audio.NumberPadElement.*
import edu.artic.db.Playable
import edu.artic.db.daos.*
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.media.audio.AudioPlayerService
import edu.artic.media.audio.AudioPrefManager
import edu.artic.media.audio.preferredLanguage
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/**
 * This class provides important audio-related logic to an [AudioLookupFragment].
 *
 * For the ViewModel shown for details about a single audio file, check out
 * [AudioDetailsViewModel] instead.
 *
 * For lookup, we always query [ArticObject.objectSelectorNumber].
 */
class AudioLookupViewModel @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        objectLookupDao: ArticObjectDao,
        tourLookupDao: ArticTourDao,
        private val audioFileDao: ArticAudioFileDao,
        generalInfoDao: GeneralInfoDao,
        private val languageSelector: LanguageSelector,
        private val audioPrefManager: AudioPrefManager
) : NavViewViewModel<AudioLookupViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object Search : NavigationEndpoint()
        object AudioDetails : NavigationEndpoint()
        object ClearSearch : NavigationEndpoint()
    }

    /**
     * This reflects the [ArticGeneralInfo] currently assigned to this screen.
     *
     * It provides helpful content like
     * [the lookup hint text][ArticGeneralInfo.Translation.audioTitle] and
     * [the subheader instructions][ArticGeneralInfo.Translation.audioSubtitle].
     */
    val chosenInfo: Subject<ArticGeneralInfo.Translation> = BehaviorSubject.create()

    val adapterClicks: Subject<NumberPadElement> = PublishSubject.create()

    /**
     * New requests for audio lookup. Each entry to this will trigger a response,
     * published on [lookupFailures].
     */
    val lookupRequests: Subject<String> = PublishSubject.create()

    /**
     * Failing responses to search queries [passed in][io.reactivex.Observer.onNext] to
     * [lookupRequests].
     *
     * Successful responses are handled directly in [playAndDisplay].
     */
    val lookupFailures: Subject<LookupResult.NotFound> = PublishSubject.create()

    /**
     * This is reset to null in [cleanup], which we expect to usually be called by
     * [edu.artic.viewmodel.BaseViewModelFragment.onDestroyView].
     */
    @SuppressLint("StaticFieldLeak")
    var audioService: AudioPlayerService? = null

    /**
     * See [NumberPadAdapter] for details on all this.
     */
    val preferredNumberPadElements: Subject<List<NumberPadElement>> = BehaviorSubject.createDefault(listOf(
            Numeric("1"),
            Numeric("2"),
            Numeric("3"),
            Numeric("4"),
            Numeric("5"),
            Numeric("6"),
            Numeric("7"),
            Numeric("8"),
            Numeric("9"),
            DeleteBack,
            // NB: Due to a bug in the ideal_sans_medium font files, the 0 and o look very similar. This is a zero.
            Numeric("0"),
            GoSearch
    ))

    init {

        val objectFoundObservable = lookupRequests
                .observeOn(Schedulers.io())
                .map { objectSelectorNumber ->

                    var result: Playable? = objectLookupDao.getObjectBySelectorNumber(objectSelectorNumber)?.getOrNull(0)
                    if (result == null) {
                        result = tourLookupDao.getTourBySelectorNumber(objectSelectorNumber)
                    }
                    objectSelectorNumber to Optional(result)
                }

        objectFoundObservable
                .filter { (_, result) -> result.value != null }
                .map { (objectSelectorNumber, result) -> objectSelectorNumber to result.value as Playable }
                .map { (objectSelectorNumber, result) -> LookupResult.FoundAudio(result, objectSelectorNumber) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { playAndDisplay(it) }
                .disposedBy(disposeBag)

        objectFoundObservable
                .filter { (_, result) -> result.value == null }
                .map { LookupResult.NotFound("") }
                .bindToMain(lookupFailures)
                .disposedBy(disposeBag)


        /**
         * Subscribe to locale change event.
         */
        Observables.combineLatest(
                languageSelector.currentLanguage,
                generalInfoDao.getGeneralInfo().toObservable()
        )
                .map { (_, generalObject) ->
                    languageSelector.selectFrom(generalObject.allTranslations())
                }.bindTo(chosenInfo)
                .disposedBy(disposeBag)
    }


    private fun playAndDisplay(foundAudio: LookupResult.FoundAudio) {
        val playable = foundAudio.hostObject

        audioService?.let {
            when (playable) {
                is ArticObject -> playArticObject(playable, foundAudio.objectSelectorNumber, it)
                is ArticTour -> playArticTour(playable, it)
            }
        }
    }

    private fun playArticTour(tour: ArticTour, audioService: AudioPlayerService) {
        tour.tourAudio?.let { audioID ->
            audioFileDao.getAudioByIdAsync(audioID)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy { audioFile ->

                        // Clear search prior to playPlayer since AudioTutorial may intercede
                        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ClearSearch))

                        // Trigger analytics for playback
                        val analyticsParamMap: Map<String, String> = mapOf(
                                AnalyticsLabel.playbackSource to ScreenName.AudioGuide.screenName,
                                AnalyticsLabel.tourTitle to tour.title,
                                AnalyticsLabel.audioTitle to audioFile.title.orEmpty(),
                                AnalyticsLabel.playbackLanguage to audioFile.asAudioFileModel().fileLanguageForAnalytics().toString()
                        )
                        analyticsTracker.reportCustomEvent(EventCategoryName.AudioPlayed, analyticsParamMap)

                        // Request the actual playback (this triggers its own analytics event)
                        audioService.playPlayer(tour, audioFile.preferredLanguage(languageSelector))

                        // Switch to the details screen
                        if (audioPrefManager.hasSeenAudioTutorial) {
                            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.AudioDetails))
                        }
                    }
                    .disposedBy(disposeBag)

        }
    }

    private fun playArticObject(articObject: ArticObject,
                                objectSelectorNumber: String,
                                audioService: AudioPlayerService) {

        val selectedAudioFileModel = articObject.audioFileBySelectorNumber(objectSelectorNumber)

        // Clear search prior to playPlayer since AudioTutorial may intercede
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.ClearSearch))

        // Request the actual playback (this triggers its own analytics event)
        if (selectedAudioFileModel != null) {
            val audioModel = selectedAudioFileModel.preferredLanguage(languageSelector)
            audioService.playPlayer(articObject, audioModel)

            // Trigger analytics event for playback
            val analyticsParamMap: Map<String, String> = mapOf(
                    AnalyticsLabel.playbackSource to ScreenName.AudioGuide.screenName,
                    AnalyticsLabel.title to articObject.title,
                    AnalyticsLabel.tourTitle to articObject.tourTitles.orEmpty(),
                    AnalyticsLabel.audioTitle to selectedAudioFileModel.title.orEmpty(),
                    AnalyticsLabel.playbackLanguage to selectedAudioFileModel.asAudioFileModel().fileLanguageForAnalytics().toString()
            )
            analyticsTracker.reportCustomEvent(EventCategoryName.AudioPlayed, analyticsParamMap)

        } else {
            audioService.playPlayer(articObject)
        }

        // Switch to the details screen
        if (audioPrefManager.hasSeenAudioTutorial) {
            navigateTo.onNext(Navigate.Forward(NavigationEndpoint.AudioDetails))
        }
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }

    override fun cleanup() {
        super.cleanup()
        audioService = null
    }

}

/**
 * Constant class for the asynchronous response to looking for an
 * [ArticObject] by [selector id][ArticObject.objectSelectorNumber].
 *
 * Two known subclasses:
 * * [FoundAudio]
 * * [NotFound]
 */
sealed class LookupResult {

    /**
     * Audio was found! You can pass [hostObject] to the
     * [AudioPlayerService][edu.artic.media.audio.AudioPlayerService]
     * if you want to play it.
     *
     * NB: [hostObject] can be safely assumed to contain at least one
     * [ArticAudioFile][edu.artic.db.models.ArticAudioFile].
     *
     * @see AudioLookupViewModel.lookupRequests
     */
    class FoundAudio(val hostObject: Playable, val objectSelectorNumber: String) : LookupResult()

    /**
     * The requested id was found.
     *
     * TODO: Add analytics event indicating that this query was not found.
     */
    class NotFound(val searchQuery: String) : LookupResult()
}
