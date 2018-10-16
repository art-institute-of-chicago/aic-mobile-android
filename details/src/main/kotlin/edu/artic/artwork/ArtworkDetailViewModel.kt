package edu.artic.artwork

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.Playable
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.db.models.audioFile
import edu.artic.localization.LanguageSelector
import edu.artic.media.audio.PlayerService
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class ArtworkDetailViewModel @Inject constructor(
        private val analyticsTracker: AnalyticsTracker,
        private val languageSelector: LanguageSelector
) : NavViewViewModel<ArtworkDetailViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        data class ObjectOnMap(val articObject: ArticSearchArtworkObject) : NavigationEndpoint()
    }

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val title: Subject<String> = BehaviorSubject.create()
    val galleryNumber : Subject<String> = BehaviorSubject.create()
    val authorCulturalPlace: Subject<String> = BehaviorSubject.create()
    val showOnMapButtonText: Subject<String> = BehaviorSubject.createDefault("Show on Map") // TODO: replace when special localizer is done
    val showOnMapVisible: Subject<Boolean> = BehaviorSubject.create()
    val playAudioButtonText: Subject<String> = BehaviorSubject.createDefault("Play Audio")// TODO: replace when special localizer is done
    val playAudioVisible: Subject<Boolean> = BehaviorSubject.create()
    private val articObjectObservable: Subject<ArticSearchArtworkObject> = BehaviorSubject.create()

    var playerService : PlayerService? = null

    var articObject: ArticSearchArtworkObject? = null
        set(value) {
            field = value
            value?.let {
                articObjectObservable.onNext(it)
            }
        }

    var searchTerm: String? = null

    init {

        articObjectObservable
                .map { it.title }
                .bindTo(title)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({ it.largeImageUrl != null }, {it.largeImageUrl!!})
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({ it is ArticSearchArtworkObject }, { it as ArticSearchArtworkObject })
                .map {
                    it.artistDisplay?.replace("\r", "\n").orEmpty()
                }.bindTo(authorCulturalPlace)
                .disposedBy(disposeBag)

        articObjectObservable
                .map { it.locationValue }
                .map { it.isNotEmpty() }
                .bindTo(showOnMapVisible)
                .disposedBy(disposeBag)

        articObjectObservable
                .filter { it.backingObject == null }
                .map { false }
                .bindTo(playAudioVisible)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({ it.backingObject != null },{it.backingObject!!})
                .map{
                    it.audioCommentary.isNotEmpty()
                }
                .bindTo(playAudioVisible)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({it.gallery != null} , {it.gallery!!})
                .filter { it.number != null }
                .map { it.number.toString() }
                .bindTo(galleryNumber)
                .disposedBy(disposeBag)

    }

    fun onClickShowOnMap() {
        articObject?.let { articObj ->
            analyticsTracker.reportEvent(EventCategoryName.Map, AnalyticsAction.mapShowArtwork, articObj.title)
            navigateTo.onNext(
                    Navigate.Forward(
                            NavigationEndpoint.ObjectOnMap(articObj)
                    )
            )
        }
    }

    fun onClickPlayAudio() {
        playerService?.let {playerService ->
            analyticsTracker.reportEvent(EventCategoryName.SearchPlayArtwork, articObject?.title.orEmpty(), searchTerm.orEmpty())
            articObject?.backingObject?.audioFile?.allTranslations()?.let {
                val articObject = articObject?.backingObject as Playable
                playerService.playPlayer(articObject, languageSelector.selectFrom(it))
            }
        }
    }

    override fun cleanup() {
        super.cleanup()
        playerService = null
    }

}

