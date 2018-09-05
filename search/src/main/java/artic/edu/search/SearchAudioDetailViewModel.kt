package artic.edu.search

import com.fuzz.rx.bindTo
import com.fuzz.rx.disposedBy
import com.fuzz.rx.filterFlatMap
import edu.artic.analytics.AnalyticsTracker
import edu.artic.base.utils.DateTimeHelper
import edu.artic.db.models.ArticEvent
import edu.artic.db.models.ArticObject
import edu.artic.viewmodel.NavViewViewModel
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

class SearchAudioDetailViewModel @Inject constructor(private val analyticsTracker: AnalyticsTracker)
    : NavViewViewModel<SearchAudioDetailViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint

    val imageUrl: Subject<String> = BehaviorSubject.create()
    val title: Subject<String> = BehaviorSubject.createDefault("test")
    val authorCulturalPlace: Subject<String> = BehaviorSubject.create()
    val showOnMapButtonText: Subject<String> = BehaviorSubject.createDefault("Show on Map") // TODO: replace when special localizer is done
    val playAudioButtonText: Subject<String> = BehaviorSubject.createDefault("Play Audio")// TODO: replace when special localizer is done

    private val articObjectObservable: Subject<ArticObject> = BehaviorSubject.create()

    var articObject: ArticObject? = null
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
                .map { it.getPlayableThumbnailUrl().orEmpty() }
                .bindTo(imageUrl)
                .disposedBy(disposeBag)

        articObjectObservable
                .filterFlatMap({ it is ArticObject }, { it as ArticObject })
                .map {
                    it.artistCulturePlaceDelim?.replace("\r", "\n").orEmpty()
                }.bindTo(authorCulturalPlace)
                .disposedBy(disposeBag)
    }

    fun onClickShowOnMap() {
        //TODO: go to map to show this
    }

    fun onClickPlayAudio() {
        //TODO: start playing
    }

}

