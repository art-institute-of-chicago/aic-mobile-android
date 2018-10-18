package edu.artic.map

import android.location.Location
import com.fuzz.rx.*
import com.fuzz.rx.Optional
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import edu.artic.analytics.AnalyticsAction
import edu.artic.analytics.AnalyticsTracker
import edu.artic.analytics.EventCategoryName
import edu.artic.db.daos.ArticMapAnnotationDao
import edu.artic.db.daos.ArticMapFloorDao
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.daos.GeneralInfoDao
import edu.artic.db.models.*
import edu.artic.localization.LanguageSelector
import edu.artic.location.LocationPreferenceManager
import edu.artic.location.LocationService
import edu.artic.location.isLocationInMuseum

import edu.artic.map.helpers.toLatLng
import edu.artic.map.rendering.MapItemModel
import edu.artic.map.rendering.MarkerHolder
import edu.artic.map.tutorial.TutorialPreferencesManager
import edu.artic.tours.manager.TourProgressManager
import edu.artic.viewmodel.NavViewViewModel
import edu.artic.viewmodel.Navigate
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Description: the primary ViewModel backing [MapFragment].
 *
 * Most of the fields are documented, so take a look at those first.
 */
class MapViewModel @Inject constructor(val mapMarkerConstructor: MapMarkerConstructor,
                                       private val articObjectDao: ArticObjectDao,
                                       private val languageSelector: LanguageSelector,
                                       private val searchManager: SearchManager,
                                       private val analyticsTracker: AnalyticsTracker,
                                       private val tourProgressManager: TourProgressManager,
                                       private val locationService: LocationService,
                                       private val tutorialPreferencesManager: TutorialPreferencesManager,
                                       private val locationPreferenceManager: LocationPreferenceManager,
                                       articMapAnnotationDao: ArticMapAnnotationDao,
                                       generalInfoDao: GeneralInfoDao,
                                       mapFloorDao: ArticMapFloorDao
) : NavViewViewModel<MapViewModel.NavigationEndpoint>() {

    sealed class NavigationEndpoint {
        object LocationPrompt : NavigationEndpoint()
        class Tutorial(val currentFloor: Int) : NavigationEndpoint()
        object Search : NavigationEndpoint()
    }

    private val articMapFloorMap: Subject<Map<Int, ArticMapFloor>> = BehaviorSubject.create()

    /**
     * Current floor of map to show. Defaults to 1, valid values are
     *
     * * 0 (a.k.a. `"LL"`)
     * * 1
     * * 2
     * * 3
     *
     * Please compare proposed floors against the [edu.artic.db.INVALID_FLOOR] constant
     * before setting them to this field.
     */
    private val floor: Subject<Int> = BehaviorSubject.createDefault(1)
    private val focus: Subject<MapFocus> = BehaviorSubject.create()

    /**
     * Current mode of display.
     *
     * Changes infrequently.
     *
     * Defaults to [edu.artic.map.MapDisplayMode.CurrentFloor].
     */
    val displayMode: Subject<MapDisplayMode> = BehaviorSubject.create()
    /**
     * Current object of interest.
     *
     * Set by search or by tapping a marker.
     *
     * Not set by default.
     */
    val selectedArticObject: Subject<ArticObject> = BehaviorSubject.create()
    /**
     * Which [ArticGeneralInfo.Translation] to use for important, general-purpose text.
     *
     * Updated whenever the app language changes.
     *
     * Set to the current language's translation by default.
     *
     * @see LanguageSelector.currentLanguage
     */
    val chosenInfo: Subject<ArticGeneralInfo.Translation> = BehaviorSubject.create()
    /**
     * Current exhibition of interest.
     *
     * Set by search or by tapping a marker.
     *
     * Not set by default.
     */
    val selectedExhibition: Subject<ArticExhibition> = BehaviorSubject.create()
    /**
     * Position and desired [MapFocus].
     *
     * Set when something has been clicked.
     *
     * Not set by default.
     */
    val individualMapChange: Subject<Optional<Pair<LatLng, MapFocus>>> = PublishSubject.create()
    /**
     * Points that should be visible on the map.
     *
     * Set when the camera needs to move so we can see them.
     *
     * Not set by default.
     */
    val boundsOfInterestChanged: Relay<List<LatLng>> = PublishRelay.create()
    /**
     * Reference to the [GoogleMap] we're displaying stuff in.
     *
     * Set whenever the [actual widget][android.widget.RemoteViews.RemoteView] boots up.
     *
     * Defaults to Optional(null).
     */
    val currentMap: Subject<Optional<GoogleMap>> = BehaviorSubject.createDefault(Optional(null))
    /**
     * Proxy link to [TourProgressManager.leaveTourRequest]. Only receives `true` events.
     *
     * Called whenever the user wants to switch out of a [MapDisplayMode.Tour].
     *
     * Not set by default.
     */
    val leaveTourRequest: Subject<Boolean> = PublishSubject.create()
    /**
     * Callback to release [MapDisplayMode.Tour]-related resources.
     *
     * This is given an event at the end of each call to [leaveCurrentTour].
     *
     * Not set by default.
     */
    val leftActiveTour: Subject<Boolean> = PublishSubject.create()
    /**
     * Suggestion that maybe we should leave the current tour and show this one instead.
     *
     * Called only if we're already in [MapDisplayMode.Tour] when a new 'Tour' is proposed.
     *
     * Not set by default.
     *
     * @see loadMapDisplayMode
     */
    val switchTourRequest: Subject<Pair<ArticTour, ArticTour.TourStop>> = PublishSubject.create()

    val isUserInMuseum: Subject<Boolean> = BehaviorSubject.createDefault(false)

    val showCompass: Subject<Boolean> = BehaviorSubject.createDefault(false)

    val focusToTracking: Subject<Pair<GoogleMap, Optional<Location>>> = BehaviorSubject.create()

    val showFirstRunHeader: Subject<Boolean> = BehaviorSubject.create()

    /**
     * Safe and simple reference to [floor]. Only emits when the floor number changes, will
     * never emit [edu.artic.db.INVALID_FLOOR].
     */
    private val distinctFloorInt = floor.distinctUntilChanged().filter { it in 0..3 }


    val distinctFloor: Subject<ArticMapFloor> = BehaviorSubject.create()

    val selectedTourStopMarkerId: Subject<String> = BehaviorSubject.create()
    val selectedDiningPlace: Subject<Optional<ArticMapAnnotation>> = BehaviorSubject.create()

    private val visibleRegionChanges: Subject<VisibleRegion> = BehaviorSubject.create()

    // when set, normal visible region changes are locked until, say the map move completes.
    private val lockVisibleRegion: Subject<Boolean> = BehaviorSubject.createDefault(false)

    private var shouldFollowUserObservable: Subject<Boolean> = BehaviorSubject.createDefault(false)
    private var shouldFollowUserDistinct: Observable<Boolean> = shouldFollowUserObservable.distinctUntilChanged()
    private val hasSeenHeaderThisSession: Subject<Boolean> = BehaviorSubject.createDefault(false)
    private var shouldFollowUser: Boolean = false
        set(value) {
            field = value
            shouldFollowUserObservable.onNext(value)
        }


    init {

        setupLocationServiceBindings()


        mapFloorDao.getMapFloors()
                .map { floorMap -> floorMap.associateBy { it.number } }
                .bindTo(articMapFloorMap)
                .disposedBy(disposeBag)

        Observables
                .combineLatest(distinctFloorInt, articMapFloorMap)
                .subscribeBy { (floor, floorMap) ->
                    floorMap[floor]?.let {
                        distinctFloor.onNext(it)
                    }

                }.disposedBy(disposeBag)

        mapMarkerConstructor
                .bindToMapChanges(
                        distinctFloor.map { it.number },
                        focus.distinctUntilChanged(),
                        displayMode.distinctUntilChanged(),
                        visibleRegionChanges.distinctUntilChanged(),
                        selectedArticObject.distinctUntilChanged()
                )


        // We need this for the header's title and subtitle
        Observables.combineLatest(
                languageSelector.currentLanguage,
                generalInfoDao.getGeneralInfo().toObservable()
        )
                .map { (_, generalObject) ->
                    languageSelector.selectFrom(generalObject.allTranslations())
                }
                .bindTo(chosenInfo)
                .disposedBy(disposeBag)



        // when we change to tour mode, we notify the tourProgressManager.
        displayMode
                .distinctUntilChanged()
                .filterFlatMap({ it is MapDisplayMode.Tour }, { (it as MapDisplayMode.Tour).tour })
                .doOnNext {
                    floorChangedTo(it.floorAsInt)
                    analyticsTracker.reportEvent(EventCategoryName.Tour, AnalyticsAction.tourStarted, it.title)
                }
                .mapOptional()
                .bindTo(tourProgressManager.selectedTour)
                .disposedBy(disposeBag)
        /**
         * Update the floor if the selected tour stop is not in current floor.
         */
        Observables.combineLatest(tourProgressManager.selectedTour, tourProgressManager.selectedStop)
                .filterFlatMap({ (tour, _) -> tour.value != null },
                        { (tour, stop) -> tour.value!! to stop })
                .flatMap { (_, stopID) ->
                    articObjectDao.getObjectById(stopID).toObservable()
                }.withLatestFrom(floor)
                .subscribeBy { (tourStop, floor) ->
                    if (tourStop.floor != floor) {
                        floorChangedTo(tourStop.floor)
                    }
                }
                .disposedBy(disposeBag)

        /**
         * Select correct floor for the search display mode.
         */
        displayMode
                .distinctUntilChanged()
                .filterFlatMap({ it is MapDisplayMode.Search.ObjectSearch }, { (it as MapDisplayMode.Search.ObjectSearch) })
                .subscribe {
                    floorChangedTo(it.item.floor)
                }.disposedBy(disposeBag)

        /**
         * Select the floor which has at least one amenity.
         */
        displayMode
                .distinctUntilChanged()
                .filterFlatMap({ it is MapDisplayMode.Search.AmenitiesSearch }, { (it as MapDisplayMode.Search.AmenitiesSearch) })
                .flatMap { searchMode ->
                    val amenityType = ArticMapAmenityType.getAmenityTypes(searchMode.item)
                    articMapAnnotationDao
                            .getAmenitiesByAmenityType(amenityType)
                            .toObservable()
                            .map { annotations -> annotations.first() }
                }
                .subscribe { annotation ->
                    annotation.floor?.let {
                        floorChangedTo(it)
                    }
                }.disposedBy(disposeBag)

        displayMode
                .distinctUntilChanged()
                .filterFlatMap({ it is MapDisplayMode.Tour }, { it as MapDisplayMode.Tour })
                .filterFlatMap({ it.selectedTourStop?.objectId != null }, { it.selectedTourStop?.objectId })
                .map { it }
                .bindTo(tourProgressManager.selectedStop)
                .disposedBy(disposeBag)


        /**
         * Sync the selected tour stop with the carousel.
         */
        selectedArticObject
                .distinctUntilChanged()
                .map { it.nid }
                .bindTo(tourProgressManager.selectedStop)
                .disposedBy(disposeBag)

        /**
         * Sync the carousel tour stop with map tour stop.
         */
        tourProgressManager.selectedStop
                .distinctUntilChanged()
                .bindTo(selectedTourStopMarkerId)
                .disposedBy(disposeBag)

        tourProgressManager
                .leaveTourRequest
                .bindTo(leaveTourRequest)
                .disposedBy(disposeBag)

        searchManager.leaveSearchMode
                .filter { it }
                .subscribe {
                    displayModeChanged(MapDisplayMode.CurrentFloor)
                }.disposedBy(disposeBag)

        /**
         * Update map bounds to focus selected annotation.
         * Used by dining carousel.
         */

        searchManager.activeDiningPlace
                .bindToMain(selectedDiningPlace)
                .disposedBy(disposeBag)

        searchManager.activeDiningPlace
                .filterValue()
                .delay(750, TimeUnit.MILLISECONDS)
                .subscribe { diningPlace ->
                    diningPlace.floor?.let {
                        floorChangedTo(it)
                    }
                }
                .disposedBy(disposeBag)

        selectedExhibition
                .subscribe { exhibition ->
                    exhibition.floor?.let {
                        floorChangedTo(it)
                    }
                }
                .disposedBy(disposeBag)

        this.currentMap
                .bindTo(mapMarkerConstructor.map)
                .disposedBy(disposeBag)

        /**
         * If new tour is requested when one is in progress save it as next tour.
         */
        switchTourRequest
                .mapOptional()
                .bindTo(tourProgressManager.proposedTour)
                .disposedBy(disposeBag)
    }

    private fun setupLocationServiceBindings() {

        locationService.currentUserLocation
                .map {
                    isLocationInMuseum(it)
                }
                .bindTo(isUserInMuseum)
                .disposedBy(disposeBag)

        isUserInMuseum
                .bindTo(showCompass)
                .disposedBy(disposeBag)

        Observables.combineLatest(
                locationService.currentUserLocation,
                shouldFollowUserDistinct,
                currentMap
        ) { currentLocation, shouldFollowUser, map ->
            return@combineLatest if (map.value != null && shouldFollowUser) {
                map.value to Optional(currentLocation)
            } else {
                map.value to Optional(null)
            }
        }
                .filterFlatMap(
                        { (map, _) -> map != null },
                        { (map, optional) -> map!! to optional }
                )
                .bindTo(focusToTracking)
                .disposedBy(disposeBag)

    }

    fun setupPreferenceBindings(lifetimeDisposeBag: DisposeBag) {


        Observables
                .combineLatest(
                        locationPreferenceManager.hasSeenLocationPromptObservable,
                        locationPreferenceManager.hasClosedLocationPromptObservable,
                        tutorialPreferencesManager.hasSeenTutorialObservable
                ).map {
                    (hasSeenPrompt, hasClosedPrompt, hasSeenTutorial) ->

                    !hasSeenPrompt to (hasClosedPrompt && !hasSeenTutorial)
                }
                .filter {
                    it.first || it.second
                }
                .withLatestFrom(floor)
                .map {
                    (whatToShow, floor) ->
                    val (shouldShowPrompt, shouldShowTutorial) = whatToShow
                    when {
                        shouldShowPrompt -> Navigate.Forward(NavigationEndpoint.LocationPrompt)
                        shouldShowTutorial -> Navigate.Forward(NavigationEndpoint.Tutorial(floor))
                        else -> throw IllegalStateException("Map Overlay requested, but none of the available types are permissible at this time.")
                    }
                }
                .bindTo(navigateTo)
                .disposedBy(lifetimeDisposeBag)

        Observables
                .combineLatest(
                        tutorialPreferencesManager.hasClosedTutorialObservable,
                        displayMode.distinctUntilChanged(),
                        hasSeenHeaderThisSession.distinctUntilChanged()
                ) { hasClosedTutorial, displayMode, hasSeenHeader ->
                    val shouldShowHeader = hasClosedTutorial && !hasSeenHeader && displayMode == MapDisplayMode.CurrentFloor
                    if (shouldShowHeader || (hasClosedTutorial && !hasSeenHeader && displayMode != MapDisplayMode.CurrentFloor))
                        hasSeenHeaderThisSession.onNext(true)
                    return@combineLatest shouldShowHeader
                }.distinctUntilChanged()
                .filter { it }
                .bindTo(showFirstRunHeader)
                .disposedBy(lifetimeDisposeBag)

    }

    fun onClickCompass() {
        shouldFollowUser = !shouldFollowUser

        if (shouldFollowUser) {
            analyticsTracker.reportEvent(EventCategoryName.Location, AnalyticsAction.locationHeadingEnabled)
        }
    }

    fun onClickSearch() {
        navigateTo.onNext(Navigate.Forward(NavigationEndpoint.Search))
    }


    fun setMap(map: GoogleMap) {
        currentMap.onNext(optionalOf(map))
    }

    fun floorChangedTo(floor: Int) {
        this.floor.onNext(floor)
    }

    fun zoomLevelChanged(zoomLevel: ZoomLevel) {
        this.focus.onNext(zoomLevel.toMapFocus())
    }

    fun departmentMarkerSelected(department: ArticMapAnnotation) {
        individualMapChange.onNext(optionalOf(department.toLatLng() to MapFocus.Individual))
    }

    fun articObjectSelected(articObject: ArticObject) {
        lockVisibleRegion.onNext(true)
        selectedArticObject.onNext(articObject)
    }

    fun exhibitionSelected(exhibition: ArticExhibition) {
        lockVisibleRegion.onNext(true)
        selectedExhibition.onNext(exhibition)
    }

    /**
     * Called (essentially exclusively) by [loadMapDisplayMode].
     *
     * Triggers animations as needed so that [selectedArticObject]
     * etc. is visible on the map.
     */
    fun displayModeChanged(displayMode: MapDisplayMode) {
        this.displayMode.onNext(displayMode)
        if (displayMode is MapDisplayMode.Tour) {
            animateToTourStopBounds(displayMode)
        } else if (displayMode is MapDisplayMode.Search<*>) {
            animateToSearchItemBounds(displayMode)
        }
    }

    private fun animateToSearchItemBounds(displayMode: MapDisplayMode.Search<*>) {
        val displayItem = displayMode.item

        Observable.just(displayItem)
                .map {
                    when (it) {
                        is ArticSearchArtworkObject -> listOf(it.toLatLng())
                        is ArticExhibition -> listOf(it.toLatLng())
                        else -> emptyList()
                    }
                }
                .bindToMain(boundsOfInterestChanged)
                .disposedBy(disposeBag)

        if (displayItem is ArticSearchArtworkObject) {
            displayItem.backingObject?.let {
                articObjectSelected(it)
            }
        }
        if (displayItem is ArticExhibition) {
            exhibitionSelected(displayItem)
        }
    }

    private fun animateToTourStopBounds(displayMode: MapDisplayMode.Tour) {
        val tour = displayMode.tour
        val tourStop = displayMode.selectedTourStop ?: tour.getIntroStop()

        val latLongs: Observable<List<LatLng>> = if (tourStop.isIntroStop()) {
            /** If the starting tour is intro stop load map in bird eye view**/
            val allToursLatLongs = articObjectDao
                    .getObjectsByIdList(tour.tourStops.mapNotNull { it.objectId })
                    .map { stops -> stops.map { it.toLatLng() } }
                    .toObservable()

            Observable.merge(
                    Observable.just(listOf(tour.toLatLng())),
                    allToursLatLongs
            )
        } else {
            articObjectDao.getObjectById(tourStop.objectId!!)
                    .toObservable()
                    .map { listOf(it.toLatLng()) }
        }

        latLongs
                .bindToMain(boundsOfInterestChanged)
                .disposedBy(disposeBag)
    }

    fun visibleRegionChanged(visibleRegion: VisibleRegion) {
        // only emit if visible region is not locked.
        lockVisibleRegion
                .take(1)
                .filter { !it }
                .subscribeBy {
                    visibleRegionChanges.onNext(visibleRegion)
                }
                .disposedBy(disposeBag)
    }

    fun visibleRegionIdle(visibleRegion: VisibleRegion) {
        this.lockVisibleRegion.onNext(false)
        visibleRegionChanged(visibleRegion)
    }

    /**
     * Retrieve an object from the [MapMarkerConstructor]
     */
    fun retrieveObjectById(nid: String): Observable<Optional<MarkerHolder<MapItemModel>>> {
        return mapMarkerConstructor.objectsMapItemRenderer.getMarkerHolderById(nid)
    }

    override fun onCleared() {
        super.onCleared()
        mapMarkerConstructor.cleanup()
    }


    /**
     * Updates the display mode of the map.
     * For future search integration, active tour should be canceled before we display the search items in map.
     * refer [TourProgressManager] for managing the tour state.
     *
     * If the current [MapDisplayMode] is no longer appropriate, this method will call
     * [displayModeChanged] to emit the necessary change.
     */
    fun loadMapDisplayMode(requestedTourInfo: Pair<ArticTour?, ArticTour.TourStop?>) {

        Observables.combineLatest(
                tourProgressManager.selectedTour,
                tourProgressManager.proposedTour,
                searchManager.selectedObject,
                searchManager.selectedAmenityType,
                searchManager.selectedExhibition
        ) { currentTour, nextTour, lastSearchedObject, annotationType, exhibition ->
            val tours = currentTour.value to nextTour.value
            val search = Triple(lastSearchedObject.value, annotationType.value, exhibition.value)
            tours to search
        }
                .take(1)
                .subscribeBy { (tours, searchTypes) ->
                    val (activeTour, proposedTour) = tours
                    val (searchObject, searchAnnotationType, searchExhibition) = searchTypes
                    val (requestedTour, requestedTourStop) = requestedTourInfo

                    if ((searchObject != null || searchAnnotationType != null || searchExhibition != null) && requestedTour == null) {
                        /**
                         * If user requests to load search when tour is active, prompt user to leave tour.
                         */
                        if (activeTour != null) {
                            leaveTourRequest.onNext(true)
                        } else {
                            if (searchObject != null) {
                                displayModeChanged(MapDisplayMode.Search.ObjectSearch(searchObject))
                            } else if (searchAnnotationType != null) {
                                displayModeChanged(MapDisplayMode.Search.AmenitiesSearch(searchAnnotationType))
                            } else if (searchExhibition != null) {
                                displayModeChanged(MapDisplayMode.Search.ExhibitionSearch(searchExhibition))
                            }
                        }
                    } else if (requestedTour != null && activeTour != null && requestedTour != activeTour) {
                        /**
                         * If requestedTour is different than current tour display "Leave Current Tour ?" prompt.
                         */
                        searchManager.selectedObject.onNext(Optional(null))
                        val startStop = requestedTourStop ?: activeTour.getIntroStop()
                        switchTourRequest.onNext(requestedTour to startStop)
                    } else if (proposedTour != null) {
                        val (tour, stop) = proposedTour
                        displayModeChanged(MapDisplayMode.Tour(tour, stop))
                        tourProgressManager.proposedTour.onNext(Optional(null))
                    } else {
                        searchManager.selectedObject.onNext(Optional(null))
                        val tourToLoad = requestedTour ?: activeTour
                        if (tourToLoad != null) {
                            displayModeChanged(MapDisplayMode.Tour(tourToLoad, requestedTourStop))
                        } else {
                            displayModeChanged(MapDisplayMode.CurrentFloor)
                        }
                    }
                }.disposedBy(disposeBag)

    }

    /**
     * Loads display mode for the map.
     */
    fun onResume(tourInfo: Pair<ArticTour?, ArticTour.TourStop?>,
                 searchedObject: ArticSearchArtworkObject?,
                 searchedAnnotationType: String?,
                 searchExhibition: ArticExhibition?) {

        locationService.requestTrackingUserLocation()

        if (searchedObject == null &&
                searchExhibition == null &&
                searchedAnnotationType == null &&
                tourInfo.first == null &&
                tourInfo.second == null &&
                (displayMode as BehaviorSubject).hasValue()) {
            return
        }

        /**
         * Store the search object to memory.
         * Used in [MapViewModel.loadMapDisplayMode] to determine the map display mode.
         *
         * When the map gets a searchedObject, it saves searchedObject to
         * [SearchManager.selectedObject] until is cleared out from [SearchManager].
         * If the searchedObject is null we discard it as if there was no search request
         * and fetch cached data from [SearchManager.selectedObject].
         */
        searchedObject?.let {
            searchManager.selectedObject.onNext(Optional(searchedObject))
            searchManager.selectedAmenityType.onNext(Optional(null))
            searchManager.selectedExhibition.onNext(Optional(null))
        }

        searchedAnnotationType?.let {
            searchManager.selectedObject.onNext(Optional(null))
            searchManager.selectedAmenityType.onNext(Optional(it))
            searchManager.selectedExhibition.onNext(Optional(null))
        }

        searchExhibition?.let {
            searchManager.selectedObject.onNext(Optional(null))
            searchManager.selectedAmenityType.onNext(Optional(null))
            searchManager.selectedExhibition.onNext(Optional(it))
        }

        loadMapDisplayMode(tourInfo)
    }

    /**
     * This method clears the active tour.
     * Emits tour ended event via [MapViewModel.leftActiveTour] subject.
     */
    fun leaveCurrentTour() {
        /**
         * Update analytics : user left tour : <tour title>
         */
        tourProgressManager.selectedTour.take(1)
                .filterFlatMap({ it.value != null }, { it.value!! })
                .subscribe {
                    analyticsTracker.reportEvent(EventCategoryName.Tour, AnalyticsAction.tourLeft, it.title)
                }.disposedBy(disposeBag)

        /**
         * Clear selected tour language.
         */
        languageSelector.setTourLanguage(Locale.ROOT)

        /**
         * Finally, clear out the tour object from memory.
         */
        tourProgressManager.selectedTour.onNext(Optional(null))

        /**
         * Emit tour left event.
         * This event is consumed by view to update view states (e.g. audio player, tour carousel etc.).
         */
        leftActiveTour.onNext(true)

    }

    /**
     * User stays with current tour.
     * Clear out cached search object if any.
     * Clear out purposed tour.
     */
    fun stayWithCurrentTour() {
        searchManager.selectedObject.onNext(Optional(null))
        tourProgressManager.proposedTour.onNext(Optional(null))
    }

    fun onTouchWithHeader() {
        showFirstRunHeader.onNext(false)
    }
}