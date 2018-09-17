package edu.artic.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.ScreenCategoryName
import edu.artic.db.models.ArticSearchArtworkObject
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchAudioDetailViewModel @Inject constructor(private val analyticsTracker: AnalyticsTracker)
    : NavViewViewModel<SearchAudioDetailViewModel.NavigationEndpoint>() {

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

    var articObject: ArticSearchArtworkObject? = null
        set(value) {
            field = value
            value?.let {
                articObjectObservable.onNext(it)
            }
        }

    init {

        articObjectObservable
                .map { it.title }
                .bindTo(title)
                .disposedBy(disposeBag)

        articObjectObservable
                .map { it.largeImageUrl.orEmpty() }
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({ it is ArticSearchArtworkObject }, { it as ArticSearchArtworkObject })
                .map {
                    it.artistDisplay?.replace("\r", "\n").orEmpty()
                }.bindTo(authorCulturalPlace)
                .disposedBy(disposeBag)

        articObjectObservable
                .map { it.location.orEmpty() }
                .map { !it.isEmpty() }
                .bindTo(showOnMapVisible)
                .disposedBy(disposeBag)

        articObjectObservable
                .filter { it.audioObject == null }
                .map { false }
                .bindTo(playAudioVisible)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({ it.audioObject != null },{it.audioObject!!})
                .map{
                    it.audioCommentary.isNotEmpty()
                }
                .bindTo(playAudioVisible)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({it.gallery != null} , {it.gallery!!})
                .filter { it.number != null }
                .map { "Gallery ${it.number.toString()}" } // TODO: use localizer
                .bindTo(galleryNumber)
                .disposedBy(disposeBag)

    }

    fun onClickShowOnMap() {
        articObject?.let { articObj ->
            analyticsTracker.reportEvent(ScreenCategoryName.Map, AnalyticsAction.mapShowArtwork, articObj.title)
            navigateTo.onNext(
                    Navigate.Forward(
                            NavigationEndpoint.ObjectOnMap(articObj)
                    )
            )
        }
    }

    fun onClickPlayAudio() {
        //TODO: start playing
    }

}

