package edu.artic.map.rendering

import com.bumptech.glide.request.RequestOptions
import com.fuzz.rx.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.VisibleRegion
import edu.artic.db.daos.ArticObjectDao
import edu.artic.db.models.ArticMapAmenityType
import edu.artic.db.models.ArticObject
import edu.artic.image.GlideApp
import edu.artic.image.asRequestObservable
import edu.artic.image.loadWithThumbnail
import edu.artic.map.*
import edu.artic.map.R
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ObjectsMapItemRenderer(private val objectsDao: ArticObjectDao)
    : MapItemRenderer<MapItemModel>(useBitmapQueue = true) {

    private val selectedArticObject: Subject<Optional<ArticObject>> = BehaviorSubject.createDefault(Optional(null))

    private val articObjectMarkerGenerator by lazy { ArticObjectMarkerGenerator(context) }

    // The rate we check for changes from the incoming stream. (it can update faster).
    private val visibleRegionSampleRate = 500L

    private val loadingBitmap by lazy {
        BitmapDescriptorFactory.fromBitmap(
                articObjectMarkerGenerator.makeIcon(null, scale = .7f))
    }
    private val scaledDot by lazy {
        BitmapDescriptorFactory.fromBitmap(
                articObjectMarkerGenerator.makeIcon(null, scale = .15f))
    }

    init {
        Observables.combineLatest(visibleRegionChanges, selectedArticObject)
                .sample(visibleRegionSampleRate, TimeUnit.MILLISECONDS) // too many events, prevent flooding.
                .withLatestFrom(mapItems, mapChangeEvents)
                .filter { (_, _, mapEvent) -> mapEvent.displayMode !is MapDisplayMode.Tour }
                .toFlowable(BackpressureStrategy.LATEST) // if downstream can't keep up, let's pick latest.
                .subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { (regionAndObject, mapItems, mapChangeEvent) ->
                    val (region, optArticObject) = regionAndObject
                    Timber.d("Evaluating Markers For Visible Region Change $region")
                    mapItems.values.forEach { markerHolder ->
                        adjustVisibleMarker(markerHolder, optArticObject.value, region, mapChangeEvent)
                    }
                }
                .disposedBy(imageQueueDisposeBag)
    }

    fun bindToSelectedArticObject(selectedArticObject: Observable<ArticObject>, disposeBag: DisposeBag) {
        selectedArticObject
                .mapOptional()
                .bindTo(this.selectedArticObject)
                .disposedBy(disposeBag)
    }


    /**
     * This method adjusts all visible [ArticObject] mapitems on screen based on [VisibleRegion] changes.
     *
     * If the marker [isCloseEnoughToCenter], we display a loading indicator and load the image on a separate thread.
     * We shouldn't hold the image in memory, so we must reload the image after it's been collapsed.
     */
    override fun adjustVisibleMarker(
            existing: MarkerHolder<MapItemModel>,
            visibleRegion: VisibleRegion,
            mapChangeEvent: MapChangeEvent) {
        adjustVisibleMarker(existing, null, visibleRegion, mapChangeEvent)
    }

    private fun adjustVisibleMarker(
            existing: MarkerHolder<MapItemModel>,
            selectedArticObject: ArticObject?,
            visibleRegion: VisibleRegion,
            mapChangeEvent: MapChangeEvent) {
        val position = getAdjustedLocationFromItem(existing.item)
        val meta = existing.marker.metaData()
                ?: MarkerMetaData(existing.item, loadedBitmap = false, requestDisposable = null, isSelected = false)

        val isSelectedObject = existing.item.isObject(selectedArticObject)

        if (mapChangeEvent.displayMode !is MapDisplayMode.CurrentFloor
                || position.isCloseEnoughToCenter(visibleRegion)
                || isSelectedObject) {
            if (!meta.loadedBitmap) {
                // show loading while its loading.
                existing.marker.setIcon(loadingBitmap)

                // enqueue replacement and add to marker meta.
                existing.marker.tag = meta.copy(
                        requestDisposable = enqueueBitmapFetch(item = existing.item, mapChangeEvent = mapChangeEvent), isSelected = isSelectedObject)
            } else if (meta.isSelected xor isSelectedObject) {
                existing.marker.tag = meta.copy(
                        requestDisposable = enqueueBitmapFetch(item = existing.item, mapChangeEvent = mapChangeEvent), isSelected = isSelectedObject)
            }
        } else {
            // reset loading state here.
            existing.marker.apply {
                meta.requestDisposable?.let { existing -> imageQueueDisposeBag.remove(existing) }
                tag = meta.copy(loadedBitmap = false, requestDisposable = null)
                setIcon(scaledDot)
            }
        }
    }

    override fun displayMarker(
            item: MapItemModel,
            mapChangeEvent: MapChangeEvent,
            visibleRegion: VisibleRegion,
            displayMode: MapDisplayMode,
            map: GoogleMap,
            floor: Int,
            id: String
    ): MarkerHolder<MapItemModel>? {
        val position = getAdjustedLocationFromItem(item)
        var requestDisposable: Disposable? = null

        // slightly different logic than the above method. If close enough to center or on tour, enqueue and show loading.
        // otherwise just show dot.
        val fastBitmap = if (displayMode !is MapDisplayMode.CurrentFloor ||
                position.isCloseEnoughToCenter(visibleRegion)) {
            requestDisposable = enqueueBitmapFetch(item = item, mapChangeEvent = mapChangeEvent)
            loadingBitmap
        } else {
            scaledDot
        }

        val isSelectedTourStop = if (mapChangeEvent.displayMode is MapDisplayMode.Tour) {
            mapChangeEvent.displayMode.selectedTourStop?.objectId == id
        } else {
            false
        }

        // fast bitmap returns immediately.
        return constructAndAddMarkerHolder(
                item = item,
                bitmap = fastBitmap,
                displayMode = displayMode,
                map = map,
                floor = floor,
                id = id,
                // bitmap queue not used, means bitmap is considered loaded
                loadedBitmap = !useBitmapQueue,
                isSelected = isSelectedTourStop,
                requestDisposable = requestDisposable)
    }

    override fun getItems(floor: Int, displayMode: MapDisplayMode): Flowable<List<MapItemModel>> = when (displayMode) {
        is MapDisplayMode.CurrentFloor -> objectsDao.getObjectsByFloor(floor = floor).map {
            it.map {
                MapItemModel.fromArticObject(it)
            }
        }
        is MapDisplayMode.Tour -> objectsDao.getObjectsByIdList(displayMode.tour.tourStops.mapNotNull { it.objectId }).map {
            it.map {
                MapItemModel.fromArticObject(it)
            }
        }
        is MapDisplayMode.Search.ObjectSearch -> {
            listOf(MapItemModel.fromArticSearchArtwork(displayMode.item)).asFlowable()
        }
        is MapDisplayMode.Search.ExhibitionSearch -> {
            listOf(MapItemModel.fromExhibition(displayMode.item)).asFlowable()
        }
        is MapDisplayMode.Search.AmenitiesSearch -> listOf<List<MapItemModel>>().toFlowable()
    }

    override fun getVisibleMapFocus(displayMode: MapDisplayMode): Set<MapFocus> =
            when (displayMode) {
                is MapDisplayMode.Tour -> MapFocus.values().toSet()
                is MapDisplayMode.Search.ObjectSearch -> MapFocus.values().toSet()
                // Objects NEVER show up in the AmenitiesSearch mode.
                is MapDisplayMode.Search.AmenitiesSearch -> emptySet()
                is MapDisplayMode.Search.ExhibitionSearch -> MapFocus.values().toSet()
                // 'else' includes CurrentFloor at this time.
                else -> setOf(MapFocus.Individual)
            }

    override fun getLocationFromItem(item: MapItemModel): LatLng = item.latLng

    override fun getIdFromItem(item: MapItemModel): String = item.id

    override fun getFastBitmap(item: MapItemModel, displayMode: MapDisplayMode): BitmapDescriptor? =
            loadingBitmap

    override fun getBitmapFetcher(item: MapItemModel, displayMode: MapDisplayMode): Observable<BitmapDescriptor>? {
        val imageSize = context.resources.getDimension(R.dimen.artic_object_map_image_size).toInt()
        return GlideApp.with(context)
                .asBitmap()
                .placeholder(R.drawable.circular_placeholder)
                .apply(RequestOptions().disallowHardwareConfig())
                .error(R.drawable.circular_placeholder)
                .loadWithThumbnail(
                        item.thumbURL,
                        item.imageURL
                )
                .asRequestObservable(context,
                        width = imageSize,
                        height = imageSize)
                .withLatestFrom(selectedArticObject)
                .map { (bitmap, articObject) ->
                    val isSelected = item.isObject(articObject.value)
                    val order: String? = item.getTourOrderNumberBasedOnDisplayMode(displayMode)
                    BitmapDescriptorFactory.fromBitmap(
                            articObjectMarkerGenerator.makeIcon(bitmap, overlay = order, selected = isSelected))
                }
    }

    override val zIndex: Float = 2.0f

    override fun getMarkerAlpha(floor: Int, mapDisplayMode: MapDisplayMode, item: MapItemModel): Float {
        // on tour, set the alpha depending on current floor.
        return if (shouldShowMarkerOnAllFloors(mapDisplayMode)) {
            if (item.floor == floor) ALPHA_VISIBLE else ALPHA_DIMMED
        } else {
            ALPHA_VISIBLE
        }
    }

    private fun shouldShowMarkerOnAllFloors(mode: MapDisplayMode): Boolean {
        return mode is MapDisplayMode.Tour ||
                mode is MapDisplayMode.Search.ObjectSearch ||
                mode is MapDisplayMode.Search.ExhibitionSearch ||
                (mode is MapDisplayMode.Search.AmenitiesSearch && mode.item == ArticMapAmenityType.DINING)
    }
}
